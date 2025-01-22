package com.atlassian.actor.exceptions;

public class ActorTerminatedException extends RuntimeException {
    public ActorTerminatedException(String msg) {
        super(msg);
    }
}