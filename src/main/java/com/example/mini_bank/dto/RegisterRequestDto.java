package com.example.mini_bank.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.example.mini_bank.enums.Roles;
import com.example.mini_bank.security.validation.UniqueEmail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  
@AllArgsConstructor
public class RegisterRequestDto {
	@NotBlank(message = "Email is required")
	@Email(message = "Email should be valid")
	@UniqueEmail(message = "Email already exists")
	private String email;
    
	@NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    /*@Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
        message = "Password must contain at least one digit, one lowercase and one uppercase letter"
    )*/
	private String password;
	
	private Roles role;
	
	@NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	private String username;
}
