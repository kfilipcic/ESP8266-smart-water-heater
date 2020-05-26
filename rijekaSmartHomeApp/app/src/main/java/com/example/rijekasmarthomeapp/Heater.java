package com.example.rijekasmarthomeapp;

public class Heater extends Device {
    private String temperature;
    private String moisture;
    private boolean tempNotifs;
    private boolean autoRegulateTemperature;
    private double minTemp = -500.0;
    private double maxTemp = 500.0;

    public Heater(String name, String state, String temperature, String moisture, int id_num) {
        super(name, state, id_num);
        this.temperature = temperature;
        this.moisture = moisture;
    }

    public Heater(String name, int id_num) {
        super(name, id_num);
        this.temperature = "N/A";
        this.moisture = "N/A";
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getTemperature() {
        return temperature;
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
