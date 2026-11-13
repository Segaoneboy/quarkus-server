package ru.test.data.serialization;

import com.google.gson.*;
import ru.test.data.migration.MigrationEngine;
import ru.test.data.serialization.adapter.ProfileTypeAdapter;
import ru.test.data.serialization.dto.GraphDto;
import ru.test.domain.Profile;
import ru.test.domain.exception.MigrationException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JsonGraphSerializer implements Serializer<GraphDto> {

    private final Gson gson;

    public JsonGraphSerializer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Profile.class, new ProfileTypeAdapter())
                .create();
    }

    @Override
    public String getFormatName() {
        return "JSON (*.json)";
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public void save(GraphDto data, File file) throws IOException {
        saveWithVersion(data, file, MigrationEngine.CURRENT_VERSION);
    }

    public void saveWithVersion(GraphDto data, File file, int targetVersion) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Целевой файл не может быть null");
        }
        try {
            String jsonString = serializeToString(data, targetVersion);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(jsonString);
            }
        } catch (MigrationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public String serializeToString(GraphDto data, int targetVersion) throws MigrationException {
        JsonObject root = gson.toJsonTree(data).getAsJsonObject();
        JsonObject downgraded = MigrationEngine.downgrade(root, targetVersion);
        return gson.toJson(downgraded);
    }

    @Override
    public GraphDto load(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("Файл не существует или путь равен null");
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Файл пуст или содержит некорректный JSON");
            }
            return deserializeFromJsonObject(parsed.getAsJsonObject());
        } catch (MigrationException | JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public GraphDto deserializeFromJsonObject(JsonObject rawObject) throws MigrationException {
        JsonObject migrated = MigrationEngine.migrateToCurrent(rawObject);
        return gson.fromJson(migrated, GraphDto.class);
    }
}