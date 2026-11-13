package ru.test.data.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.test.domain.exception.ErrorCode;
import ru.test.domain.exception.MigrationException;

import java.util.ArrayList;
import java.util.List;

public class MigrationEngine {
    public static final int CURRENT_VERSION = 3;

    public record DowngradeWarning(int targetVersion, List<String> droppedFields, String message) {}

    public static JsonObject migrateToCurrent(JsonObject root) throws MigrationException {
        if (root == null || !root.has("schema_version") || !root.get("schema_version").isJsonPrimitive()) {
            throw new MigrationException(ErrorCode.CORRUPTED_FILE, "Отсутствует или повреждён заголовок schema_version");
        }

        int version;
        try {
            version = root.get("schema_version").getAsInt();
        } catch (NumberFormatException e) {
            throw new MigrationException(ErrorCode.CORRUPTED_FILE, "schema_version не является целым числом");
        }

        if (version < 1) {
            throw new MigrationException(ErrorCode.UNSUPPORTED_SCHEMA_VERSION, version, "Версия схемы не может быть меньше 1: " + version);
        }
        if (version > CURRENT_VERSION) {
            throw new MigrationException(
                    ErrorCode.FUTURE_SCHEMA_VERSION,
                    version,
                    "Файл создан в более новой версии программы (v" + version + "). Текущая версия: v" + CURRENT_VERSION
            );
        }

        if (!root.has("profiles") || !root.get("profiles").isJsonArray()) {
            throw new MigrationException(ErrorCode.CORRUPTED_FILE, version, "В файле отсутствует массив profiles");
        }

        JsonArray profiles = root.getAsJsonArray("profiles");

        for (int i = 0; i < profiles.size(); i++) {
            JsonElement el = profiles.get(i);
            if (!el.isJsonObject()) {
                throw new MigrationException(ErrorCode.CORRUPTED_FILE, version, "Элемент profiles[" + i + "] не является JSON-объектом");
            }
            JsonObject p = el.getAsJsonObject();
            if (!p.has("id") || p.get("id").getAsInt() < 0) {
                throw new MigrationException(ErrorCode.INVALID_FIELD_VALUE, version, "Некорректный ID в записи #" + i);
            }
            if (!p.has("name") || p.get("name").getAsString().isBlank()) {
                throw new MigrationException(ErrorCode.INVALID_FIELD_VALUE, version, "Пустое имя в записи #" + i);
            }
            if (p.has("birthYear")) {
                int year = p.get("birthYear").getAsInt();
                if (year < 1900 || year > 2026) {
                    throw new MigrationException(ErrorCode.INVALID_FIELD_VALUE, version, "Некорректный год рождения (" + year + ") в записи #" + i);
                }
            }
        }

        // v1 -> v2
        if (version == 1) {
            for (JsonElement el : profiles) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("rating") || obj.get("rating").isJsonNull()) {
                    obj.addProperty("rating", 0.0);
                } else if (obj.get("rating").getAsDouble() < 0) {
                    throw new MigrationException(ErrorCode.INVALID_FIELD_VALUE, 1, "Отрицательный рейтинг при миграции v1->v2");
                }
            }
            version = 2;
        }

        // v2 -> v3
        if (version == 2) {
            for (JsonElement el : profiles) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("bio") || obj.get("bio").isJsonNull()) {
                    obj.addProperty("bio", "");
                }
            }
            version = 3;
        }

        root.addProperty("schema_version", CURRENT_VERSION);
        return root;
    }

    public static JsonObject downgrade(JsonObject currentRoot, int targetVersion) throws MigrationException {
        if (targetVersion < 1 || targetVersion > CURRENT_VERSION) {
            throw new MigrationException(ErrorCode.UNSUPPORTED_SCHEMA_VERSION, targetVersion, "Невозможно экспортировать в версию v" + targetVersion);
        }

        JsonObject copy = currentRoot.deepCopy();
        JsonArray profiles = copy.getAsJsonArray("profiles");

        if (targetVersion < 3) {
            for (JsonElement el : profiles) {
                el.getAsJsonObject().remove("bio");
            }
        }
        if (targetVersion < 2) {
            for (JsonElement el : profiles) {
                el.getAsJsonObject().remove("rating");
            }
        }

        copy.addProperty("schema_version", targetVersion);
        return copy;
    }

    public static DowngradeWarning getDowngradeWarning(int targetVersion) {
        List<String> dropped = new ArrayList<>();
        if (targetVersion < 3) dropped.add("bio");
        if (targetVersion < 2) dropped.add("rating");

        String msg = dropped.isEmpty()
                ? "Exported to current schema v" + CURRENT_VERSION + " without data loss."
                : "Warning! Export to schema v" + targetVersion + " drops fields: " + String.join(", ", dropped);

        return new DowngradeWarning(targetVersion, dropped, msg);
    }
}