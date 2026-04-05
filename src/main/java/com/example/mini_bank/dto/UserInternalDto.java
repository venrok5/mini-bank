package com.example.mini_bank.dto;

import com.example.mini_bank.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor 
public class UserInternalDto {
	private Long id;
	private String username;
	private String email;
	private UserStatus status;
}
