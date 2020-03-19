package com.example.documentation_20190713.Retrofit;

public class Position {
    private Integer id;
    private Float value_x;
    private Float value_y;
    private Float value_z;
    private Message messages;

    public Position(Float value_x, Float value_y, Float value_z) {
        this.value_x = value_x;
        this.value_y = value_y;
        this.value_z = value_z;
    }

    public Integer getId() {
        return id;
    }

    public Float getValueX() {
        return value_x;
    }

    public Float getValueY() {
        return value_y;
    }

    public Float getValueZ() {
        return value_z;
    }

    public Message getMessages() {
        return messages;
    }
}