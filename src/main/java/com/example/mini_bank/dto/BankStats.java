package com.example.mini_bank.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BankStats {
    private long usersCount;
    private long cardsCount;
    private BigDecimal totalBalance;
    private long todayTransfersCount;
}