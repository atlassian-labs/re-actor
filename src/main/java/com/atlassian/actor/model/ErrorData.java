package com.atlassian.actor.model;

import com.atlassian.actor.ActorRef;

public class ErrorData {
    private final Throwable error;
    private final Object message;
    private final ActorRef actorRef;

    public ErrorData(Throwable error, Object message, ActorRef actorRef) {
        this.error = error;
        this.message = message;
        this.actorRef = actorRef;
    }

    // getters and setters

    public Throwable getError() {
        return error;
    }

    public Object getMessage() {
        return message;
    }

    public ActorRef getActorRef() {
        return actorRef;
    }
}