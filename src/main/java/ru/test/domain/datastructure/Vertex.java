package ru.test.domain.datastructure;

import ru.test.domain.Profile;

public class Vertex {
    private Profile profile;
    private Connections[] connections;
    private int connectionsCount;
    private Vertex nextInBucket;

    public Vertex(Profile profile, int initialCapacity) {
        this.profile = profile;
        this.connections = new Connections[Math.max(initialCapacity, 4)];
        this.connectionsCount = 0;
        this.nextInBucket = null;
    }

    public void addConnection(int friendId, int strength) {
        for (int i = 0; i < connectionsCount; i++) {
            if (connections[i].getFriendId() == friendId) {
                connections[i].setConnection(strength);
                return;
            }
        }
        if (connectionsCount >= connections.length) {
            Connections[] newConnections = new Connections[connections.length * 2];
            System.arraycopy(connections, 0, newConnections, 0, connectionsCount);
            connections = newConnections;
        }
        connections[connectionsCount++] = new Connections(friendId, strength);
    }

    public boolean removeConnection(int friendId) {
        for (int i = 0; i < connectionsCount; i++) {
            if (connections[i].getFriendId() == friendId) {
                int numMoved = connectionsCount - i - 1;
                if (numMoved > 0) {
                    System.arraycopy(connections, i + 1, connections, i, numMoved);
                }
                connections[--connectionsCount] = null;
                return true;
            }
        }
        return false;
    }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public Connections[] getConnections() { return connections; }
    public int getConnectionCount() { return connectionsCount; }

    public Vertex getNextInBucket() { return nextInBucket; }
    public void setNextInBucket(Vertex nextInBucket) { this.nextInBucket = nextInBucket; }
}