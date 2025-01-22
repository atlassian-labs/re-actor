package com.atlassian.actor.exceptions;

public class QueueOverflowException extends RuntimeException {
    public QueueOverflowException(String msg) {
        super(msg);
    }
}