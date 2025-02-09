package com.example.unimate;
public class RoomModel {
    private String building;
    private String floor;
    private String roomNumber;
    private String imageKey;

    public RoomModel(String building, String floor, String roomNumber, String imageKey) {
        this.building = building;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.imageKey = imageKey;
    }

    public String getBuilding() {
        return building;
    }

    public String getFloor() {
        return floor;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getImageKey() {
        return imageKey;
    }
}
