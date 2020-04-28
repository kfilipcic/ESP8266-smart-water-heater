package com.example.rijekasmarthomeapp;

public class Device {
    private String name;
    private String state;

    Device(String name, String state) {
        this.name = name;
        this.state = state;
    }

    Device(String name) {
        this.name = name;
        this.state = "N/A";
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }
}
