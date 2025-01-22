package com.atlassian.actor.model;

/**
 * A singleton class that is used to terminate the reactor. It is sent to the reactor as a message.
 */
public class PoisonPill {

    private PoisonPill() {
    }

    private static final PoisonPill instance = new PoisonPill();

    public static PoisonPill getInstance() {
        return instance;
    }
}
