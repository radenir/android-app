package com.example.documentation_20190713.Retrofit;

public class Ekg {
    private Integer id;
    private Float value;
    private Message messages;

    public Ekg(Float value) {
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public Float getValue() {
        return value;
    }

    public Message getMessages() {
        return messages;
    }
}