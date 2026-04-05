package com.example.mini_bank.service;

import java.math.BigDecimal;
import com.example.mini_bank.exception.AuthException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.mini_bank.dto.RegisterRequestDto;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.util.ConstantsClass;

import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ValidationService extends BaseService {
	
	private final UserRepository userRepository;	
	
	public void validateTransfer(Long userId, Card from, Card to, BigDecimal amount) {
	    logAction("Validating transfer for userId={} fromCard={} toCard={} amount={}", 
	             userId, from.getId(), to.getId(), amount);

	    if (amount == null) {
	        logError("Transfer validation failed: amount is null");
	        throw new IllegalArgumentException("Amount cannot be null");
	    }

	    
	    if (!from.getOwner().getId().equals(userId)) {
	        logError("Transfer validation failed: source card {} does not belong to userId={}", 
	                from.getId(), userId);
	        throw new SecurityException("Source card does not belong to user");
	    }
	    
	    if (!to.getOwner().getId().equals(userId)) {
	        logError("Transfer validation failed: destination card {} does not belong to userId={}", 
	                to.getId(), userId);
	        throw new SecurityException("Destination card does not belong to user");
	    }

	    if (from.getStatus() != CardStatus.ACTIVE) {
	        logError("Transfer validation failed: source card {} is not active, status={}", 
	                from.getId(), from.getStatus());
	        throw new IllegalStateException("Source card is not active");
	    }
	    
	    if (to.getStatus() != CardStatus.ACTIVE) {
	        logError("Transfer validation failed: destination card {} is not active, status={}", 
	                to.getId(), to.getStatus());
	        throw new IllegalStateException("Destination card is not active");
	    }

	    if (from.getId().equals(to.getId())) {
	        logError("Transfer validation failed: cannot transfer to the same card, cardId={}", 
	                from.getId());
	        throw new IllegalArgumentException("Cannot transfer to the same card");
	    }

	    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
	        logError("Transfer validation failed: amount must be positive, amount={}", amount);
	        throw new IllegalArgumentException("Transfer amount must be positive");
	    }

	    if (from.getBalance().compareTo(amount) < 0) {
	        logError("Transfer validation failed: insufficient funds on cardId={}, balance={}, amount={}", 
	                from.getId(), from.getBalance(), amount);
	        throw new IllegalArgumentException("Insufficient funds");
	    }

	    if (amount.compareTo(ConstantsClass.MAX_LIMIT) > 0) {
	        logError("Transfer validation failed: amount exceeds maximum limit, amount={}, limit={}", 
	                amount, ConstantsClass.MAX_LIMIT);
	        throw new IllegalArgumentException("Transfer amount exceeds maximum limit");
	    }

	    logAction("Transfer validation passed for userId={}", userId);
	}
	
	public void validateCardOwnership(Long userId, Card card) {
        logAction("Validating card ownership for userId={} cardId={}", userId, card.getId());
        
        
        if (!card.getOwner().getId().equals(userId)) {
            logError("Card ownership validation failed for userId={} cardId={}", userId, card.getId());
            
            throw new SecurityException("Access denied: card does not belong to user");
        }
    }
	
	public void validateCardNotBlocked(Card card) {
        logAction("Validating card is not blocked cardId={}", card.getId());
        
        if (card.isBlocked()) {
            logError("Card is blocked cardId={}", card.getId());
            
            throw new IllegalStateException("Card is blocked");
        }
    }
	
	public void validateUserActive(User user) {
        logAction("Validating user active userId={}", user.getId());
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            logError("User is blocked userId={}", user.getId());
            
            throw new SecurityException("User is blocked");
        }
    }
	
	public Card requireCard(Optional<Card> cardOpt) {
        return cardOpt.orElseThrow(() -> {
            logError("Card not found");
            
            return new NoSuchElementException("Card not found");
        });
    }
	
	public User requireUser(Optional<User> userOpt) {
        return userOpt.orElseThrow(() -> {
            logError("User not found");
           
            return new NoSuchElementException("User not found");
        });
    }
	
	public void validatePositiveAmount(BigDecimal amount) {
        logAction("Validating positive amount={}", amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logError("Amount is not positive amount={}", amount);
           
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
	
	public void validateRegistration(RegisterRequestDto req) {
        logAction("Validating registration for email={} username={}", req.getEmail(), req.getUsername());
        if (userRepository.existsByEmail(req.getEmail())) {
            logError("Email already exists email={}", req.getEmail());
            
            throw new AuthException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(req.getUsername())) {
            logError("Username already taken username={}", req.getUsername());
            
            throw new AuthException("Email already taken", HttpStatus.BAD_REQUEST);
        }
    }
	
}