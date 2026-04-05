package com.example.mini_bank.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.mini_bank.TestDataFactory;
import com.example.mini_bank.config.KafkaTestConfig;
import com.example.mini_bank.config.PostgresTestConfig;
import com.example.mini_bank.dto.BankStats;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.enums.UserStatus;

import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.service.admin.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@ActiveProfiles("test")
@Import({KafkaTestConfig.class, PostgresTestConfig.class})
@ComponentScan(basePackages = {
    "com.example.mini_bank",     
    "com.example.mini_bank.mappers"
})
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser1;
    private User testUser2;
    private Card testCard1;
    private Card testCard2;
    private Transaction testTransaction;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = userRepository.save(TestDataFactory.createUser("admin_test1@example.com", "encodedPass"));
        testUser2 = userRepository.save(TestDataFactory.createUser("admin_test2@example.com", "encodedPass"));

        testCard1 = cardRepository.save(TestDataFactory.createCard(testUser1, "1111222233334444", BigDecimal.valueOf(1000), CardStatus.ACTIVE));
        testCard2 = cardRepository.save(TestDataFactory.createCard(testUser2, "5555666677778888", BigDecimal.valueOf(500), CardStatus.BLOCKED));

        testTransaction = transactionRepository.save(TestDataFactory.createTransaction(testCard1, testCard2, BigDecimal.valueOf(50), TransactionType.TRANSFER));
    }

    @Test
    void testIssueCard_Success() throws Exception {
    	// Получаем начальное количество карт
        List<Card> initialCards = cardRepository.findByOwnerId(testUser1.getId());
        int initialCardCount = initialCards.size();

        // Создаем одну карту
        mockMvc.perform(post("/api/admin/cards")
                .param("userId", String.valueOf(testUser1.getId()))
                .param("ownerName", "Test Owner Full Name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Test Owner Full Name"))
                .andExpect(jsonPath("$.status").value(CardStatus.ACTIVE.toString()))
                .andExpect(jsonPath("$.balance").value(0.0));

        // Проверяем, что добавилась только одна карта
        List<Card> userCards = cardRepository.findByOwnerId(testUser1.getId());
        assertEquals(initialCardCount + 1, userCards.size());
        
        Card newCard = userCards.stream()
                .filter(card -> "Test Owner Full Name".equals(card.getOwnerName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("New card not found"));
        
        assertEquals(CardStatus.ACTIVE, newCard.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(newCard.getBalance()));
        assertNotNull(newCard.getCardNumber());
        assertEquals(16, newCard.getCardNumber().length());
    }

    @Test
    void testIssueCard_UserNotFound() throws Exception {
        mockMvc.perform(post("/api/admin/cards")
                .param("userId", "9999")
                .param("ownerName", "Test Owner"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testIssueCard_InvalidParameters() throws Exception {
        // Пустое имя владельца
        mockMvc.perform(post("/api/admin/cards")
                .param("userId", String.valueOf(testUser1.getId()))
                .param("ownerName", ""))
                .andExpect(status().isBadRequest());

        // Слишком короткое имя
        mockMvc.perform(post("/api/admin/cards")
                .param("userId", String.valueOf(testUser1.getId()))
                .param("ownerName", "A"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChangeCardStatus_Success() throws Exception {
        mockMvc.perform(put("/api/admin/cards/{cardId}/status", testCard1.getId())
                .param("status", CardStatus.BLOCKED.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCard1.getId()))
                .andExpect(jsonPath("$.status").value(CardStatus.BLOCKED.toString()));

        Card updatedCard = cardRepository.findById(testCard1.getId()).orElseThrow();
        assertEquals(CardStatus.BLOCKED, updatedCard.getStatus());
    }

    @Test
    void testChangeCardStatus_CardNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/cards/{cardId}/status", 9999L)
                .param("status", CardStatus.ACTIVE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testChangeCardStatus_InvalidStatus() throws Exception {
        mockMvc.perform(put("/api/admin/cards/{cardId}/status", testCard1.getId())
                .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

   
    @Test
    void testChangeUserStatus_Success() throws Exception {
        mockMvc.perform(put("/api/admin/users/{userId}/status", testUser1.getId())
                .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser1.getId()))
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        User updatedUser = userRepository.findById(testUser1.getId()).orElseThrow();
        assertEquals(UserStatus.BLOCKED, updatedUser.getStatus());
    }

    @Test
    void testChangeUserStatus_UserNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/users/{userId}/status", 9999L)
                .param("active", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllCards_Success() throws Exception {
        mockMvc.perform(get("/api/admin/cards")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].maskedNumber").exists())
                .andExpect(jsonPath("$.content[0].balance").exists());
    }

    @Test
    void testGetAllCards_Pagination() throws Exception {
        // Первая страница с 1 элементом
        mockMvc.perform(get("/api/admin/cards")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        // Вторая страница с 1 элементом
        mockMvc.perform(get("/api/admin/cards")
                .param("page", "1")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void testGetUserTransactions_Success() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}/transactions", testUser1.getId())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(testTransaction.getId()))
                .andExpect(jsonPath("$.content[0].amount").value(50.00))
                .andExpect(jsonPath("$.content[0].fromCardMasked").exists())
                .andExpect(jsonPath("$.content[0].toCardMasked").exists());
    }

    @Test
    void testGetUserTransactions_UserNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}/transactions", 9999L)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserTransactions_NoTransactions() throws Exception {
        // Создаем нового пользователя без транзакций
        User newUser = userRepository.save(TestDataFactory.createUser("no_transactions@example.com", "encodedPass"));

        mockMvc.perform(get("/api/admin/users/{userId}/transactions", newUser.getId())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetBankStats_Success() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersCount").exists())
                .andExpect(jsonPath("$.cardsCount").exists())
                .andExpect(jsonPath("$.totalBalance").exists())
                .andExpect(jsonPath("$.todayTransfersCount").exists()) 
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void testGetBankStats_Consistency() throws Exception {
        // Проверяем, что статистика совпадает с реальными данными в БД
        BankStats stats = adminService.getBankStats();
        
        long actualUsersCount = userRepository.count();
        long actualCardsCount = cardRepository.count();
        BigDecimal actualTotalBalance = cardRepository.findAll().stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long actualTodayTransfers = transactionRepository.countByDate(LocalDate.now());
        
        assertEquals(actualUsersCount, stats.getUsersCount());
        assertEquals(actualCardsCount, stats.getCardsCount());
        assertEquals(0, actualTotalBalance.compareTo(stats.getTotalBalance()));
        assertEquals(actualTodayTransfers, stats.getTodayTransfersCount()); // Исправлено на getTodayTransfers()
    }

    @Test
    void testValidationErrors() throws Exception {
        // Неверные параметры пагинации
        mockMvc.perform(get("/api/admin/cards")
                .param("page", "-1")
                .param("size", "5"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/cards")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/cards")
                .param("page", "0")
                .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

}