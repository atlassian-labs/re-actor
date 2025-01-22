package com.atlassian.actor.exceptions;

public class ActorNameExistsException extends RuntimeException {
    public ActorNameExistsException(String msg) {
        super(msg);
    }
}