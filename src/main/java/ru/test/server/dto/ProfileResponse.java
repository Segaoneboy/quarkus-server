package ru.test.server.dto;

import java.util.List;

public record ProfileResponse(
        int id,
        String type,
        String name,
        String city,
        int birthYear,
        double rating,
        String bio,
        boolean editable,
        List<String> groups,
        List<EdgeInfo> friends
) {
    public record EdgeInfo(int friendId, int strength) {}
}