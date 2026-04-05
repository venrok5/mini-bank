package com.example.mini_bank.service;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService extends BaseService {

	private final UserRepository userRepository;
	private final ValidationService validationService;
	private final NotificationServiceProducer notificationService;

    public User setActive(Long userId, boolean active) {
        logAction("Setting active={} for userId={}", active, userId);
    	User user = validationService.requireUser(userRepository.findById(userId));
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        logAction("User status updated userId={} active={}", saved.getId(), saved.getStatus());
        return saved;
    }

    public Optional<User> findByUsername(String username) {
        logAction("Finding user by username={}", username);
        
        return userRepository.findByUsername(username);
    }

    public Page<User> getAll(Pageable pageable) {
        logAction("Fetching all users page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
    	
        return userRepository.findAll(pageable);
    }
    
    public User getUserById(Long userId) {
        logAction("Fetching user by userId={}", userId);
        
        User user = validationService.requireUser(userRepository.findById(userId));
        
        logAction("User fetched successfully userId={}", userId);
        
        return user;
    }
    
    public void saveUser(User user) {
    	userRepository.save(user);
    	
    	logAction("User account created ={}", user.toString());
    	
    	notificationService.sendUserRegisteredEvent(user);
    }
}