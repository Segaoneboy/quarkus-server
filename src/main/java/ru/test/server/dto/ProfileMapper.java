package ru.test.server.dto;

import ru.test.domain.*;
import ru.test.domain.datastructure.Connections;

import java.util.ArrayList;
import java.util.List;

public final class ProfileMapper {
    private ProfileMapper() {}

    public static Profile toDomain(CreateProfileRequest req, int resolvedId) {
        if (req == null) {
            throw new IllegalArgumentException("Тело запроса не может быть пустым");
        }
        String rawType = (req.type() != null && !req.type().isBlank())
                ? req.type().trim().toUpperCase()
                : "USER";

        if ("DELETED".equals(rawType)) {
            throw new IllegalArgumentException("Создание DELETED-профилей через API запрещено (доступно только через импорт)");
        }

        double rating = (req.rating() != null) ? req.rating() : 0.0;
        String bio = (req.bio() != null) ? req.bio() : "";
        Connections[] emptyConnections = new Connections[0];

        return switch (rawType) {
            case "ADMIN" -> new AdminProfile(
                    resolvedId, req.name(), req.city(), req.birthYear(),
                    emptyConnections,
                    req.groups() != null ? new ArrayList<>(req.groups()) : new ArrayList<>(),
                    rating, bio
            );
            case "USER" -> new UserProfile(
                    resolvedId, req.name(), req.city(), req.birthYear(),
                    emptyConnections, rating, bio
            );
            default -> throw new IllegalArgumentException("Неизвестный тип профиля: " + rawType + ". Допустимы: USER, ADMIN");
        };
    }

    public static ProfileResponse toResponse(Profile p) {
        String type = "USER";
        List<String> groups = List.of();

        if (p instanceof AdminProfile admin) {
            type = "ADMIN";
            groups = admin.getGroups() != null ? List.copyOf(admin.getGroups()) : List.of();
        } else if (p instanceof DeletedProfile) {
            type = "DELETED";
        }

        List<ProfileResponse.EdgeInfo> friends = new ArrayList<>();
        Connections[] conns = p.getFriendshipList();
        if (conns != null) {
            for (int i = 0; i < p.getFriendshipCount(); i++) {
                if (conns[i] != null) {
                    friends.add(new ProfileResponse.EdgeInfo(conns[i].getFriendId(), conns[i].getConnection()));
                }
            }
        }

        return new ProfileResponse(
                p.getId(),
                type,
                p.getName(),
                p.getCity(),
                p.getBirthYear(),
                p.getRating(),
                p.getBio(),
                p instanceof Editable,
                groups,
                friends
        );
    }
}