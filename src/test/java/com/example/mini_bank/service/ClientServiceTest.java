package com.example.mini_bank.service;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.service.client.ClientService;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;
import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;
import com.example.mini_bank.dto.TransactionFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ValidationService validationService;

    @Mock
    private NotificationServiceProducer notificationService;

    @InjectMocks
    private ClientService clientService;

    private Card cardFrom;
    private Card cardTo;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        cardFrom = new Card();
        cardFrom.setId(1L);
        cardFrom.setBalance(BigDecimal.valueOf(100));
        cardFrom.setOwner(user); 
        cardFrom.setStatus(CardStatus.ACTIVE);

        cardTo = new Card();
        cardTo.setId(2L);
        cardTo.setBalance(BigDecimal.valueOf(50));
        cardTo.setOwner(user); 
        cardTo.setStatus(CardStatus.ACTIVE);
    }

    @Test
    void getUserCards_withStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(cardFrom));
        when(cardRepository.findByOwnerIdAndStatus(1L, CardStatus.ACTIVE, pageable)).thenReturn(page);

        Page<Card> result = clientService.getUserCards(1L, CardStatus.ACTIVE, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(cardFrom, result.getContent().get(0));
    }

    @Test
    void getUserCards_withoutStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(List.of(cardFrom));
        when(cardRepository.findByOwnerId(1L, pageable)).thenReturn(page);

        Page<Card> result = clientService.getUserCards(1L, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(cardFrom, result.getContent().get(0));
    }

    @Test
    void requestCardBlock_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardFrom));
        doNothing().when(validationService).validateCardOwnership(1L, cardFrom);
        when(validationService.requireCard(any())).thenReturn(cardFrom);
        when(cardRepository.save(any(Card.class))).thenReturn(cardFrom);
        doNothing().when(notificationService).sendCardBlockRequestEvent(cardFrom);

        clientService.requestCardBlock(1L, 1L);

        assertEquals(CardStatus.BLOCK_REQUESTED, cardFrom.getStatus());
        verify(notificationService).sendCardBlockRequestEvent(cardFrom);
    }

    @Test
    void transferBetweenUsersCards_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardFrom));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(cardTo));
        doNothing().when(validationService).validateTransfer(1L, cardFrom, cardTo, BigDecimal.valueOf(50));
        when(validationService.requireCard(any())).thenReturn(cardFrom).thenReturn(cardTo);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).sendTransactionEvent(any(Transaction.class), anyString());

        Transaction tx = clientService.transferBetweenUsersCards(1L, 1L, 2L, BigDecimal.valueOf(50), "Test transfer");

        assertEquals(BigDecimal.valueOf(50), cardFrom.getBalance());
        assertEquals(BigDecimal.valueOf(100), cardTo.getBalance());
        assertEquals(TransactionType.TRANSFER, tx.getType());
        assertEquals(user.getEmail(), cardFrom.getOwner().getEmail());
        verify(notificationService).sendTransactionEvent(tx, cardFrom.getOwner().getEmail());
    }

    @Test
    void getCardBalance_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardFrom));
        doNothing().when(validationService).validateCardOwnership(1L, cardFrom);
        when(validationService.requireCard(any())).thenReturn(cardFrom);

        BigDecimal balance = clientService.getCardBalance(1L, 1L);

        assertEquals(cardFrom.getBalance(), balance);
    }

    @Test
    void getCardTransactionsHistory_allCases() {
        PageRequest pageable = PageRequest.of(0, 10);
        TransactionFilter filter = new TransactionFilter();
        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardFrom));
        when(validationService.requireCard(any())).thenReturn(cardFrom);
        doNothing().when(validationService).validateCardOwnership(1L, cardFrom);

        // mock all variants
        when(transactionRepository.findByCardId(anyLong(), eq(pageable))).thenReturn(page);
        when(transactionRepository.findByCardIdAndType(anyLong(), any(), eq(pageable))).thenReturn(page);
        when(transactionRepository.findByCardIdAndTimestampBetween(anyLong(), any(), any(), eq(pageable))).thenReturn(page);
        when(transactionRepository.findByCardIdAndTypeAndTimestampBetween(anyLong(), any(), any(), any(), eq(pageable))).thenReturn(page);

        Page<Transaction> result = clientService.getCardTransactionsHistory(1L, 1L, filter, pageable);
        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getStatement_success() {
        PageRequest pageable = PageRequest.of(0, 10);
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now();
        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardFrom));
        when(validationService.requireCard(any())).thenReturn(cardFrom);
        doNothing().when(validationService).validateCardOwnership(1L, cardFrom);
        when(transactionRepository.findByIdAndTimestampBetween(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(pageable)
        )).thenReturn(page);

        Page<Transaction> result = clientService.getStatement(1L, 1L, from, to, pageable);

        assertEquals(1, result.getTotalElements());
    }
}