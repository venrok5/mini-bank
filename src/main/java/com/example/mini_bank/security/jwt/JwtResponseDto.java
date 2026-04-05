package com.example.mini_bank.security.jwt;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class JwtResponseDto {
	private String token;
	private Long userId;
}
