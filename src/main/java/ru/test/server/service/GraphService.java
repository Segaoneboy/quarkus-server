package ru.test.server.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.enterprise.context.ApplicationScoped;
import ru.test.data.DataGenerator;
import ru.test.data.migration.MigrationEngine;
import ru.test.data.serialization.JsonGraphSerializer;
import ru.test.data.serialization.ProtobufGraphSerializer;
import ru.test.data.serialization.dto.GraphDto;
import ru.test.data.serialization.proto.GraphSnapshotProto;
import ru.test.domain.Editable;
import ru.test.domain.Profile;
import ru.test.domain.datastructure.BFSResult;
import ru.test.domain.datastructure.Graph;
import ru.test.domain.datastructure.Vertex;
import ru.test.domain.exception.CsvParserException;
import ru.test.domain.exception.MigrationException;
import ru.test.server.dto.*;
import ru.test.server.dto.OperationReports.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class GraphService {

    private final Graph graph = new Graph(32);
    private final JsonGraphSerializer jsonSerializer = new JsonGraphSerializer();
    private final ProtobufGraphSerializer protobufSerializer = new ProtobufGraphSerializer();
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private final DataGenerator dataGenerator = new DataGenerator();

    public Graph getGraph() {
        return graph;
    }

    public List<ProfileResponse> getAllProfiles(String cityFilter) {
        Vertex[] vertices = graph.getVerticesArray();
        List<ProfileResponse> list = new ArrayList<>(vertices.length);
        for (Vertex v : vertices) {
            Profile p = v.getProfile();
            if (cityFilter == null || cityFilter.isBlank() ||
                    (p.getCity() != null && p.getCity().equalsIgnoreCase(cityFilter))) {
                list.add(ProfileMapper.toResponse(p));
            }
        }
        return list;
    }

    public ProfileResponse getProfileById(int id) {
        return ProfileMapper.toResponse(findDomainProfile(id));
    }

    public Profile findDomainProfile(int id) {
        Vertex v = graph.findVertexById(id);
        if (v == null) {
            throw new NoSuchElementException("Профиль с ID " + id + " не найден");
        }
        return v.getProfile();
    }

    public ProfileResponse createProfile(CreateProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Тело запроса не может быть пустым");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Имя профиля не может быть пустым");
        }
        if (request.birthYear() < 1900 || request.birthYear() > 2026) {
            throw new IllegalArgumentException("Некорректный год рождения: " + request.birthYear());
        }
        if (request.rating() != null && request.rating() < 0.0) {
            throw new IllegalArgumentException("Рейтинг не может быть отрицательным");
        }

        int resolvedId;
        if (request.id() == null || request.id() <= 0) {
            resolvedId = idCounter.incrementAndGet();
        } else {
            resolvedId = request.id();
            if (graph.findVertexById(resolvedId) != null) {
                throw new IllegalStateException("Профиль с ID " + resolvedId + " уже существует");
            }
            idCounter.updateAndGet(curr -> Math.max(curr, resolvedId));
        }

        Profile profile = ProfileMapper.toDomain(request, resolvedId);
        graph.addVertex(profile);
        return ProfileMapper.toResponse(profile);
    }

    public ProfileResponse updateProfile(int id, UpdateProfileRequest request) throws CsvParserException {
        Profile existing = findDomainProfile(id);
        if (!(existing instanceof Editable editable)) {
            throw new UnsupportedOperationException("Профиль ID=" + id + " является read-only (DeletedProfile) и не подлежит изменению");
        }
        editable.updateFrom(request);
        return ProfileMapper.toResponse(existing);
    }

    public void deleteProfile(int id) {
        if (!graph.removeVertex(id)) {
            throw new NoSuchElementException("Профиль с ID " + id + " не найден");
        }
    }

    public FriendshipResponse addFriendship(int id1, int id2, int strength) {
        if (!graph.addEdge(id1, id2, strength)) {
            throw new NoSuchElementException("Не удалось создать связь: один из профилей не найден или ID совпадают");
        }
        return new FriendshipResponse("connected", id1, id2, strength);
    }

    public BFSResult findShortestPath(int from, int to) {
        findDomainProfile(from);
        findDomainProfile(to);
        return graph.findShortestPath(from, to);
    }

    public GenerateResponse generateData(int count) {
        int edgesAdded = dataGenerator.generateIntoGraph(graph, idCounter, count);
        return new GenerateResponse(count, edgesAdded, graph.getVertexCount(), graph.getTotalEdges());
    }

    public String exportJson(int targetVersion) throws MigrationException {
        GraphDto snapshot = graph.exportSnapshot(getAllDomainProfiles());
        return jsonSerializer.serializeToString(snapshot, targetVersion);
    }

    public byte[] exportProtobuf(int targetVersion) throws MigrationException {
        GraphDto snapshot = graph.exportSnapshot(getAllDomainProfiles());
        return protobufSerializer.serializeToBytes(snapshot, targetVersion);
    }

    public ImportResponse importJson(String rawJson) throws MigrationException {
        JsonObject rawObj = JsonParser.parseString(rawJson).getAsJsonObject();
        int originalVersion = rawObj.has("schema_version") ? rawObj.get("schema_version").getAsInt() : -1;

        GraphDto migratedDto = jsonSerializer.deserializeFromJsonObject(rawObj);
        graph.restoreFromSnapshot(migratedDto);
        syncIdCounterWithGraph();

        return new ImportResponse(
                "json",
                originalVersion,
                MigrationEngine.CURRENT_VERSION,
                originalVersion < MigrationEngine.CURRENT_VERSION,
                migratedDto.getProfiles().size(),
                migratedDto.getEdges().size()
        );
    }

    public ImportResponse importProtobuf(byte[] data) throws MigrationException {
        int originalVersion = -1;
        try {
            originalVersion = GraphSnapshotProto.parseFrom(data).getSchemaVersion();
        } catch (InvalidProtocolBufferException e) {
            // все равно обработается в deserializeFromBytes
        }

        GraphDto migratedDto = protobufSerializer.deserializeFromBytes(data);
        graph.restoreFromSnapshot(migratedDto);
        syncIdCounterWithGraph();

        return new ImportResponse(
                "protobuf",
                originalVersion,
                MigrationEngine.CURRENT_VERSION,
                originalVersion > 0 && originalVersion < MigrationEngine.CURRENT_VERSION,
                migratedDto.getProfiles().size(),
                migratedDto.getEdges().size()
        );
    }

    private List<Profile> getAllDomainProfiles() {
        Vertex[] vertices = graph.getVerticesArray();
        List<Profile> list = new ArrayList<>(vertices.length);
        for (Vertex v : vertices) {
            list.add(v.getProfile());
        }
        return list;
    }

    private void syncIdCounterWithGraph() {
        int maxId = 0;
        for (Vertex v : graph.getVerticesArray()) {
            if (v.getProfile().getId() > maxId) {
                maxId = v.getProfile().getId();
            }
        }
        idCounter.set(maxId);
    }
}