package com.example.mini_bank.controller;

import com.example.mini_bank.controllers.AdminController;
import com.example.mini_bank.dto.*;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.mappers.CardMapper;
import com.example.mini_bank.mappers.TransactionMapper;
import com.example.mini_bank.mappers.UserMapper;
import com.example.mini_bank.security.jwt.JwtUtils;
import com.example.mini_bank.service.UserService;
import com.example.mini_bank.service.ValidationService;
import com.example.mini_bank.service.admin.AdminService;
import com.example.mini_bank.service.client.ClientService;
import com.example.mini_bank.service.security.BankUserDetailsService;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false) 
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private CardMapper cardMapper;

    @MockBean
    private UserMapper userMapper;
   

    @MockBean
    private ClientService clientService;

    @MockBean
    private UserService userService;



    @MockBean
    private TransactionMapper transactionMapper;
    
    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private BankUserDetailsService bankUserDetailsService;

    @MockBean
    private ValidationService validationService;


    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CARD_ID = 1L;

    // ------------------ Helper DTOs ------------------
    private CardDto createCardDto() {
        return new CardDto(TEST_CARD_ID, "**** **** **** 1234", "Test User",
                LocalDate.now().plusYears(3), CardStatus.ACTIVE, BigDecimal.valueOf(1000));
    }

    private UserResponseDto createUserDto() {
        return new UserResponseDto(TEST_USER_ID, "TestUser", "test@mail.com", UserStatus.ACTIVE);
    }

    private TransactionResponseDto createTransactionDto() {
        return new TransactionResponseDto(1L, "****1234", "****5678", BigDecimal.valueOf(100));
    }

    private BankStats createBankStats() {
        return new BankStats(100, 500, BigDecimal.valueOf(1000000), 50);
    }

    // ------------------ Admin Endpoints ------------------

    @Test
    void issueCard_success() throws Exception {
        CardDto cardDto = createCardDto();

        when(adminService.issueCard(anyLong(), anyString())).thenReturn(new Card());
        when(cardMapper.toDto(any())).thenReturn(cardDto);

        mockMvc.perform(post("/api/admin/cards")
                        .param("userId", TEST_USER_ID.toString())
                        .param("ownerName", "Test User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_CARD_ID))
                .andExpect(jsonPath("$.ownerName").value("Test User"));
    }

    @Test
    void changeCardStatus_success() throws Exception {
        CardDto cardDto = createCardDto();

        when(adminService.changeCardStatus(anyLong(), any())).thenReturn(new Card());
        when(cardMapper.toDto(any())).thenReturn(cardDto);

        mockMvc.perform(put("/api/admin/cards/{cardId}/status", TEST_CARD_ID)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_CARD_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void changeUserStatus_success() throws Exception {
        UserResponseDto userDto = createUserDto();

        when(adminService.changeUserStatus(anyLong(), anyBoolean())).thenReturn(new User());
        when(userMapper.toExternalDto(any())).thenReturn(userDto);

        mockMvc.perform(put("/api/admin/users/{userId}/status", TEST_USER_ID)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.username").value("TestUser"));
    }

    @Test
    void getAllCards_success() throws Exception {
        CardDto cardDto = createCardDto();
        Page<Card> cards = new PageImpl<>(List.of(new Card()));

        when(adminService.getAllCards(any(Pageable.class))).thenReturn(cards);
        when(cardMapper.toDto(any())).thenReturn(cardDto);

        mockMvc.perform(get("/api/admin/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_CARD_ID));
    }

    @Test
    void getUserTransactions_success() throws Exception {
        TransactionResponseDto transactionDto = createTransactionDto();
        Page<Transaction> transactions = new PageImpl<>(List.of(new Transaction()));

        when(adminService.getUserTransactions(anyLong(), any(Pageable.class))).thenReturn(transactions);
        when(transactionMapper.toResponseDto(any())).thenReturn(transactionDto);

        mockMvc.perform(get("/api/admin/users/{userId}/transactions", TEST_USER_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void getBankStats_success() throws Exception {
        BankStats stats = createBankStats();

        when(adminService.getBankStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").value(100))
                .andExpect(jsonPath("$.cardsCount").value(500))
                .andExpect(jsonPath("$.totalBalance").value(1000000));
    }

    // ------------------ Error Scenarios ------------------

    @Test
    void changeCardStatus_cardNotFound() throws Exception {
        when(adminService.changeCardStatus(anyLong(), any()))
                .thenThrow(new EntityNotFoundException("Card not found"));

        mockMvc.perform(put("/api/admin/cards/{cardId}/status", TEST_CARD_ID)
                        .param("status", "ACTIVE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeUserStatus_userNotFound() throws Exception {
        when(adminService.changeUserStatus(anyLong(), anyBoolean()))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(put("/api/admin/users/{userId}/status", TEST_USER_ID)
                        .param("active", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllCards_invalidPage() throws Exception {
        mockMvc.perform(get("/api/admin/cards")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }
}