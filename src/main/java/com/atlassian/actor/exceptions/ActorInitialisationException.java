package com.atlassian.actor.exceptions;

public class ActorInitialisationException extends RuntimeException {
    public ActorInitialisationException(String msg) {
        super(msg);
    }
}