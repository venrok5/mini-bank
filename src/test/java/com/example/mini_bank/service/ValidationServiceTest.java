package com.example.mini_bank.service;

import com.example.mini_bank.dto.RegisterRequestDto;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.exception.AuthException;
import com.example.mini_bank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ValidationService validationService;

    // ===== validate Registration =====
    @Test
    void validateRegistration_emailExists_throwsAuthException() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setEmail("test@example.com");
        dto.setUsername("user1");

        Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class, () -> validationService.validateRegistration(dto));
        assertEquals("Email already exists", ex.getMessage());
    }

    @Test
    void validateRegistration_usernameExists_throwsAuthException() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setEmail("email@example.com");
        dto.setUsername("user1");

        Mockito.when(userRepository.existsByEmail("email@example.com")).thenReturn(false);
        Mockito.when(userRepository.existsByEmail("user1")).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class, () -> validationService.validateRegistration(dto));
        assertEquals("Email already taken", ex.getMessage());
    }

    @Test
    void validateRegistration_success() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setEmail("email@example.com");
        dto.setUsername("user1");

        Mockito.when(userRepository.existsByEmail("email@example.com")).thenReturn(false);
        Mockito.when(userRepository.existsByEmail("user1")).thenReturn(false);

        assertDoesNotThrow(() -> validationService.validateRegistration(dto));
    }

    // ===== validate Transfer =====
    @Test
    void validateTransfer_insufficientFunds_throwsIllegalArgumentException() {
        User user = new User();
        user.setId(1L);

        Card from = new Card();
        from.setId(1L);
        from.setOwner(user);
        from.setBalance(BigDecimal.valueOf(50));

        Card to = new Card();
        to.setId(2L);
        to.setOwner(user);
        to.setBalance(BigDecimal.valueOf(100));

        assertThrows(IllegalArgumentException.class,
                () -> validationService.validateTransfer(1L, from, to, BigDecimal.valueOf(100)));
    }

    @Test
    void validateTransfer_differentUser_throwsSecurityException() {
        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        Card from = new Card();
        from.setId(1L);
        from.setOwner(user1);
        from.setBalance(BigDecimal.valueOf(100));

        Card to = new Card();
        to.setId(2L);
        to.setOwner(user2);
        to.setBalance(BigDecimal.valueOf(100));

        assertThrows(SecurityException.class,
                () -> validationService.validateTransfer(1L, from, to, BigDecimal.valueOf(50)));
    }

    @Test
    void validateTransfer_sameCard_throwsIllegalArgumentException() {
        User user = new User();
        user.setId(1L);

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setBalance(BigDecimal.valueOf(100));

        assertThrows(IllegalArgumentException.class,
                () -> validationService.validateTransfer(1L, card, card, BigDecimal.valueOf(50)));
    }

    @Test
    void validateTransfer_blockedCard_throwsIllegalStateException() {
        User user = new User();
        user.setId(1L);

        Card from = new Card();
        from.setId(1L);
        from.setOwner(user);
        from.setBalance(BigDecimal.valueOf(100));
        from.setStatus(CardStatus.BLOCKED);

        Card to = new Card();
        to.setId(2L);
        to.setOwner(user);
        to.setBalance(BigDecimal.valueOf(100));

        assertThrows(IllegalStateException.class,
                () -> validationService.validateTransfer(1L, from, to, BigDecimal.valueOf(50)));
    }

    @Test
    void validateTransfer_success() {
        User user = new User();
        user.setId(1L);

        Card from = new Card();
        from.setId(1L);
        from.setOwner(user);
        from.setBalance(BigDecimal.valueOf(100));

        Card to = new Card();
        to.setId(2L);
        to.setOwner(user);
        to.setBalance(BigDecimal.valueOf(50));

        assertDoesNotThrow(() -> validationService.validateTransfer(1L, from, to, BigDecimal.valueOf(50)));
    }

    // ===== validate Card Ownership =====
    @Test
    void validateCardOwnership_wrongUser_throwsSecurityException() {
        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user2);

        assertThrows(SecurityException.class,
                () -> validationService.validateCardOwnership(1L, card));
    }

    @Test
    void validateCardOwnership_success() {
        User user = new User();
        user.setId(1L);

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user);

        assertDoesNotThrow(() -> validationService.validateCardOwnership(1L, card));
    }

    // ===== validate Card Not Blocked =====
    @Test
    void validateCardNotBlocked_blockedCard_throwsIllegalStateException() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.BLOCKED);;

        assertThrows(IllegalStateException.class, () -> validationService.validateCardNotBlocked(card));
    }

    @Test
    void validateCardNotBlocked_success() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);

        assertDoesNotThrow(() -> validationService.validateCardNotBlocked(card));
    }

    // ===== validateUserActive =====
    @Test
    void validateUserActive_blockedUser_throwsSecurityException() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.BLOCKED);

        assertThrows(SecurityException.class, () -> validationService.validateUserActive(user));
    }

    @Test
    void validateUserActive_success() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        assertDoesNotThrow(() -> validationService.validateUserActive(user));
    }

    // ===== requireCard & requireUser =====
    @Test
    void requireCard_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> validationService.requireCard(Optional.empty()));
    }

    @Test
    void requireCard_success() {
        Card card = new Card();
        card.setId(1L);
        Card result = validationService.requireCard(Optional.of(card));
        assertEquals(card, result);
    }

    @Test
    void requireUser_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> validationService.requireUser(Optional.empty()));
    }

    @Test
    void requireUser_success() {
        User user = new User();
        user.setId(1L);
        User result = validationService.requireUser(Optional.of(user));
        assertEquals(user, result);
    }

    // ===== validatePositiveAmount =====
    @Test
    void validatePositiveAmount_zeroOrNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.validatePositiveAmount(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> validationService.validatePositiveAmount(BigDecimal.valueOf(-1)));
    }

    @Test
    void validatePositiveAmount_success() {
        assertDoesNotThrow(() -> validationService.validatePositiveAmount(BigDecimal.valueOf(10)));
    }
}