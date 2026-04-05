package com.example.mini_bank.mappers;
import com.example.mini_bank.util.MaskingUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.example.mini_bank.dto.UserInternalDto;
import com.example.mini_bank.dto.UserResponseDto;
import com.example.mini_bank.entity.User;

@Component
@RequiredArgsConstructor
public class UserMapper {
    
    private final MaskingUtil maskingUtil;
    
    public UserResponseDto toExternalDto(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setMaskedEmail(maskingUtil.maskEmail(user.getEmail()));
        dto.setStatus(user.getStatus());
        
        return dto;
    }
    
    public UserInternalDto toInternalDto(User user) {
        if (user == null) {
            return null;
        }
        
        UserInternalDto dto = new UserInternalDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setStatus(user.getStatus());
        
        return dto;
    }
    
    public User toEntity(UserInternalDto dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setId(dto.getId());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setStatus(dto.getStatus());
        
        return user;
    }
    
    public User toEntitySafe(UserInternalDto dto) {
        return dto != null ? toEntity(dto) : null;
    }
}