package com.example.mini_bank.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class CardCreateDto {
	@NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotNull(message = "Expiration date is required")
    private LocalDate expirationDate;
}
