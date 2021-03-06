package ir.tildaweb.tildachat.models.base_models;

import com.google.gson.annotations.SerializedName;

public class Chatroom {

    @SerializedName("id")
    private Integer id;
    @SerializedName("room_id")
    private String roomId;
    @SerializedName("room_title")
    private String roomTitle;
    @SerializedName("room_picture")
    private String roomPicture;
    @SerializedName("type")
    private String type;
    @SerializedName("last_message")
    private Message lastMessage;
    @SerializedName("unseen_count")
    private Integer unseenCount;
    @SerializedName("members_count")
    private Integer membersCount;

    public Integer getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(Integer membersCount) {
        this.membersCount = membersCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public void setRoomTitle(String roomTitle) {
        this.roomTitle = roomTitle;
    }

    public String getRoomPicture() {
        return roomPicture;
    }

    public void setRoomPicture(String roomPicture) {
        this.roomPicture = roomPicture;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Integer getUnseenCount() {
        return unseenCount;
    }

    public void setUnseenCount(Integer unseenCount) {
        this.unseenCount = unseenCount;
    }
}
