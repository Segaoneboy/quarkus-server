package ru.test.data;

import ru.test.domain.AdminProfile;
import ru.test.domain.DeletedProfile;
import ru.test.domain.Profile;
import ru.test.domain.UserProfile;
import ru.test.domain.datastructure.Connections;
import ru.test.domain.datastructure.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DataGenerator {

    private static final String[] NAMES = {
            "Александр", "Дмитрий", "Максим", "Иван", "Артем",
            "Анна", "Мария", "Елена", "Ольга", "Татьяна"
    };

    private static final String[] CITIES = {
            "Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург",
            "Казань", "Нижний Новгород", "Челябинск", "Самара"
    };

    private static final String[] GROUPS = {
            "IT-Club", "Спорт", "Музыка", "Кино", "Наука", "Книги"
    };

    private static final String[] BIOS = {
            "Разработчик на Java", "Увлекаюсь алгоритмами", "Люблю путешествия",
            "Студент НГТУ", "Изучаю графы и сети", ""
    };

    private final Random random = new Random();

    public int generateIntoGraph(Graph graph, AtomicInteger idCounter, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Количество генерируемых записей должно быть больше 0");
        }
        if (count > 100_000) {
            throw new IllegalArgumentException("Превышен лимит генерации за один запрос (максимум 100 000)");
        }

        int[] createdIds = new int[count];
        int edgesCreated = 0;

        for (int i = 0; i < count; i++) {
            int id = idCounter.incrementAndGet();
            createdIds[i] = id;

            String name = NAMES[random.nextInt(NAMES.length)] + "_" + id;
            String city = CITIES[random.nextInt(CITIES.length)];
            int birthYear = 1975 + random.nextInt(33);
            double rating = Math.round(random.nextDouble() * 50.0) / 10.0;
            String bio = BIOS[random.nextInt(BIOS.length)];

            Profile profile;
            int typeRoll = random.nextInt(10);

            if (typeRoll == 0) {
                profile = new DeletedProfile(id, name, city, birthYear, new Connections[0], rating, bio);
            } else if (typeRoll == 1) {
                List<String> adminGroups = new ArrayList<>(1);
                adminGroups.add(GROUPS[random.nextInt(GROUPS.length)]);
                profile = new AdminProfile(id, name, city, birthYear, new Connections[0], adminGroups, rating, bio);
            } else {
                profile = new UserProfile(id, name, city, birthYear, new Connections[0], rating, bio);
            }

            graph.addVertex(profile);

            if (i > 0 && random.nextDouble() < 0.6) {
                int friendId = createdIds[random.nextInt(i)];
                int strength = 1 + random.nextInt(10);
                if (graph.addEdge(id, friendId, strength)) {
                    edgesCreated++;
                }
            }
        }

        return edgesCreated;
    }
}