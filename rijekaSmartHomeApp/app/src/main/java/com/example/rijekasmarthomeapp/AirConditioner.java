package com.example.rijekasmarthomeapp;

public class AirConditioner extends Device {
    private String temperature;
    private String moisture;
    private boolean tempNotifs = false;
    private boolean autoRegulateTemperature;
    private double minTemp = -500.0;
    private double maxTemp = 500.0;
    private int fanLevel = -1;
    private int tempLevel = 0;
    private int mode = -1;
    private boolean swing = false;
    private boolean sleep = false;
    private boolean direct = false;

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

    public void setAutoRegulateTemperature(boolean autoRegulateTemperature) {
        this.autoRegulateTemperature = autoRegulateTemperature;
    }

    public boolean getAutoRegulateTemperature() { return autoRegulateTemperature; }

    public int getFanLevel() {
        return fanLevel;
    }

    public void setFanLevel(int fanLevel) {
        this.fanLevel = fanLevel;
    }

    public void setSwing(boolean swing) {
        this.swing = swing;
    }

    public boolean getSwing() { return swing; }

    public void setSleep(boolean sleep) {
        this.sleep = sleep;
    }

    public boolean getSleep() { return sleep; }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public boolean getDirect() { return direct; }

    public int getTempLevel() {
        return tempLevel;
    }

    public void setTempLevel(int tempLevel) {
        this.tempLevel = tempLevel;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
