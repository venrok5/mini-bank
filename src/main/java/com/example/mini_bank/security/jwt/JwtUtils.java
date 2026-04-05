package com.example.mini_bank.security.jwt;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.example.mini_bank.security.userdetails.BankUserDetails;

import java.util.Date;

import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public String generateToken(BankUserDetails userDetails) {
        SecretKey key = jwtProperties.getSecretKey();
        
        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("role", userDetails.getAuthorities().iterator().next().getAuthority())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
                .signWith(key)
                .compact();

        log.info("[JWT] Token generated for user '{}'", userDetails.getUsername());
        
        return token;
    }

    public String getEmailFromToken(String token) {
        SecretKey key = jwtProperties.getSecretKey();
        
        String email = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        log.debug("[JWT] Extracted email '{}' from token", email);
       
        return email;
    }

    public boolean validateJwtToken(String token) {
        try {
            SecretKey key = jwtProperties.getSecretKey();
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            
            log.debug("[JWT] Token validated successfully");
            
            return true;
        } catch (JwtException e) {
            log.warn("[JWT] Token validation failed: {}", e.getMessage());
            
            return false;
        }
    }
}