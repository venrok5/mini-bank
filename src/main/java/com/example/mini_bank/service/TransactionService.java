package com.example.mini_bank.service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService extends BaseService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final ValidationService validationService;

    @Transactional
    public Transaction transfer(Long fromCardId, Long toCardId, BigDecimal amount, String idempotencyKey) {
        logAction("Transfer requested fromCard={} toCard={} amount={} idempotencyKey={}", fromCardId, toCardId, amount, idempotencyKey);

        // transaction dublicat check
        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            logAction("Duplicate transaction detected idempotencyKey={}", idempotencyKey);
            
            return existingTx.get();
        }

        Card from = validationService.requireCard(cardRepository.findById(fromCardId));
        Card to = validationService.requireCard(cardRepository.findById(toCardId));

        validationService.validatePositiveAmount(amount);
        validationService.validateTransfer(from.getOwner().getId(), from, to, amount);
        
        // update  balance
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        cardRepository.save(from);
        cardRepository.save(to);

        Transaction tx = new Transaction();
        tx.setFromCard(from);
        tx.setToCard(to);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setIdempotencyKey(idempotencyKey);
        
        // new transaction
        Transaction saved = transactionRepository.save(tx);
       
        logAction("Transfer completed transactionId={}", saved.getId());
       
        return saved;
    }
    
    // 1 card hystory
    public Page<Transaction> getCardTransactions(Long cardId, Pageable pageable) {
        logAction("Fetching transactions for cardId={} page={} size={}", cardId, pageable.getPageNumber(), pageable.getPageSize());
        
        return transactionRepository.findByFromCardIdOrToCardId(cardId, cardId, pageable);
    }
    
    // all user transactions histry
    public Page<Transaction> getUserTransactions(Long userId, Pageable pageable) {
        logAction("Fetching transactions for userId={} page={} size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        
        return transactionRepository.findByUserId(userId, pageable);
    }
}