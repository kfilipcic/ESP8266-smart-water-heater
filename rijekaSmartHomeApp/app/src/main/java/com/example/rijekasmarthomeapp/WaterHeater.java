package com.example.rijekasmarthomeapp;

public class WaterHeater extends Device {
    private String name;
    private boolean state;
    private String waterTemperature;
    private String roomTemperature;

    public WaterHeater (String name, boolean state, String waterTemperature, String roomTemperature) {
        super(name, state);
        this.waterTemperature = waterTemperature;
        this.roomTemperature = roomTemperature;
    }

    public String getName() {
        return name;
    }

    public String getWaterTemperature() {
        return waterTemperature;
    }

    public String getRoomTemperature() {
        return roomTemperature;
    }
}
