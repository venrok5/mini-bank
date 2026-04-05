package com.example.mini_bank.config;

import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import com.example.mini_bank.dto.NotificationEventDto;

@TestConfiguration
public class KafkaTestConfig {
	@Bean
	public KafkaTemplate<String, NotificationEventDto> kafkaTemplate(EmbeddedKafkaBroker embeddedKafkaBroker) {
	    Map<String, Object> props = Map.of(
	        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
	        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
	        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
	    );
	    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
	}
}
