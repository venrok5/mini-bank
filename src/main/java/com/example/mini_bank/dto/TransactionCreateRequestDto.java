package com.example.mini_bank.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.example.mini_bank.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class TransactionCreateRequestDto {
	@NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "fromCardId is required")
    private Long fromCardId;

    @NotNull(message = "toCardId is required")
    private Long toCardId;

    private String description;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
