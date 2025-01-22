package com.atlassian.actor.exceptions;

public class ActorPostStopException extends RuntimeException {
    public ActorPostStopException(String msg) {
        super(msg);
    }

    public ActorPostStopException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public ActorPostStopException(Throwable throwable) {
        super(throwable);
    }
}