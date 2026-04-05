package com.example.mini_bank.controller;

import com.example.mini_bank.controllers.AuthController;
import com.example.mini_bank.dto.LoginRequestDto;
import com.example.mini_bank.dto.RegisterRequestDto;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.Roles;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.security.jwt.JwtUtils;
import com.example.mini_bank.security.userdetails.BankUserDetails;
import com.example.mini_bank.service.UserService;
import com.example.mini_bank.service.ValidationService;
import com.example.mini_bank.service.security.BankUserDetailsService;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import javax.validation.ValidationException;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // off Spring Security layer
class AuthControllerWebMvcTest {
	
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankUserDetailsService bankUserDetailsService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserService userService;

    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private ValidationService validationService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "test@mail.com";
    private static final String TEST_PASSWORD = "Password123";
    private static final String TEST_USERNAME = "TestUser";

    @Test
    void register_success() throws Exception {
        RegisterRequestDto registerDto = new RegisterRequestDto();
        registerDto.setEmail(TEST_EMAIL);
        registerDto.setPassword(TEST_PASSWORD);
        registerDto.setUsername(TEST_USERNAME);

        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        
        doNothing().when(validationService).validateRegistration(any(RegisterRequestDto.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(validationService, times(1)).validateRegistration(any(RegisterRequestDto.class));
        verify(userService, times(1)).saveUser(ArgumentMatchers.any(User.class));
    }

    @Test
    void register_invalidData_shouldReturnBadRequest() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setEmail("");
        dto.setPassword("123"); 
        dto.setUsername(""); 

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void login_success() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto();
        loginDto.setEmail(TEST_EMAIL);
        loginDto.setPassword(TEST_PASSWORD);

        Authentication auth = mock(Authentication.class);
        User user = new User(1L, TEST_EMAIL, "encodedPassword", Roles.USER, UserStatus.ACTIVE, TEST_USERNAME);
        BankUserDetails userDetails = new BankUserDetails(user);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateToken(userDetails)).thenReturn("jwtToken");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwtToken"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_invalidCredentials_shouldReturnUnauthorized() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto();
        loginDto.setEmail(TEST_EMAIL);
        loginDto.setPassword("wrongPassword");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication error: Invalid email or password"));
    }
    
    @Test
    void register_emailAlreadyExists_shouldReturnBadRequest() throws Exception {
        RegisterRequestDto registerDto = new RegisterRequestDto();
        registerDto.setEmail(TEST_EMAIL);
        registerDto.setPassword(TEST_PASSWORD);
        registerDto.setUsername(TEST_USERNAME);

        doThrow(new ValidationException("Email already exists"))
            .when(validationService).validateRegistration(any(RegisterRequestDto.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void register_success_shouldSetCorrectRoleAndStatus() throws Exception {
        RegisterRequestDto registerDto = new RegisterRequestDto();
        registerDto.setEmail(TEST_EMAIL);
        registerDto.setPassword(TEST_PASSWORD);
        registerDto.setUsername(TEST_USERNAME);

        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        doNothing().when(validationService).validateRegistration(any(RegisterRequestDto.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk());

        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertEquals(Roles.USER, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals(TEST_EMAIL, savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
    }
}