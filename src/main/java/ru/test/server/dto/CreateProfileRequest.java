package ru.test.server.dto;

import java.util.List;

public record CreateProfileRequest(
        String type,
        Integer id,
        String name,
        String city,
        int birthYear,
        Double rating,
        String bio,
        List<String> groups
) {}