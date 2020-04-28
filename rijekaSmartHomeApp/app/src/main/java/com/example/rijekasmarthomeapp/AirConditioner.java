package com.example.rijekasmarthomeapp;

public class AirConditioner extends Device {
    private String roomTemperature;
    private String moisture;

    public AirConditioner(String name, String state, String roomTemperature, String moisture) {
        super(name, state);
        this.roomTemperature = roomTemperature;
        this.moisture = moisture;
    }

    public AirConditioner(String name, String roomTemperature, String moisture) {
        super(name);
        this.roomTemperature = roomTemperature;
        this.moisture = moisture;
    }

    public AirConditioner(String name) {
        super(name);
        this.roomTemperature = "N/A";
        this.moisture = "N/A";
    }

    public void setRoomTemperature(String roomTemperature) {
        this.roomTemperature = roomTemperature;
    }

    public String getRoomTemperature() {
        return roomTemperature;
    }

    public void setMoisture(String moisture) {
        this.moisture = moisture;
    }

    public String getMoisture() {
        return moisture;
    }
}
