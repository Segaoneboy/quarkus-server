package ru.test.data.serialization.dto;

import com.google.gson.annotations.SerializedName;

public class EdgeDto {

    @SerializedName("from_id")
    private int fromId;

    @SerializedName("friend_id")
    private int friendId;

    @SerializedName("strength")
    private int strength;

    public EdgeDto(){}

    public EdgeDto(int fromId,int friendId, int strength){
        this.fromId = fromId;
        this.friendId = friendId;
        this.strength = strength;
    }
    public int getFromId(){
        return fromId;
    }
    public void setFromId(int fromId){
        this.fromId = fromId;
    }
    public int getFriendId(){
        return friendId;
    }
    public void setFriendId(int friendId){
        this.friendId = friendId;
    }
    public int getStrength(){
        return strength;
    }
    public void setStrength(int strength){
        this.strength = strength;
    }

}
