package com.example.mini_bank.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class TransactionResponseDto {
	private Long id;
    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
}
