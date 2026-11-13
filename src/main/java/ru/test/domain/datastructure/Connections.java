package ru.test.domain.datastructure;


public class Connections {
    private int friendId;
    private int connection;

    public Connections(){
    }
    public Connections(int friendId, int connection ){
        this.friendId = friendId;
        this.connection = connection;
    }   
    public int getFriendId(){
        return this.friendId;
    }
    public int getConnection(){
        return this.connection;
    }

    public void setFriendId(int friendId){
        this.friendId = friendId;
    }
    public void setConnection(int connection){
        this.connection = connection;
    }

}
