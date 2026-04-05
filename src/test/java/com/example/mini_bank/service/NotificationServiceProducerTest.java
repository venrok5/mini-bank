package com.example.mini_bank.service;


import com.example.mini_bank.dto.NotificationEventDto;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.EventType;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class NotificationServiceProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationEventDto> kafkaTemplate;

    private NotificationServiceProducer producer;

    @BeforeEach
    void setUp() {
        producer = new NotificationServiceProducer(kafkaTemplate);

        // fake SendResult
        SendResult<String, NotificationEventDto> sendResult =
                new SendResult<>(null, new RecordMetadata(null, 0, 0, 0L, 0L, 0, 0));

        SettableListenableFuture<SendResult<String, NotificationEventDto>> future = new SettableListenableFuture<>();
        future.set(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), any(NotificationEventDto.class)))
                .thenReturn(future);
    }

    @Test
    void sendUserRegisteredEvent_shouldSendEvent() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setUsername("tester");

        producer.sendUserRegisteredEvent(user);

        ArgumentCaptor<NotificationEventDto> captor = ArgumentCaptor.forClass(NotificationEventDto.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(user.getEmail()), captor.capture());

        NotificationEventDto event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.USER_REGISTERED.name());
        assertThat(event.getToEmail()).isEqualTo(user.getEmail());
        assertThat(event.getAdditionalData()).containsEntry("userId", user.getId());
    }

    @Test
    void sendTransactionEvent_shouldSendEvent() {
        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setType(TransactionType.TRANSFER);

        String email = "user@mail.com";

        producer.sendTransactionEvent(tx, email);

        ArgumentCaptor<NotificationEventDto> captor = ArgumentCaptor.forClass(NotificationEventDto.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(email), captor.capture());

        NotificationEventDto event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.TRANSACTION_CREATED.name());
        assertThat(event.getAdditionalData()).containsEntry("transactionId", tx.getId());
    }

    @Test
    void sendCardBlockRequestEvent_shouldSendEvent() {
        User user = new User();
        user.setEmail("carduser@mail.com");

        Card card = new Card();
        card.setId(5L);
        card.setCardNumber("1234567890123456");
        card.setOwner(user);

        producer.sendCardBlockRequestEvent(card);

        ArgumentCaptor<NotificationEventDto> captor = ArgumentCaptor.forClass(NotificationEventDto.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(user.getEmail()), captor.capture());

        NotificationEventDto event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.CARD_BLOCK_REQUESTED.name());
        assertThat(event.getAdditionalData()).containsEntry("cardId", card.getId());
    }

    @Test
    void sendCardStatusChangedEvent_shouldSendEvent() {
        User user = new User();
        user.setEmail("owner@mail.com");

        Card card = new Card();
        card.setId(7L);
        card.setCardNumber("9876543210987654");
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);

        producer.sendCardStatusChangedEvent(card);

        ArgumentCaptor<NotificationEventDto> captor = ArgumentCaptor.forClass(NotificationEventDto.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(user.getEmail()), captor.capture());

        NotificationEventDto event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.CARD_STATUS_CHANGED.name());
        assertThat(event.getAdditionalData()).containsEntry("status", card.getStatus().name());
    }

    @Test
    void sendUserStatusChangedEvent_shouldSendEvent() {
        User user = new User();
        user.setId(3L);
        user.setEmail("status@mail.com");
        user.setStatus(UserStatus.ACTIVE);

        producer.sendUserStatusChangedEvent(user);

        ArgumentCaptor<NotificationEventDto> captor = ArgumentCaptor.forClass(NotificationEventDto.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(user.getEmail()), captor.capture());

        NotificationEventDto event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.USER_STATUS_CHANGED.name());
        assertThat(event.getAdditionalData()).containsEntry("status", user.getStatus().name());
    }
}