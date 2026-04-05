package com.example.mini_bank.util;

import org.springframework.stereotype.Component;

@Component
public class MaskingUtil {
	
	public static String maskCardNumber(String number) {
        if (number == null || number.length() < 4) {
            return "****";
        }
        return "**** **** **** " + number.substring(number.length() - 4);
    }
	
	public static String maskEmail(String email) {
	    	
		if (email == null || !email.contains("@")) {
			return "****";
		}
	        
		String[] parts = email.split("@");
		String local = parts[0];
		String domain = parts[1];
	        
		if (local.length() <= 2) {
			return local.charAt(0) + "****@" + domain;
		}
	        
		return local.substring(0, 2) + "****@" + domain;
	}
}
