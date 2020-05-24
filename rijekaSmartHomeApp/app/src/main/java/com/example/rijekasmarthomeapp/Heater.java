package com.example.rijekasmarthomeapp;

public class Heater extends Device {
    private String roomTemperature;
    private String moisture;
    private boolean tempNotifs;

    public Heater(String name, String state, String roomTemperature, String moisture) {
        super(name, state);
        this.roomTemperature = roomTemperature;
        this.moisture = moisture;
    }

    public Heater(String name) {
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

    public void setTempNotifs(boolean tempNotifs) {
        this.tempNotifs = tempNotifs;
    }

    public boolean getTempNotifs() {
        return tempNotifs;
    }
}
