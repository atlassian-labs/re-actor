package com.atlassian.actor.model;

import com.atlassian.actor.ActorRef;

/**
 * Terminated message is sent to the supervisor &amp; watcher when an actor is terminated.
 * It is sent to the reactor as a message containing the reactor reference.
 */
public class Terminated {
    private final ActorRef actorRef;

    public Terminated(ActorRef actorRef) {
        this.actorRef = actorRef;
    }

    // getters and setters

    public ActorRef getActorRef() {
        return actorRef;
    }
}