package com.example.mini_bank.enums;

/**
	DEPOSIT — входящий на пополнение /перевод
	WITHDRAWAL — исходящий на списание
	TRANSFER — между картами пользователя или юзерами внутри банка
 */

public enum TransactionType {
	DEPOSIT,
    WITHDRAWAL,
    ALL,
    TRANSFER 
}
