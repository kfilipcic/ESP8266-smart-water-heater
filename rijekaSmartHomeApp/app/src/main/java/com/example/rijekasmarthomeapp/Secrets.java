package com.example.rijekasmarthomeapp;

public class Secrets {
    private String username = "rijekastan";
    private String password = "A3C01610A252";
    //private String url = "http://psih.duckdns.org/";
    private String url = "http://192.168.0.56/";

    public Secrets() {

    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
