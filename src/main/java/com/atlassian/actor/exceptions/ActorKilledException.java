package com.atlassian.actor.exceptions;

public class ActorKilledException extends RuntimeException {
    public ActorKilledException(String msg) {
        super(msg);
    }
}