package com.hivemq.mqtt.message.pool.exception;

public class MessageIdUnavailableException extends Exception {

    public MessageIdUnavailableException(final int unavailableMessageId) {
        super(String.format("Desired message id %d is unavailable", unavailableMessageId));
    }
}
