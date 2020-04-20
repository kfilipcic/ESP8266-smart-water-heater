package com.example.rijekasmarthomeapp;

public class Device {
    private String name;
    private boolean state;

    public Device(String name, boolean state) {
        this.name = name;
        this.state = state;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public boolean getState() {
        return state;
    }
}
