package com.example.mini_bank.service;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;
import com.example.mini_bank.repository.UserRepository;

import com.example.mini_bank.service.admin.AdminService;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ValidationService validationService;

    @Mock
    private NotificationServiceProducer notificationService;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private Card card;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setBalance(BigDecimal.valueOf(100));
        card.setStatus(CardStatus.ACTIVE);
    }

    
    @Test
    void issueCard_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(validationService.requireUser(any())).thenReturn(user);
        
        // Card given in save()
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Card result = adminService.issueCard(1L, "John Doe");

        assertNotNull(result);
        assertEquals(user, result.getOwner());
        assertEquals("John Doe", result.getOwnerName());
        verify(notificationService).sendCardStatusChangedEvent(result);
    }

    @Test
    void changeCardStatus_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(validationService.requireCard(any())).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);

        Card result = adminService.changeCardStatus(1L, CardStatus.BLOCKED);

        assertEquals(CardStatus.BLOCKED, result.getStatus());
        verify(notificationService).sendCardStatusChangedEvent(result);
    }

    @Test
    void changeUserStatus_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(validationService.requireUser(any())).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User result = adminService.changeUserStatus(1L, false);

        assertEquals(UserStatus.BLOCKED, result.getStatus());
        verify(notificationService).sendUserStatusChangedEvent(result);
    }

    @Test
    void getAllCards_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(Collections.singletonList(card));

        when(cardRepository.findAll(pageable)).thenReturn(page);

        Page<Card> result = adminService.getAllCards(pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUserTransactions_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx = new Transaction();
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(tx));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(validationService.requireUser(any())).thenReturn(user);
        when(transactionRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<Transaction> result = adminService.getUserTransactions(1L, pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getBankStats_success() {
        when(userRepository.count()).thenReturn(5L);
        when(cardRepository.count()).thenReturn(3L);

        Card card2 = new Card();
        card2.setBalance(BigDecimal.valueOf(200));
        when(cardRepository.findAll()).thenReturn(List.of(card, card2));
        when(transactionRepository.countByDate(LocalDate.now())).thenReturn(7L);

        var stats = adminService.getBankStats();

        assertEquals(5L, stats.getUsersCount());
        assertEquals(3L, stats.getCardsCount());
        assertEquals(BigDecimal.valueOf(300), stats.getTotalBalance());
        assertEquals(7L, stats.getTodayTransfersCount());
    }
}