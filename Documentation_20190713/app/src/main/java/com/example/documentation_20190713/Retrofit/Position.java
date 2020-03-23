package com.example.documentation_20190713.Retrofit;

public class Position {
    private Integer id;
    private Float valuex;
    private Float valuey;
    private Float valuez;
    private Message messages;

    public Position(Float valuex, Float valuey, Float valuez) {
        this.valuex = valuex;
        this.valuey = valuey;
        this.valuez = valuez;
    }

    public Integer getId() {
        return id;
    }

    public Float getValueX() {
        return valuex;
    }

    public Float getValueY() {
        return valuey;
    }

    public Float getValueZ() {
        return valuez;
    }

    public Message getMessages() {
        return messages;
    }
}