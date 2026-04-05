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
public class UserResponseDto {
	private Long id;
	private String username;
	private String maskedEmail;
	private UserStatus status;
	
	/*public UserStatus getStatus() {
        return status;
    }
	
	public void setStatus(UserStatus status) {
        this.status = status;
    }*/
}
