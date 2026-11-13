package ru.test.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import ru.test.data.serialization.adapter.ProfileTypeAdapter;
import ru.test.domain.Profile;

@ApplicationScoped
public class GsonProducer {

    @Produces
    @ApplicationScoped
    public Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Profile.class, new ProfileTypeAdapter())
                .create();
    }
}