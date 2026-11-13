package ru.test.domain;

import ru.test.domain.datastructure.Connections;
import ru.test.domain.exception.CsvParserException;
import ru.test.domain.exception.ErrorCode;
import ru.test.server.dto.UpdateProfileRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminProfile extends Profile implements Editable {
    private List<String> groups = new ArrayList<>();

    public AdminProfile() {
        super();
    }

    public AdminProfile(int id, String name, String city, int birthYear, Connections[] friendshipList, List<String> groups) {
        super(id, name, city, birthYear, friendshipList);
        this.groups = (groups != null) ? groups : new ArrayList<>();
    }

    public AdminProfile(int id, String name, String city, int birthYear, Connections[] friendshipList, List<String> groups, double rating, String bio) {
        super(id, name, city, birthYear, friendshipList, rating, bio);
        this.groups = (groups != null) ? groups : new ArrayList<>();
    }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }

    @Override
    public Map<String, String> getEditableFields() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Имя", getName());
        map.put("Город", getCity());
        map.put("Год рождения", String.valueOf(getBirthYear()));
        map.put("Рейтинг", String.valueOf(getRating()));
        map.put("О себе", getBio());
        map.put("Группы", String.join(", ", groups));
        return map;
    }

//    @Override
//    public void update(Map<String, String> fields) throws CsvParserException {
//        if (fields.containsKey("Имя")) setName(fields.get("Имя"));
//        if (fields.containsKey("Город")) setCity(fields.get("Город"));
//        if (fields.containsKey("Год рождения")) {
//            try {
//                setBirthYear(Integer.parseInt(fields.get("Год рождения")));
//            } catch (NumberFormatException e) {
//                throw new CsvParserException(ErrorCode.BAD_NUMBER, "Год рождения должен быть числом");
//            }
//        }
//        if (fields.containsKey("Рейтинг")) {
//            try {
//                setRating(Double.parseDouble(fields.get("Рейтинг")));
//            } catch (NumberFormatException e) {
//                throw new CsvParserException(ErrorCode.BAD_NUMBER, "Рейтинг должен быть числом");
//            }
//        }
//        if (fields.containsKey("О себе")) setBio(fields.get("О себе"));
//        if (fields.containsKey("Группы")) {
//            List<String> newGroups = new ArrayList<>();
//            String rawGroups = fields.get("Группы");
//            if (rawGroups != null && !rawGroups.isBlank()) {
//                for (String g : rawGroups.split(",")) {
//                    newGroups.add(g.trim());
//                }
//            }
//            this.groups = newGroups;
//        }
//    }


    @Override
    public void updateFrom(UpdateProfileRequest req) throws CsvParserException {
        if (req == null) return;
        if (req.name() != null) {
            if (req.name().isBlank()) {
                throw new CsvParserException(ErrorCode.INVALID_FIELD_VALUE, "Имя профиля не может быть пустым");
            }
            setName(req.name());
        }
        if (req.city() != null) {
            setCity(req.city());
        }
        if (req.birthYear() != null) {
            int year = req.birthYear();
            if (year < 1900 || year > 2026) {
                throw new CsvParserException(ErrorCode.BAD_NUMBER, "Некорректный год рождения: " + year);
            }
            setBirthYear(year);
        }
        if (req.rating() != null) {
            if (req.rating() < 0.0) {
                throw new CsvParserException(ErrorCode.BAD_NUMBER, "Рейтинг не может быть отрицательным");
            }
            setRating(req.rating());
        }
        if (req.bio() != null) {
            setBio(req.bio());
        }
        if (req.groups() != null) {
            this.groups = new ArrayList<>(req.groups());
        }
    }

}