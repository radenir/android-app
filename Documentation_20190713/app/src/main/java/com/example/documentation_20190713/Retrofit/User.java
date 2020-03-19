package com.example.documentation_20190713.Retrofit;

public class User {
    private Integer id;
    private String name;
    private String email;
    private String password;
    private String token;

    private Message messages;

    //for register
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    //for login
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public Message getMessages() {
        return messages;
    }

}
