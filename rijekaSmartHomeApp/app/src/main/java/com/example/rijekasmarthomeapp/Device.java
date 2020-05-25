package com.example.rijekasmarthomeapp;

public class Device {
    private String name;
    private String state;
    private int id_num;
    private long startTimeHHmm = -1;
    private long endTimeHHmm = -1;
    private long startDate = -1;

    Device(String name, String state, int id_num) {
        this.name = name;
        this.state = state;
        this.id_num = id_num;
    }

    Device(String name, int id_num) {
        this.id_num = id_num;
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

    public int getId_num() {
        return id_num;
    }

    public void setId_num(int id_num) {
        this.id_num = id_num;
    }

    public long getStartTimeHHmm() {
        return startTimeHHmm;
    }

    public long getEndTimeHHmm() {
        return endTimeHHmm;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartTimeHHmm(long startTimeHHmm) {
        this.startTimeHHmm = startTimeHHmm;
    }

    public void setEndTimeHHmm(long endTimeHHmm) {
        this.endTimeHHmm = endTimeHHmm;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }
}
