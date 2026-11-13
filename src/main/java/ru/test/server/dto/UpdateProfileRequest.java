package ru.test.server.dto;
import java.util.List;
public record UpdateProfileRequest(
        String name,
        String city,
        Integer birthYear,
        Double rating,
        String bio,
        List<String> groups
) {}
