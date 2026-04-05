package com.example.mini_bank.enums;

public enum Roles {
    ADMIN,
    USER;

    public boolean isAdmin() { 
    	return this == ADMIN; 
    }
}
