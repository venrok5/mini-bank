package com.example.mini_bank.service.kafka;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.mini_bank.service.BaseService;
import com.example.mini_bank.util.MaskingUtil;

import lombok.RequiredArgsConstructor;

import com.example.mini_bank.dto.NotificationEventDto;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;

import com.example.mini_bank.enums.EventType;

@Service
@RequiredArgsConstructor
public class NotificationServiceProducer extends BaseService {

	private final KafkaTemplate<String, NotificationEventDto> kafkaTemplate;
    
    private static final String NOTIFICATIONS_TOPIC = "bank-notifications";
    
    private void sendEvent(NotificationEventDto event) {
        try {
            kafkaTemplate.send(NOTIFICATIONS_TOPIC, event.getToEmail(), event)
                .addCallback(
                    result ->  logAction("Notification event sent: {}", event.getEventType()),
                    ex ->  logAction("Failed to send notification event: {}", event.getEventType(), ex)
                );
        } catch (Exception e) {
        	 logAction("Error sending Kafka event", e);
        }
    }

    public void sendUserRegisteredEvent(User user) {
        sendEvent(newEvent(
        		EventType.USER_REGISTERED.name(),
                user.getEmail(),
                "Welcome to Mini-Bank!",
                String.format("Dear %s, your account created.", user.getUsername()),
                Map.of("userId", user.getId(), "username", user.getUsername()),
                java.time.LocalDateTime.now()
        		));
    }
    
    public void sendTransactionEvent(Transaction transaction, String userEmail) {
        sendEvent(newEvent(
        		EventType.TRANSACTION_CREATED.name(),
                userEmail,
                "New transaction",
                String.format("Amount: %s\nType: %s", transaction.getAmount(), transaction.getType()),
                Map.of("transactionId", transaction.getId(), "amount", transaction.getAmount()),
                java.time.LocalDateTime.now()
        		));
    }
    
    public void sendCardBlockRequestEvent(Card card) {
        sendEvent(newEvent(
        		EventType.CARD_BLOCK_REQUESTED.name(),
                card.getOwner().getEmail(),
                "Request to block card",
                String.format("Card %s requested for blocking.", MaskingUtil.maskCardNumber(card.getCardNumber())),
                Map.of("cardId", card.getId(), "cardNumber", card.getCardNumber()),
                java.time.LocalDateTime.now()
        		));
    }
    
    public void sendCardStatusChangedEvent(Card card) {
        sendEvent(newEvent(
        		EventType.CARD_STATUS_CHANGED.name(),
                card.getOwner().getEmail(),
                "Статус карты изменен",
                String.format("Сard status %s changed on: %s", MaskingUtil.maskCardNumber(card.getCardNumber()), card.getStatus()),
                Map.of("cardId", card.getId(), "status", card.getStatus().name()),
                java.time.LocalDateTime.now()
        		));
    }
    
    public void sendUserStatusChangedEvent(User user) {
        sendEvent(newEvent(
        		EventType.USER_STATUS_CHANGED.name(),
                user.getEmail(),
                "Accaount status changed",
                String.format("Your account status changed on: %s", user.getStatus()),
                Map.of("userId", user.getId(), "status", user.getStatus().name()),
                java.time.LocalDateTime.now()
        		));
    }
    
    private NotificationEventDto newEvent(String eventType, String toEmail, String subject, 
    		String message, Map<String, Object> additionalData, LocalDateTime timestamp) {
    	return new NotificationEventDto(eventType, toEmail, subject, message, additionalData, timestamp);
    }
}
