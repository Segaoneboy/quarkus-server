package ru.test.domain;

import ru.test.domain.datastructure.Connections;

public class Profile {
    private int id;
    private String name;
    private String city;
    private int birthYear;
    private transient Connections[] friendshipList;
    private transient int friendshipCount;

    private double rating = 0.0; // v2
    private String bio = "";     // v3

    public Profile() {
        this.friendshipList = new Connections[4];
        this.friendshipCount = 0;
    }

    public Profile(int id, String name, String city, int birthYear, Connections[] friendshipList) {
        this(id, name, city, birthYear, friendshipList, 0.0, "");
    }

    public Profile(int id, String name, String city, int birthYear, Connections[] friendshipList, double rating, String bio) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.birthYear = birthYear;
        this.rating = rating;
        this.bio = (bio != null) ? bio : "";
        if (friendshipList != null) {
            this.friendshipList = friendshipList;
            this.friendshipCount = friendshipList.length;
        } else {
            this.friendshipList = new Connections[4];
            this.friendshipCount = 0;
        }
    }

    public void addFriendship(int friendId, int strength) {
        if (friendshipList == null) {
            friendshipList = new Connections[4];
            friendshipCount = 0;
        }
        for (int i = 0; i < friendshipCount; i++) {
            if (friendshipList[i] != null && friendshipList[i].getFriendId() == friendId) {
                friendshipList[i].setConnection(strength);
                return;
            }
        }
        if (friendshipCount >= friendshipList.length) {
            int newCapacity = (friendshipList.length == 0) ? 4 : friendshipList.length * 2;
            Connections[] newArr = new Connections[newCapacity];
            System.arraycopy(friendshipList, 0, newArr, 0, friendshipCount);
            friendshipList = newArr;
        }
        friendshipList[friendshipCount++] = new Connections(friendId, strength);
    }

    public boolean removeFriendship(int friendId) {
        for (int i = 0; i < friendshipCount; i++) {
            if (friendshipList[i].getFriendId() == friendId) {
                int moved = friendshipCount - i - 1;
                if (moved > 0) {
                    System.arraycopy(friendshipList, i + 1, friendshipList, i, moved);
                }
                friendshipList[--friendshipCount] = null;
                return true;
            }
        }
        return false;
    }

    public int getId() { return this.id; }
    public String getName() { return this.name; }
    public String getCity() { return this.city; }
    public int getBirthYear() { return this.birthYear; }
    public Connections[] getFriendshipList() { return this.friendshipList; }
    public int getFriendshipCount() { return this.friendshipCount; }
    public double getRating() { return this.rating; }
    public String getBio() { return this.bio; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setBirthYear(int birthYear) { this.birthYear = birthYear; }
    public void setRating(double rating) { this.rating = rating; }
    public void setBio(String bio) { this.bio = (bio != null) ? bio : ""; }
}