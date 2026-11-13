package ru.test.domain.datastructure;

public class BFSResult {
    private final int[] path;
    private final int[] visitedOrder;
    private final boolean found;

    public BFSResult(int[] path, int[] visitedOrder, boolean found) {
        this.path = path;
        this.visitedOrder = visitedOrder;
        this.found = found;
    }

    public int[] getPath() { return path; }
    public int[] getVisitedOrder() { return visitedOrder; }
    public boolean isFound() { return found; }
}