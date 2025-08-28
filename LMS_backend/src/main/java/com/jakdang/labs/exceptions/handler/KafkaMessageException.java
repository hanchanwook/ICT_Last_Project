package com.jakdang.labs.exceptions.handler;

import org.apache.kafka.common.KafkaException;

public class KafkaMessageException extends KafkaException {
    public KafkaMessageException(String message) {
        super(message);
    }
}
