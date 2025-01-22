package com.atlassian.actor.exceptions;

public class ActorSystemTerminatingException extends RuntimeException {
    public ActorSystemTerminatingException(String msg) {
        super(msg);
    }
}