package com.example.mini_bank.controller;

import com.example.mini_bank.controllers.UserController;
import com.example.mini_bank.dto.*;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.mappers.CardMapper;
import com.example.mini_bank.mappers.TransactionMapper;
import com.example.mini_bank.mappers.UserMapper;
import com.example.mini_bank.security.jwt.JwtUtils;
import com.example.mini_bank.service.CardService;
import com.example.mini_bank.service.UserService;
import com.example.mini_bank.service.ValidationService;
import com.example.mini_bank.service.client.ClientService;
import com.example.mini_bank.service.security.BankUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) 
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    @MockBean
    private UserService userService;

    @MockBean
    private CardMapper cardMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private TransactionMapper transactionMapper;
    
    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private PasswordEncoder passwordEncoder;
    
    @MockBean
    private CardService cardService;

    @MockBean
    private BankUserDetailsService bankUserDetailsService;

    @MockBean
    private ValidationService validationService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CARD_ID = 1L;

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

    // ------------------ User Methods ------------------

    @Test
    void getUser_success() throws Exception {
        UserResponseDto userDto = createUserDto();
        when(userService.getUserById(TEST_USER_ID)).thenReturn(new com.example.mini_bank.entity.User());
        when(userMapper.toExternalDto(any())).thenReturn(userDto);

        mockMvc.perform(get("/api/client/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.username").value("TestUser"));
    }

    @Test
    void getUser_notFound() throws Exception {
        when(userService.getUserById(TEST_USER_ID)).thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/client/{userId}", TEST_USER_ID))
                .andExpect(status().isNotFound());
    }

    // ------------------ Card Methods ------------------

    @Test
    void getCards_success() throws Exception {
        CardDto cardDto = createCardDto();
        Page<com.example.mini_bank.entity.Card> cards = new PageImpl<>(List.of(new com.example.mini_bank.entity.Card()));

        when(clientService.getUserCards(eq(TEST_USER_ID), any(), any(Pageable.class))).thenReturn(cards);
        when(cardMapper.toDto(any())).thenReturn(cardDto);

        mockMvc.perform(get("/api/client/cards")
                        .param("userId", TEST_USER_ID.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_CARD_ID))
                .andExpect(jsonPath("$.content[0].ownerName").value("Test User"));
    }

    @Test
    void blockCard_success() throws Exception {
        Card card = new Card();
        card.setId(TEST_CARD_ID);
        card.setOwnerName("Test User");

        CardDto cardDto = new CardDto();
        cardDto.setId(TEST_CARD_ID);
        cardDto.setOwnerName("Test User");

        doNothing().when(clientService).requestCardBlock(TEST_USER_ID, TEST_CARD_ID);
        when(clientService.getUserCards(eq(TEST_USER_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card)));
        when(cardMapper.toDto(card)).thenReturn(cardDto);
        when(cardService.findById(TEST_CARD_ID)).thenReturn(Optional.of(card));
        when(validationService.requireCard(any())).thenReturn(card);
        when(cardMapper.toDtoSafe(card)).thenReturn(cardDto);

        mockMvc.perform(post("/api/client/cards/{cardId}/block", TEST_CARD_ID)
                        .param("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_CARD_ID))
                .andExpect(jsonPath("$.ownerName").value("Test User"));
    }

    @Test
    void blockCard_notFound() throws Exception {
        when(clientService.getUserCards(eq(TEST_USER_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post("/cards/{cardId}/block", TEST_CARD_ID) 
                        .param("userId", TEST_USER_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalance_success() throws Exception {
        when(clientService.getCardBalance(TEST_USER_ID, TEST_CARD_ID))
                .thenReturn(BigDecimal.valueOf(1234));

        mockMvc.perform(get("/api/client/cards/{cardId}/balance", TEST_CARD_ID)
                        .param("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("1234"));
    }

    // ------------------ Transactions ------------------

    @Test
    void transferBetweenOwnCards_success() throws Exception {
        TransactionCreateRequestDto request = new TransactionCreateRequestDto();
        request.setFromCardId(TEST_CARD_ID);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(100));
        request.setDescription("Test transfer");
        request.setType(TransactionType.TRANSFER);
        request.setIdempotencyKey("unique-key-123"); 

        TransactionResponseDto transactionDto = createTransactionDto();

        when(clientService.transferBetweenUsersCards(
                eq(TEST_USER_ID),
                eq(TEST_CARD_ID),
                eq(2L),
                eq(request.getAmount()),
                eq(request.getDescription())
        )).thenReturn(new com.example.mini_bank.entity.Transaction());

        when(transactionMapper.toResponseDto(any())).thenReturn(transactionDto);

        mockMvc.perform(post("/api/client/transfer")
                        .param("userId", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getTransactionsHistory_success() throws Exception {
        TransactionFilter filter = new TransactionFilter(); // can be empty
        Page<com.example.mini_bank.entity.Transaction> transactions = new PageImpl<>(List.of(new com.example.mini_bank.entity.Transaction()));
        TransactionResponseDto dto = createTransactionDto();

        when(clientService.getCardTransactionsHistory(eq(TEST_USER_ID), eq(TEST_CARD_ID), any(), any(Pageable.class)))
                .thenReturn(transactions);
        when(transactionMapper.toResponseDto(any())).thenReturn(dto);

        mockMvc.perform(post("/api/client/cards/{cardId}/transactions", TEST_CARD_ID)
                        .param("userId", TEST_USER_ID.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void getStatement_success() throws Exception {
        Page<com.example.mini_bank.entity.Transaction> transactions = new PageImpl<>(List.of(new com.example.mini_bank.entity.Transaction()));
        TransactionResponseDto dto = createTransactionDto();

        when(clientService.getStatement(eq(TEST_USER_ID), eq(TEST_CARD_ID),
                any(), any(), any(Pageable.class)))
                .thenReturn(transactions);
        when(transactionMapper.toResponseDto(any())).thenReturn(dto);

        mockMvc.perform(get("/api/client/cards/{cardId}/statement", TEST_CARD_ID)
                        .param("userId", TEST_USER_ID.toString())
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void getStatement_invalidDate_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/client/cards/{cardId}/statement", TEST_CARD_ID)
                        .param("userId", TEST_USER_ID.toString())
                        .param("from", "2025-02-01")
                        .param("to", "2025-01-01")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }
}