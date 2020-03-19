package com.example.documentation_20190713.Retrofit;

public class Message {
    private Integer error;
    private String name;
    private String email;
    private String value;
    private String password;
    private String other;

    public Integer getError() {
        return error;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getValue() {
        return value;
    }

    public String getPassword() {
        return password;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }
}