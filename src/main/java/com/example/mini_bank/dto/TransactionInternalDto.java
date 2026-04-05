package com.example.mini_bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.mini_bank.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class TransactionInternalDto {
	private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;

    private Long fromCardId;
    private String fromCardMasked;

    private Long toCardId;
    private String toCardMasked;

    private String description;
    private String idempotencyKey;
}
