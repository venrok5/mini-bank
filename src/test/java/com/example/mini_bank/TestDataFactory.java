package com.example.mini_bank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.Roles;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.enums.UserStatus;

public class TestDataFactory {

    public static User createUser(String email, String encodedPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setUsername(email.split("@")[0]);
        user.setRole(Roles.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public static Card createCard(User owner, String number, BigDecimal balance, CardStatus status) {
        Card card = new Card();
        card.setCardNumber(number);
        card.setOwner(owner);
        card.setOwnerName(owner.getUsername());
        card.setBalance(balance);
        card.setStatus(status);
        card.setExpirationDate(LocalDate.now().plusYears(3));
        return card;
    }

    public static Transaction createTransaction(Card from, Card to, BigDecimal amount, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setFromCard(from);
        tx.setToCard(to);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setTimestamp(LocalDateTime.now());
        tx.setDescription("Test transaction");
        tx.setIdempotencyKey(UUID.randomUUID().toString());
        tx.setTimestamp(LocalDateTime.now());
        return tx;
    }
}