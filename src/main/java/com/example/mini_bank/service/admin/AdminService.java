package com.example.mini_bank.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.example.mini_bank.dto.BankStats;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.service.BaseService;
import com.example.mini_bank.service.ValidationService;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService extends BaseService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ValidationService validationService;
    private final NotificationServiceProducer notificationService;

    @Transactional
    public Card issueCard(Long userId, String ownerName) {
        logAction("Issuing card for userId={} ownerName={}", userId, ownerName);
        
        User user = validationService.requireUser(userRepository.findById(userId));
        validationService.validateUserActive(user);

        Card card = new Card();
        card.setOwner(user);
        card.setOwnerName(ownerName);
        card.setCardNumber(generateCardNumber());
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);

        Card saved = cardRepository.save(card);
        
        logAction("Card issued successfully cardId={} userId={}", saved.getId(), userId);
        notificationService.sendCardStatusChangedEvent(saved);
        
        return saved;
    }

    public Card changeCardStatus(Long cardId, CardStatus status) {
        logAction("Changing card status cardId={} status={}", cardId, status);
       
        Card card = validationService.requireCard(cardRepository.findById(cardId));
        card.setStatus(status);
        Card saved = cardRepository.save(card);
        
        logAction("Card status changed successfully cardId={} status={}", saved.getId(), saved.getStatus());
        notificationService.sendCardStatusChangedEvent(saved);
        
        return saved;
    }

    public User changeUserStatus(Long userId, boolean active) {
        logAction("Changing user status userId={} active={}", userId, active);
        
        User user = validationService.requireUser(userRepository.findById(userId));
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.BLOCKED);
        User saved = userRepository.save(user);
        
        logAction("User status changed successfully userId={} status={}", saved.getId(), saved.getStatus());
        notificationService.sendUserStatusChangedEvent(saved);
        
        return saved;
    }

    public Page<Card> getAllCards(Pageable pageable) {
        logAction("Fetching all cards page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Card> cards = cardRepository.findAll(pageable);
        
        logAction("Fetched {} cards", cards.getContent().size());
        
        return cards;
    }

    public Page<Transaction> getUserTransactions(Long userId, Pageable pageable) {
        logAction("Fetching transactions for userId={} page={} size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        
        validationService.requireUser(userRepository.findById(userId));
        Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageable);
        
        logAction("Fetched {} transactions for userId={}", transactions.getContent().size(), userId);
        
        return transactions;
    }

    public BankStats getBankStats() {
        logAction("Fetching bank statistics");
       
        long usersCount = userRepository.count();
        long cardsCount = cardRepository.count();
        BigDecimal totalBalance = cardRepository.findAll().stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long todayTransfers = transactionRepository.countByDate(LocalDate.now());
        BankStats stats = new BankStats(usersCount, cardsCount, totalBalance, todayTransfers);
        
        logAction("Bank stats retrieved users={} cards={} totalBalance={} todayTransfers={}", usersCount, cardsCount, totalBalance, todayTransfers);
        return stats;
    }

    private String generateCardNumber() {
        String uuidDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        
        if (uuidDigits.length() >= 16) {
            return uuidDigits.substring(0, 16);
        } else {
            // add 0000 if numbers < 16
            return String.format("%016d", Long.parseLong(uuidDigits));
        }
    }
}