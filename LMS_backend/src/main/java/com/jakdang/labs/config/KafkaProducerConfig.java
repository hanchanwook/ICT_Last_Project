//package com.jakdang.labs.kafka.config;
//
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Kafka 프로듀서 설정 클래스
// * 배치 처리 및 성능 최적화를 위한 설정을 포함합니다.
// */
//@Configuration
//public class KafkaProducerConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Bean
//    public ProducerFactory<String, String> producerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//
//        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//
//        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
//        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 20);
//        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);
//
//        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
//
//        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
//        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
//        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);
//        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
//
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }
//
////    @Bean
////    public KafkaTemplate<String, String> kafkaTemplate() {
////        return new KafkaTemplate<>(producerFactory());
////    }
//}