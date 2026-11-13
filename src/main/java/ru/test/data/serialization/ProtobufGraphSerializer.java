package ru.test.data.serialization;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.test.data.migration.MigrationEngine;
import ru.test.data.serialization.dto.EdgeDto;
import ru.test.data.serialization.dto.GraphDto;
import ru.test.data.serialization.proto.EdgeProto;
import ru.test.data.serialization.proto.GraphSnapshotProto;
import ru.test.data.serialization.proto.ProfileProto;
import ru.test.data.serialization.proto.ProfileTypeProto;
import ru.test.domain.AdminProfile;
import ru.test.domain.DeletedProfile;
import ru.test.domain.Profile;
import ru.test.domain.UserProfile;
import ru.test.domain.datastructure.Connections;
import ru.test.domain.exception.ErrorCode;
import ru.test.domain.exception.MigrationException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProtobufGraphSerializer implements Serializer<GraphDto> {

    public static final int CURRENT_SCHEMA_VERSION = MigrationEngine.CURRENT_VERSION;

    @Override
    public String getFormatName() {
        return "Protocol Buffers (*.pb)";
    }

    @Override
    public String getExtension() {
        return "pb";
    }

    @Override
    public void save(GraphDto data, File file) throws IOException {
        saveWithVersion(data, file, CURRENT_SCHEMA_VERSION);
    }

    public void saveWithVersion(GraphDto data, File file, int targetVersion) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Целевой файл не может быть null");
        }
        try {
            byte[] bytes = serializeToBytes(data, targetVersion);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
        } catch (MigrationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public byte[] serializeToBytes(GraphDto data, int targetVersion) throws MigrationException {
        if (targetVersion < 1 || targetVersion > CURRENT_SCHEMA_VERSION) {
            throw new MigrationException(
                    ErrorCode.UNSUPPORTED_SCHEMA_VERSION,
                    targetVersion,
                    "Неподдерживаемая целевая версия для экспорта Protobuf: v" + targetVersion
            );
        }

        GraphSnapshotProto.Builder snapshotBuilder = GraphSnapshotProto.newBuilder()
                .setSchemaVersion(targetVersion);

        List<Profile> profiles = (data != null && data.getProfiles() != null) ? data.getProfiles() : List.of();
        for (Profile p : profiles) {
            ProfileProto.Builder pBuilder = ProfileProto.newBuilder()
                    .setId(p.getId())
                    .setName(p.getName() != null ? p.getName() : "")
                    .setCity(p.getCity() != null ? p.getCity() : "")
                    .setBirthYear(p.getBirthYear());

            if (p instanceof AdminProfile admin) {
                pBuilder.setType(ProfileTypeProto.ADMIN);
                if (admin.getGroups() != null) {
                    pBuilder.addAllGroups(admin.getGroups());
                }
            } else if (p instanceof DeletedProfile) {
                pBuilder.setType(ProfileTypeProto.DELETED);
            } else {
                pBuilder.setType(ProfileTypeProto.USER);
            }

            if (targetVersion >= 2) {
                pBuilder.setRating(p.getRating());
            }
            if (targetVersion >= 3) {
                pBuilder.setBio(p.getBio() != null ? p.getBio() : "");
            }

            snapshotBuilder.addProfiles(pBuilder.build());
        }

        List<EdgeDto> edges = (data != null && data.getEdges() != null) ? data.getEdges() : List.of();
        for (EdgeDto edge : edges) {
            snapshotBuilder.addEdges(EdgeProto.newBuilder()
                    .setFromId(edge.getFromId())
                    .setFriendId(edge.getFriendId())
                    .setStrength(edge.getStrength())
                    .build());
        }

        return snapshotBuilder.build().toByteArray();
    }

    @Override
    public GraphDto load(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("Файл не существует или путь равен null");
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            return deserializeFromBytes(fis.readAllBytes());
        } catch (MigrationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public GraphDto deserializeFromBytes(byte[] bytes) throws MigrationException {
        if (bytes == null || bytes.length == 0) {
            throw new MigrationException(ErrorCode.CORRUPTED_FILE, "Пустой массив байтов Protobuf");
        }

        GraphSnapshotProto snapshot;
        try {
            snapshot = GraphSnapshotProto.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new MigrationException(ErrorCode.CORRUPTED_FILE, "Повреждённый бинарный поток Protobuf: " + e.getMessage());
        }

        int version = snapshot.getSchemaVersion();
        if (version < 1) {
            throw new MigrationException(
                    ErrorCode.UNSUPPORTED_SCHEMA_VERSION,
                    version,
                    "Отсутствует или некорректна schema_version в Protobuf: " + version
            );
        }
        if (version > CURRENT_SCHEMA_VERSION) {
            throw new MigrationException(
                    ErrorCode.FUTURE_SCHEMA_VERSION,
                    version,
                    "Protobuf-файл создан в более новой версии (v" + version + "). Текущая: v" + CURRENT_SCHEMA_VERSION
            );
        }

        List<Profile> migratedProfiles = new ArrayList<>(snapshot.getProfilesCount());
        Connections[] dummyConnections = new Connections[0];

        for (ProfileProto pp : snapshot.getProfilesList()) {
            int id = pp.getId();
            String name = pp.getName();
            String city = pp.getCity();
            int birthYear = pp.getBirthYear();

            if (id < 0 || name.isBlank() || birthYear < 1900 || birthYear > 2026) {
                throw new MigrationException(
                        ErrorCode.INVALID_FIELD_VALUE,
                        version,
                        "Некорректные базовые поля профиля в Protobuf (ID=" + id + ")"
                );
            }

            double rating = 0.0;
            if (version >= 2) {
                rating = pp.getRating();
                if (rating < 0.0) {
                    throw new MigrationException(
                            ErrorCode.INVALID_FIELD_VALUE,
                            version,
                            "Отрицательный рейтинг в Protobuf у профиля ID=" + id
                    );
                }
            }

            String bio = "";
            if (version >= 3) {
                bio = pp.getBio() != null ? pp.getBio() : "";
            }

            Profile profile = switch (pp.getType()) {
                case ADMIN -> new AdminProfile(id, name, city, birthYear, dummyConnections, new ArrayList<>(pp.getGroupsList()), rating, bio);
                case DELETED -> new DeletedProfile(id, name, city, birthYear, dummyConnections, rating, bio);
                case USER -> new UserProfile(id, name, city, birthYear, dummyConnections, rating, bio);
                default -> throw new MigrationException(ErrorCode.CORRUPTED_FILE, version, "Неизвестный тип профиля в Protobuf");
            };

            migratedProfiles.add(profile);
        }

        List<EdgeDto> edges = new ArrayList<>(snapshot.getEdgesCount());
        for (EdgeProto ep : snapshot.getEdgesList()) {
            edges.add(new EdgeDto(ep.getFromId(), ep.getFriendId(), ep.getStrength()));
        }

        return new GraphDto(CURRENT_SCHEMA_VERSION, migratedProfiles, edges);
    }
}