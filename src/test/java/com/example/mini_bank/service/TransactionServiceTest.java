package com.example.mini_bank.service;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;

import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private TransactionService transactionService;

    private Card fromCard;
    private Card toCard;
    private Transaction tx;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setBalance(BigDecimal.valueOf(100));
        fromCard.setOwner(user);
        fromCard.setStatus(CardStatus.ACTIVE);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setBalance(BigDecimal.valueOf(50));
        toCard.setOwner(user);
        toCard.setStatus(CardStatus.ACTIVE);

        tx = new Transaction();
        tx.setId(1L);
        tx.setFromCard(fromCard);
        tx.setToCard(toCard);
        tx.setAmount(BigDecimal.valueOf(10));
        tx.setIdempotencyKey("key-123");
    }

    @Test
    void transfer_success() {
        // no twins
        when(transactionRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.empty());

        // card mocks
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // no validation
        doNothing().when(validationService).validatePositiveAmount(any());
        doNothing().when(validationService).validateTransfer(anyLong(), any(), any(), any());

        // requireCard gives Optional-value
        when(validationService.requireCard(any(Optional.class)))
                .thenAnswer(invocation -> ((Optional<Card>) invocation.getArgument(0)).orElseThrow());

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.transfer(1L, 2L, BigDecimal.valueOf(10), "key-123");

        assertEquals(BigDecimal.valueOf(90), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(60), toCard.getBalance());
        assertEquals("key-123", result.getIdempotencyKey());
    }

    @Test
    void transfer_duplicateTransaction_returnsExisting() {
        when(transactionRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(tx));

        Transaction result = transactionService.transfer(1L, 2L, BigDecimal.valueOf(10), "key-123");

        assertEquals(tx, result);
    }

    @Test
    void getCardTransactions_success() {
        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findByFromCardIdOrToCardId(1L, 1L, PageRequest.of(0,10))).thenReturn(page);

        Page<Transaction> result = transactionService.getCardTransactions(1L, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getUserTransactions_success() {
        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findByUserId(1L, PageRequest.of(0,10))).thenReturn(page);

        Page<Transaction> result = transactionService.getUserTransactions(1L, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }
}