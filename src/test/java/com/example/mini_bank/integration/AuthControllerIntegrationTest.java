package com.example.mini_bank.integration;

import com.example.mini_bank.mappers.CardMapper;
import com.example.mini_bank.mappers.TransactionMapper;
import com.example.mini_bank.mappers.UserMapper;
import com.example.mini_bank.TestDataFactory;
import com.example.mini_bank.config.KafkaTestConfig;
import com.example.mini_bank.config.PostgresTestConfig;
import com.example.mini_bank.controllers.AdminController;
import com.example.mini_bank.dto.LoginRequestDto;
import com.example.mini_bank.dto.RegisterRequestDto;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;


@SpringBootTest
@ComponentScan(basePackages = {
	    "com.example.mini_bank",       // beans
	    "com.example.mini_bank.mappers" // MapStruct beans
	})
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({KafkaTestConfig.class, PostgresTestConfig.class})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void liquibaseShouldRun() {
        List<User> users = userRepository.findAll();
        assertNotNull(users); 
    }
    
    @Test
    void registerUser_success() throws Exception {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setEmail("john@example.com");
        req.setPassword("password123");
        req.setUsername("john");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User registered successfully")));


        assert userRepository.findByEmail("john@example.com").isPresent();
    }

    @Test
    void registerUser_alreadyExists_shouldFail() throws Exception {

        userRepository.save(TestDataFactory.createUser("jane@example.com", passwordEncoder.encode("123456")));

        RegisterRequestDto req = new RegisterRequestDto();
        req.setEmail("jane@example.com");
        req.setPassword("password123");
        req.setUsername("jane");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email already exists")));
    }

    @Test
    void login_success() throws Exception {

        userRepository.save(TestDataFactory.createUser("alice@example.com", passwordEncoder.encode("secret123")));

        LoginRequestDto req = new LoginRequestDto();
        req.setEmail("alice@example.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
				        .andDo(result -> {
				            System.out.println("Status: " + result.getResponse().getStatus());
				            System.out.println("Response body: " + result.getResponse().getContentAsString());
				        })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.userId", notNullValue()));
    }

    @Test
    void login_invalidPassword_shouldFail() throws Exception {
        userRepository.save(TestDataFactory.createUser("bob@example.com", passwordEncoder.encode("realpass")));

        LoginRequestDto req = new LoginRequestDto();
        req.setEmail("bob@example.com");
        req.setPassword("wrongpass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid email or password")));
    }
}