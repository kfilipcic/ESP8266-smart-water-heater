package com.example.rijekasmarthomeapp;

public class WaterHeater extends Device {
    private String waterTemperature;

    public WaterHeater (String name, String state, String waterTemperature) {
        super(name, state);
        this.waterTemperature = waterTemperature;
    }

    public WaterHeater (String name) {
        super(name);
        this.waterTemperature = "N/A";
    }

    public String getWaterTemperature() {
        return waterTemperature;
    }

    public void setWaterTemperature(String waterTemperature) {
        this.waterTemperature = waterTemperature;
    }
}
