package com.example.mini_bank.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
import com.example.mini_bank.dto.TransactionCreateRequestDto;
import com.example.mini_bank.dto.TransactionFilter;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;
import com.example.mini_bank.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) 
@Testcontainers
@ActiveProfiles("test")
@Import({KafkaTestConfig.class, PostgresTestConfig.class})
@ComponentScan(basePackages = {
	    "com.example.mini_bank",       // beans
	    "com.example.mini_bank.mappers" // MapStruct beans
	})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;
    

    private User testUser;
    private Card testCard1;
    private Card testCard2;
    private Transaction testTransaction;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(TestDataFactory.createUser("test@example.com", "encodedPass"));
        testCard1 = cardRepository.save(TestDataFactory.createCard(testUser, "1111222233334444", BigDecimal.valueOf(1000), CardStatus.ACTIVE));
        testCard2 = cardRepository.save(TestDataFactory.createCard(testUser, "5555666677778888", BigDecimal.valueOf(500), CardStatus.ACTIVE));

        testTransaction = transactionRepository.save(TestDataFactory.createTransaction(testCard1, testCard2, BigDecimal.valueOf(50), TransactionType.TRANSFER));
    }

    @Test
    void testGetUser() throws Exception {
        mockMvc.perform(get("/api/client/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.maskedEmail").exists());
    }

    @Test
    void testGetCards() throws Exception {
        mockMvc.perform(get("/api/client/cards")
                .param("userId", String.valueOf(testUser.getId()))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void testBlockCard() throws Exception {
        mockMvc.perform(post("/api/client/cards/{cardId}/block", testCard1.getId())
                .param("userId", String.valueOf(testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCard1.getId()));

        Card updatedCard = cardRepository.findById(testCard1.getId()).orElseThrow();
        assertEquals(CardStatus.BLOCK_REQUESTED, updatedCard.getStatus());
    }

    @Test
    void testTransferBetweenOwnCards() throws Exception {
        var requestDto = new TransactionCreateRequestDto();
        requestDto.setFromCardId(testCard1.getId());
        requestDto.setToCardId(testCard2.getId());
        requestDto.setAmount(BigDecimal.valueOf(100));
        requestDto.setDescription("Test transfer");
        requestDto.setType(TransactionType.TRANSFER);
        requestDto.setIdempotencyKey(UUID.randomUUID().toString());
        
        
        mockMvc.perform(post("/api/client/transfer")
                .param("userId", String.valueOf(testUser.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCardMasked").exists())
                .andExpect(jsonPath("$.toCardMasked").exists())
                .andExpect(jsonPath("$.amount").value(100));

        assertEquals(0, BigDecimal.valueOf(900).compareTo(cardRepository.findById(testCard1.getId()).get().getBalance()));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(cardRepository.findById(testCard2.getId()).get().getBalance()));
    }

    @Test
    void testGetBalance() throws Exception {
        mockMvc.perform(get("/api/client/cards/{cardId}/balance", testCard1.getId())
                .param("userId", String.valueOf(testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));
    }

    @Test
    void testGetTransactionsHistory() throws Exception {
      
        LocalDate transactionDate = testTransaction.getTimestamp().toLocalDate();
        
        
        LocalDate fromDate = transactionDate.minusDays(2);
        LocalDate toDate = transactionDate; 
        
        TransactionFilter filter = new TransactionFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
      
     // Детальная отладка
        System.out.println("=== DEBUG TRANSACTION SEARCH ===");
        System.out.println("Card ID: " + testCard1.getId());
        System.out.println("From date: " + fromDate.atStartOfDay());
        System.out.println("To date: " + toDate.plusDays(1).atStartOfDay());
        System.out.println("Transaction timestamp: " + testTransaction.getTimestamp());
        System.out.println("Transaction fromCard ID: " + testTransaction.getFromCard().getId());
        System.out.println("Transaction toCard ID: " + testTransaction.getToCard().getId());
        System.out.println("Transaction in range: " + 
            (testTransaction.getTimestamp().isAfter(fromDate.atStartOfDay()) && 
             testTransaction.getTimestamp().isBefore(toDate.plusDays(1).atStartOfDay())));
        
        // Проверим что транзакция действительно существует в БД
        List<Transaction> allTransactions = transactionRepository.findAll();
        System.out.println("All transactions in DB: " + allTransactions.size());
        for (Transaction t : allTransactions) {
            System.out.println("  Transaction " + t.getId() + ": " + t.getTimestamp() + 
                              ", fromCard: " + t.getFromCard().getId() + 
                              ", toCard: " + t.getToCard().getId());
        }
        
        mockMvc.perform(post("/api/client/cards/{cardId}/transactions", testCard1.getId())
                .param("userId", String.valueOf(testUser.getId()))
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
    
    @Test
    void testGetStatement() throws Exception {
        String from = LocalDate.now().minusDays(1).toString();
        String to = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/client/cards/{cardId}/statement", testCard1.getId())
                .param("userId", String.valueOf(testUser.getId()))
                .param("from", from)
                .param("to", to)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetUser_NotFound() throws Exception {
        mockMvc.perform(get("/api/client/{userId}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBlockCard_NotFound() throws Exception {
        mockMvc.perform(post("/api/client/cards/{cardId}/block", 9999)
                .param("userId", String.valueOf(testUser.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void testTransferBetweenOwnCards_InsufficientFunds() throws Exception {
        TransactionCreateRequestDto requestDto = new TransactionCreateRequestDto();
        requestDto.setFromCardId(testCard1.getId());
        requestDto.setToCardId(testCard2.getId());
        requestDto.setAmount(BigDecimal.valueOf(5000)); // > balance (1000)
        requestDto.setDescription("Fail transfer");
        requestDto.setType(TransactionType.TRANSFER);
        requestDto.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/client/transfer")
                .param("userId", String.valueOf(testUser.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); 

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(
            cardRepository.findById(testCard1.getId()).get().getBalance()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(
            cardRepository.findById(testCard2.getId()).get().getBalance()));
    }

    @Test
    void testGetStatement_InvalidDateRange() throws Exception {
        String from = LocalDate.now().plusDays(1).toString();
        String to = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(get("/api/client/cards/{cardId}/statement", testCard1.getId())
                .param("userId", String.valueOf(testUser.getId()))
                .param("from", from)
                .param("to", to))
                .andExpect(status().isBadRequest());
    }
}