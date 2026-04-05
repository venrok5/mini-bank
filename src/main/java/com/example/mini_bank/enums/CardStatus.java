package com.example.mini_bank.enums;

/**
	NEW -> ACTIVE
	ACTIVE -> BLOCK_REQUESTED -> ACTIVE/BLOCKED
	ACTIVE/BLOCKED -> INACTIVE
 */

public enum CardStatus {
	ACTIVE,
	BLOCK_REQUESTED,
    BLOCKED,
    EXPIRED,
    DELETED
}
