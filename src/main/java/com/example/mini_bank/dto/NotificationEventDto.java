package com.example.mini_bank.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {
    private String eventType;
    private String toEmail;
    private String subject;
    private String message;
    private Map<String, Object> additionalData;
    private LocalDateTime timestamp;
}