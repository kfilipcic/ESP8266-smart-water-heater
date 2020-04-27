package com.example.rijekasmarthomeapp;

public class WaterHeater extends Device {
    private String waterTemperature;

    public WaterHeater (String name, boolean state, String waterTemperature) {
        super(name, state);
        this.waterTemperature = waterTemperature;
    }

    public String getWaterTemperature() {
        return waterTemperature;
    }

    public void setWaterTemperature(String waterTemperature) {
        this.waterTemperature = waterTemperature;
    }
}
