package com.example.mini_bank.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import com.example.mini_bank.security.validation.UniqueEmail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor 
public class UserCreateDto {
	
	@Email
	@NotBlank 
	@UniqueEmail
	private String email;
    
	@NotBlank 
	@Size(min = 6) 
	//@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")
	private String password;
	
	@NotBlank
    @Size(min = 2, max = 100)
    private String username;
}
