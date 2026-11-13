package ru.test.data.serialization.adapter;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import ru.test.domain.AdminProfile;
import ru.test.domain.DeletedProfile;
import ru.test.domain.Profile;
import ru.test.domain.UserProfile;
import ru.test.domain.datastructure.Connections;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProfileTypeAdapter implements JsonSerializer<Profile>, JsonDeserializer<Profile> {

    private static final String TYPE_PROPERTY = "type";
    private static final String TYPE_USER = "USER";
    private static final String TYPE_ADMIN = "ADMIN";
    private static final String TYPE_DELETED = "DELETED";

    @Override
    public JsonElement serialize(Profile src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", src.getId());
        jsonObject.addProperty("name", src.getName());
        jsonObject.addProperty("city", src.getCity());
        jsonObject.addProperty("birthYear", src.getBirthYear());
        jsonObject.addProperty("rating", src.getRating());
        jsonObject.addProperty("bio", src.getBio() != null ? src.getBio() : "");

        if (src instanceof AdminProfile admin) {
            jsonObject.addProperty(TYPE_PROPERTY, TYPE_ADMIN);
            jsonObject.add("groups", context.serialize(admin.getGroups()));
        } else if (src instanceof DeletedProfile) {
            jsonObject.addProperty(TYPE_PROPERTY, TYPE_DELETED);
        } else {
            jsonObject.addProperty(TYPE_PROPERTY, TYPE_USER);
        }

        return jsonObject;
    }

    @Override
    public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String type = jsonObject.has(TYPE_PROPERTY) ? jsonObject.get(TYPE_PROPERTY).getAsString() : TYPE_USER;

        int id = (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) ? jsonObject.get("id").getAsInt() : -1;
        String name = jsonObject.get("name").getAsString();
        String city = jsonObject.get("city").getAsString();
        int birthYear = jsonObject.get("birthYear").getAsInt();
        double rating = (jsonObject.has("rating") && !jsonObject.get("rating").isJsonNull())
                ? jsonObject.get("rating").getAsDouble() : 0.0;
        String bio = (jsonObject.has("bio") && !jsonObject.get("bio").isJsonNull())
                ? jsonObject.get("bio").getAsString() : "";

        Connections[] dummyConnections = new Connections[0];

        return switch (type) {
            case TYPE_ADMIN -> {
                List<String> groups = context.deserialize(jsonObject.get("groups"), new TypeToken<List<String>>() {}.getType());
                yield new AdminProfile(id, name, city, birthYear, dummyConnections, groups != null ? groups : new ArrayList<>(), rating, bio);
            }
            case TYPE_DELETED -> new DeletedProfile(id, name, city, birthYear, dummyConnections, rating, bio);
            default -> new UserProfile(id, name, city, birthYear, dummyConnections, rating, bio);
        };
    }
}