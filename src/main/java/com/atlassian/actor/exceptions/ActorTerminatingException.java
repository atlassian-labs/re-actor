package com.atlassian.actor.exceptions;

public class ActorTerminatingException extends RuntimeException {
    public ActorTerminatingException(String msg) {
        super(msg);
    }
}