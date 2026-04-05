package com.example.mini_bank.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByOwnerId(Long userId, Pageable pageable);

    Page<Card> findByOwnerIdAndStatus(Long userId, CardStatus status, Pageable pageable);
    
    Optional<Card> findById(Long id);
    
    List<Card> findByOwnerId(Long userId);

	Optional<User> findByCardNumber(String generatedNumber);
}