package ru.test.server.dto;

public final class OperationReports {
    private OperationReports() {}

    public record GenerateResponse(
            int generatedProfiles,
            int generatedEdges,
            int totalVerticesInGraph,
            int totalEdgesInGraph
    ) {}

    public record ImportResponse(
            String format,
            int importedFromVersion,
            int migratedToVersion,
            boolean wasMigrated,
            int profilesCount,
            int edgesCount
    ) {}

    public record FriendshipResponse(
            String status,
            int from,
            int to,
            int strength
    ) {}
}