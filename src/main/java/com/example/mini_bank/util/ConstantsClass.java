package com.example.mini_bank.util;

import java.math.BigDecimal;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ConstantsClass {
	// Rate limit requests count
    public static final Integer REQUESTS_PER_MINUTE_LIMIT = 100;
    
    public static final String FILE_STORAGE_ROOT = "./uploads";
    public static final String LOGS_DIRECTORY = "./logs";
    
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_DEBUG = "DEBUG";
    public static final String LOG_LEVEL_ERROR = "ERROR";
    
    public static final BigDecimal MAX_LIMIT = BigDecimal.valueOf(1000000.00);
}