package com.sungjiduk.backend.user.constants;

public enum Role {
    USER("user"), ADMIN("admin");
    private String value;
    Role(String value){
        this.value = value;
    }
}
