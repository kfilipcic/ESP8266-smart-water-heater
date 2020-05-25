package com.example.rijekasmarthomeapp;

public class AirConditioner extends Device {
    private String temperature;
    private String moisture;
    private boolean tempNotifs = false;
    private boolean autoRegulateTemperature;
    private double minTemp = -500.0;
    private double maxTemp = 500.0;

    public AirConditioner(String name, String state, String temperature, String moisture, int id_num) {
        super(name, state, id_num);
        this.temperature = temperature;
        this.moisture = moisture;
    }

    public AirConditioner(String name, String temperature, String moisture, int id_num) {
        super(name, id_num);
        this.temperature = temperature;
        this.moisture = moisture;
    }

    public AirConditioner(String name, int id_num) {
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
}
