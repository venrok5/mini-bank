package com.example.mini_bank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.mini_bank.enums.CardStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor 
public class CardDto {
	private Long id;
	private String maskedNumber;
	private String ownerName;
	private LocalDate expirationDate;
	private CardStatus status;
	private BigDecimal balance;
}
