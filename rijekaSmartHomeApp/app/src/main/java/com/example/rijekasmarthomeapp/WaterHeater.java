package com.example.rijekasmarthomeapp;

public class WaterHeater extends Device {
    private String temperature;
    private boolean tempNotifs;
    private boolean autoRegulateTemperature;
    private double minTemp = -500.0;
    private double maxTemp = 500.0;

    public WaterHeater (String name, String state, String temperature, int id_num) {
        super(name, state, id_num);
        this.temperature = temperature;
    }

    public WaterHeater (String name, int id_num) {
        super(name, id_num);
        this.temperature = "N/A";
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public void setTempNotifs(boolean tempNotifs) {
        this.tempNotifs = tempNotifs;
    }

    public boolean getTempNotifs() {
        return tempNotifs;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public void setMinTemp(double minTemp) {
        this.minTemp = minTemp;
    }

    public void setMaxTemp(double maxTemp) {
        this.maxTemp = maxTemp;
    }

    public void setAutoRegulateTemperature(boolean autoRegulateTemperature) {
        this.autoRegulateTemperature = autoRegulateTemperature;
    }

    public boolean getAutoRegulateTemperature() { return autoRegulateTemperature; }
}
