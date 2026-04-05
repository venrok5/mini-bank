package com.example.mini_bank.service;

import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.repository.UserRepository;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValidationService validationService;

    @Mock
    private NotificationServiceProducer notificationService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setUsername("johndoe");
        user.setStatus(UserStatus.BLOCKED);
    }

    @Test
    void setActive_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(validationService.requireUser(any())).thenReturn(user);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.setActive(1L, true);

        assertNotNull(result);
        assertEquals(UserStatus.ACTIVE, result.getStatus());
    }

    @Test
    void findByUsername_success() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("johndoe");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void getAll_success() {
        Page<User> page = new PageImpl<>(List.of(user));
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(user, result.getContent().get(0));
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(validationService.requireUser(any())).thenReturn(user);

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(user, result);
    }

    @Test
    void saveUser_success() {
        doAnswer(invocation -> null).when(notificationService).sendUserRegisteredEvent(any(User.class));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.saveUser(user);

        verify(userRepository).save(user);
        verify(notificationService).sendUserRegisteredEvent(user);
    }
}