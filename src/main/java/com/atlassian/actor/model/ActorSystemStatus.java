package com.atlassian.actor.model;

public enum ActorSystemStatus {
    CREATED, TERMINATING, TERMINATED;

    public boolean canTerminate() {
        return this == ActorSystemStatus.CREATED;
    }

    public boolean isTerminating() {
        return this == ActorSystemStatus.TERMINATING;
    }

    public boolean isTerminated() {
        return this == ActorSystemStatus.TERMINATED;
    }

    public boolean isTerminatingOrTerminated() {
        return isTerminating() || isTerminated();
    }
}
