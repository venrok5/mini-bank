package com.example.mini_bank.service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.mini_bank.entity.User;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.security.userdetails.BankUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class BankUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("[USERDETAILS] Loading user by email '{}'", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[USERDETAILS] User with email '{}' not found", email);
                    
                    return new UsernameNotFoundException("User not found");
                });

        log.debug("[USERDETAILS] User '{}' found", email);
        
        return new BankUserDetails(user);
    }
}