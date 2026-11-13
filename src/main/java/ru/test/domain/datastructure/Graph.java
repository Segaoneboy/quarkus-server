package ru.test.domain.datastructure;

import ru.test.data.serialization.dto.EdgeDto;
import ru.test.data.serialization.dto.GraphDto;
import ru.test.domain.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Graph {

    private Vertex[] buckets;
    private int vertexCount;
    private int totalEdges;
    private final float loadFactor = 0.75f;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public Graph(int initialCapacity) {
        int cap = Math.max(initialCapacity, 16);
        this.buckets = new Vertex[cap];
        this.vertexCount = 0;
        this.totalEdges = 0;
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            this.buckets = new Vertex[Math.max(buckets.length, 16)];
            this.vertexCount = 0;
            this.totalEdges = 0;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private int getBucketIndex(int id, int capacity) {
        return (id & 0x7fffffff) % capacity;
    }

    private void resize() {
        int newCap = buckets.length * 2;
        Vertex[] newBuckets = new Vertex[newCap];

        for (Vertex head : buckets) {
            Vertex curr = head;
            while (curr != null) {
                Vertex next = curr.getNextInBucket();
                int idx = getBucketIndex(curr.getProfile().getId(), newCap);
                curr.setNextInBucket(newBuckets[idx]);
                newBuckets[idx] = curr;
                curr = next;
            }
        }
        this.buckets = newBuckets;
    }

    public boolean addVertex(Profile profile) {
        rwLock.writeLock().lock();
        try {
            if (profile == null) return false;
            int id = profile.getId();
            int idx = getBucketIndex(id, buckets.length);

            Vertex curr = buckets[idx];
            while (curr != null) {
                if (curr.getProfile().getId() == id) {
                    curr.setProfile(profile);
                    return false;
                }
                curr = curr.getNextInBucket();
            }

            if ((float) (vertexCount + 1) / buckets.length > loadFactor) {
                resize();
                idx = getBucketIndex(id, buckets.length);
            }

            Vertex newVertex = new Vertex(profile, 4);
            newVertex.setNextInBucket(buckets[idx]);
            buckets[idx] = newVertex;
            vertexCount++;
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Vertex findVertexById(int id) {
        rwLock.readLock().lock();
        try {
            return findNodeInternal(id);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private Vertex findNodeInternal(int id) {
        int idx = getBucketIndex(id, buckets.length);
        Vertex curr = buckets[idx];
        while (curr != null) {
            if (curr.getProfile().getId() == id) return curr;
            curr = curr.getNextInBucket();
        }
        return null;
    }

    public boolean addEdge(int id1, int id2, int strength) {
        rwLock.writeLock().lock();
        try {
            if (id1 == id2) return false;

            Vertex v1 = findNodeInternal(id1);
            Vertex v2 = findNodeInternal(id2);

            if (v1 != null && v2 != null) {
                v1.addConnection(id2, strength);
                v2.addConnection(id1, strength);

                v1.getProfile().addFriendship(id2, strength);
                v2.getProfile().addFriendship(id1, strength);

                totalEdges++;
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean removeVertex(int id) {
        rwLock.writeLock().lock();
        try {
            int idx = getBucketIndex(id, buckets.length);
            Vertex curr = buckets[idx];
            Vertex prev = null;
            Vertex target = null;

            while (curr != null) {
                if (curr.getProfile().getId() == id) {
                    target = curr;
                    if (prev == null) buckets[idx] = curr.getNextInBucket();
                    else prev.setNextInBucket(curr.getNextInBucket());
                    vertexCount--;
                    break;
                }
                prev = curr;
                curr = curr.getNextInBucket();
            }

            if (target == null) return false;

            Connections[] conns = target.getConnections();
            int cCount = target.getConnectionCount();
            for (int i = 0; i < cCount; i++) {
                int friendId = conns[i].getFriendId();
                Vertex friend = findNodeInternal(friendId);
                if (friend != null) {
                    if (friend.removeConnection(id)) {
                        friend.getProfile().removeFriendship(id);
                        totalEdges--;
                    }
                }
            }
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public BFSResult findShortestPath(int startId, int targetId) {
        rwLock.readLock().lock();
        try {
            if (findNodeInternal(startId) == null || findNodeInternal(targetId) == null) {
                return new BFSResult(new int[0], new int[0], false);
            }

            IntQueue queue = new IntQueue(Math.min(vertexCount, 1024));
            FastIntMap tracker = new FastIntMap(Math.min(vertexCount, 1024));

            int[] visitedOrder = new int[Math.min(vertexCount, 2048)];
            int visitedCount = 0;

            queue.enqueue(startId);
            tracker.put(startId, -1);

            boolean reached = false;

            while (!queue.isEmpty()) {
                int currentId = queue.dequeue();
                if (visitedCount < visitedOrder.length) {
                    visitedOrder[visitedCount++] = currentId;
                }

                if (currentId == targetId) {
                    reached = true;
                    break;
                }

                Vertex current = findNodeInternal(currentId);
                if (current != null) {
                    Connections[] conns = current.getConnections();
                    int cCount = current.getConnectionCount();
                    for (int i = 0; i < cCount; i++) {
                        int neighborId = conns[i].getFriendId();
                        if (!tracker.containsKey(neighborId)) {
                            tracker.put(neighborId, currentId);
                            queue.enqueue(neighborId);
                        }
                    }
                }
            }

            int[] trimmedVisited = new int[visitedCount];
            System.arraycopy(visitedOrder, 0, trimmedVisited, 0, visitedCount);

            if (!reached) {
                return new BFSResult(new int[0], trimmedVisited, false);
            }

            int[] tempPath = new int[visitedCount + 1];
            int pathLen = 0;
            int trace = targetId;

            while (trace != -1) {
                tempPath[pathLen++] = trace;
                trace = tracker.getParent(trace);
            }

            int[] finalPath = new int[pathLen];
            for (int i = 0; i < pathLen; i++) {
                finalPath[i] = tempPath[pathLen - 1 - i];
            }

            return new BFSResult(finalPath, trimmedVisited, true);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Vertex[] getVerticesArray() {
        rwLock.readLock().lock();
        try {
            Vertex[] arr = new Vertex[vertexCount];
            int idx = 0;
            for (Vertex head : buckets) {
                Vertex curr = head;
                while (curr != null) {
                    arr[idx++] = curr;
                    curr = curr.getNextInBucket();
                }
            }
            return arr;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getVertexCount() {
        rwLock.readLock().lock();
        try { return vertexCount; } finally { rwLock.readLock().unlock(); }
    }

    public int getTotalEdges() {
        rwLock.readLock().lock();
        try { return totalEdges; } finally { rwLock.readLock().unlock(); }
    }

    private static class IntQueue {
        private int[] data;
        private int head = 0;
        private int tail = 0;
        private int count = 0;

        public IntQueue(int capacity) {
            this.data = new int[Math.max(capacity, 16)];
        }

        public void enqueue(int val) {
            if (count == data.length) {
                int[] newData = new int[data.length * 2];
                for (int i = 0; i < count; i++) {
                    newData[i] = data[(head + i) % data.length];
                }
                data = newData;
                head = 0;
                tail = count;
            }
            data[tail] = val;
            tail = (tail + 1) % data.length;
            count++;
        }

        public int dequeue() {
            if (count == 0) return -1;
            int val = data[head];
            head = (head + 1) % data.length;
            count--;
            return val;
        }

        public boolean isEmpty() { return count == 0; }
    }

    private static class FastIntMap {
        private static class Entry {
            int key;
            int parent;
            Entry next;
            Entry(int key, int parent, Entry next) {
                this.key = key;
                this.parent = parent;
                this.next = next;
            }
        }

        private Entry[] table;

        public FastIntMap(int capacity) {
            this.table = new Entry[Math.max(capacity, 64)];
        }

        public void put(int key, int parent) {
            int idx = (key & 0x7fffffff) % table.length;
            table[idx] = new Entry(key, parent, table[idx]);
        }

        public boolean containsKey(int key) {
            int idx = (key & 0x7fffffff) % table.length;
            Entry curr = table[idx];
            while (curr != null) {
                if (curr.key == key) return true;
                curr = curr.next;
            }
            return false;
        }

        public int getParent(int key) {
            int idx = (key & 0x7fffffff) % table.length;
            Entry curr = table[idx];
            while (curr != null) {
                if (curr.key == key) return curr.parent;
                curr = curr.next;
            }
            return -1;
        }
    }

    public GraphDto exportSnapshot(List<Profile> currentProfiles) {
        rwLock.readLock().lock();
        try {
            List<EdgeDto> edgeList = new ArrayList<>();

            for (Vertex head : buckets) {
                Vertex curr = head;
                while (curr != null) {
                    int u = curr.getProfile().getId();
                    Connections[] conns = curr.getConnections();
                    for (int i = 0; i < curr.getConnectionCount(); i++) {
                        int v = conns[i].getFriendId();
                        if (u < v) {
                            edgeList.add(new EdgeDto(u, v, conns[i].getConnection()));
                        }
                    }
                    curr = curr.getNextInBucket();
                }
            }

            return new GraphDto(3, new ArrayList<>(currentProfiles), edgeList);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void restoreFromSnapshot(GraphDto snapshot) {
        rwLock.writeLock().lock();
        try {
            clear();
            if (snapshot == null) return;

            if (snapshot.getProfiles() != null) {
                for (Profile p : snapshot.getProfiles()) {
                    addVertex(p);
                }
            }

            if (snapshot.getEdges() != null) {
                for (EdgeDto edge : snapshot.getEdges()) {
                    addEdge(edge.getFromId(), edge.getFriendId(), edge.getStrength());
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}