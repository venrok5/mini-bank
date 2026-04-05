package com.example.mini_bank.integration;

import com.example.mini_bank.config.KafkaTestConfig;
import com.example.mini_bank.dto.NotificationEventDto;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.entity.User;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.EventType;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.enums.UserStatus;
import com.example.mini_bank.mappers.CardMapper;
import com.example.mini_bank.mappers.TransactionMapper;
import com.example.mini_bank.mappers.UserMapper;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {NotificationServiceProducer.class, KafkaTestConfig.class})
@EmbeddedKafka(partitions = 1, topics = {"bank-notifications"})
@DirtiesContext
@EnableKafka
class KafkaNotificationServiceProducerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationServiceProducer producer;

    private KafkaMessageListenerContainer<String, NotificationEventDto> container;
    private BlockingQueue<ConsumerRecord<String, NotificationEventDto>> records;

    @MockBean
    private CardMapper cardMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private TransactionMapper transactionMapper;

    @BeforeEach
    void setUp() throws InterruptedException {
        records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.mini_bank.dto");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, NotificationEventDto> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        new JsonDeserializer<>(NotificationEventDto.class)
                );

        ContainerProperties containerProps = new ContainerProperties("bank-notifications");
        containerProps.setMessageListener((MessageListener<String, NotificationEventDto>) records::add);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
        container.start();

        // Ждем инициализации контейнера
        Thread.sleep(2000);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    // --- Вспомогательный метод ---
    private ConsumerRecord<String, NotificationEventDto> pollEvent(String eventType, long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            ConsumerRecord<String, NotificationEventDto> record = records.poll(100, TimeUnit.MILLISECONDS);
            if (record != null && eventType.equals(record.value().getEventType())) {
                return record;
            }
        }
        return null;
    }

    @Test
    void testKafkaConnection() {
        assertThat(embeddedKafkaBroker.getBrokersAsString()).isNotBlank();
        System.out.println("Kafka brokers: " + embeddedKafkaBroker.getBrokersAsString());
    }

    @Test
    void sendUserRegisteredEvent_shouldProduceKafkaMessage() throws InterruptedException {
        records.clear();

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");

        producer.sendUserRegisteredEvent(user);

        ConsumerRecord<String, NotificationEventDto> record = pollEvent(EventType.USER_REGISTERED.name(), 5000);
        assertThat(record).isNotNull();

        NotificationEventDto event = record.value();
        assertThat(event.getEventType()).isEqualTo(EventType.USER_REGISTERED.name());
        assertThat(event.getToEmail()).isEqualTo("alice@example.com");

        Map<String, Object> data = event.getAdditionalData();
        Object userIdObj = data.get("userId");
        assertThat(userIdObj).isInstanceOf(Number.class);
        assertThat(((Number) userIdObj).longValue()).isEqualTo(1L);
        assertThat(data).containsEntry("username", "alice");
    }

    @Test
    void sendCardBlockRequestEvent_shouldProduceKafkaMessage() throws InterruptedException {
        records.clear();

        User user = new User();
        user.setEmail("john@example.com");

        Card card = new Card();
        card.setId(5L);
        card.setCardNumber("1111222233334444");
        card.setOwner(user);

        producer.sendCardBlockRequestEvent(card);

        ConsumerRecord<String, NotificationEventDto> record = pollEvent(EventType.CARD_BLOCK_REQUESTED.name(), 5000);
        assertThat(record).isNotNull();

        NotificationEventDto event = record.value();
        assertThat(event.getEventType()).isEqualTo(EventType.CARD_BLOCK_REQUESTED.name());
        assertThat(event.getToEmail()).isEqualTo("john@example.com");
    }

    @Test
    void sendCardStatusChangedEvent_shouldProduceKafkaMessage() throws InterruptedException {
        records.clear();

        User user = new User();
        user.setEmail("john@example.com");

        Card card = new Card();
        card.setId(6L);
        card.setCardNumber("5555666677778888");
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);

        producer.sendCardStatusChangedEvent(card);

        ConsumerRecord<String, NotificationEventDto> record = pollEvent(EventType.CARD_STATUS_CHANGED.name(), 5000);
        assertThat(record).isNotNull();

        NotificationEventDto event = record.value();
        assertThat(event.getEventType()).isEqualTo(EventType.CARD_STATUS_CHANGED.name());

        Map<String, Object> data = event.getAdditionalData();
        assertThat(data).containsEntry("status", "ACTIVE");
    }

    @Test
    void sendTransactionEvent_shouldProduceKafkaMessage() throws InterruptedException {
        records.clear();

        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAmount(BigDecimal.valueOf(500));
        tx.setType(TransactionType.DEPOSIT);

        producer.sendTransactionEvent(tx, "bob@example.com");

        ConsumerRecord<String, NotificationEventDto> record = pollEvent(EventType.TRANSACTION_CREATED.name(), 5000);
        assertThat(record).isNotNull();

        NotificationEventDto event = record.value();
        assertThat(event.getEventType()).isEqualTo(EventType.TRANSACTION_CREATED.name());
        assertThat(event.getToEmail()).isEqualTo("bob@example.com");

        Map<String, Object> data = event.getAdditionalData();
        Object transactionIdObj = data.get("transactionId");
        assertThat(transactionIdObj).isInstanceOf(Number.class);
        assertThat(((Number) transactionIdObj).longValue()).isEqualTo(10L);

        Object amountObj = data.get("amount");
        assertThat(amountObj).isInstanceOf(Number.class);
        assertThat(((Number) amountObj).doubleValue()).isEqualTo(500.0);
    }

    @Test
    void sendUserStatusChangedEvent_shouldProduceKafkaMessage() throws InterruptedException {
        records.clear();

        User user = new User();
        user.setId(2L);
        user.setEmail("mary@example.com");
        user.setStatus(UserStatus.ACTIVE);

        producer.sendUserStatusChangedEvent(user);

        ConsumerRecord<String, NotificationEventDto> record = pollEvent(EventType.USER_STATUS_CHANGED.name(), 5000);
        assertThat(record).isNotNull();

        NotificationEventDto event = record.value();

        Map<String, Object> data = event.getAdditionalData();
        Object userIdObj = data.get("userId");
        assertThat(userIdObj).isInstanceOf(Number.class);
        assertThat(((Number) userIdObj).longValue()).isEqualTo(2L);

        assertThat(data).containsEntry("status", "ACTIVE");
    }
}