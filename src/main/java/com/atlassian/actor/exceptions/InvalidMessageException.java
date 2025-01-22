package com.atlassian.actor.exceptions;

public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String msg) {
        super(msg);
    }
}