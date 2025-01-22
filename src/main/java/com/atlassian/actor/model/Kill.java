package com.atlassian.actor.model;

/**
 * A singleton class that is used to terminate the reactor. It is sent to the reactor as a message.
 */
public class Kill {

    private Kill() {
    }

    private static final Kill instance = new Kill();

    public static Kill getInstance() {
        return instance;
    }
}
