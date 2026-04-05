package com.example.mini_bank.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class LoginRequestDto {
	@NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
	private String email;
    
	@NotBlank(message = "Password is required") 
	private String password;
}
