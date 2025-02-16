package com.demo.unimate;

public class RoomModel {
    private String buildingName;
    private String floor;
    private String roomNumber;
    private String imageKey;

    public RoomModel(String buildingName, String floor, String roomNumber, String imageKey) {
        this.buildingName = buildingName;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.imageKey = imageKey;
    }

    public String getBuilding() {
        return buildingName;
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
