package com.atlassian.actor.model;

public enum ActorStatus {
    CREATED, PAUSED, STARTING, RUNNING, TERMINATING, TERMINATED;

    public boolean isPaused() {
        return this == ActorStatus.PAUSED;
    }

    public boolean canPause() {
        return this == ActorStatus.RUNNING || this == ActorStatus.STARTING;
    }

    public boolean canTerminate() {
        return this == ActorStatus.RUNNING || this == ActorStatus.STARTING || this == ActorStatus.PAUSED;
    }

    public boolean isTerminating() {
        return this == ActorStatus.TERMINATING;
    }

    public boolean isTerminated() {
        return this == ActorStatus.TERMINATED;
    }

    public boolean isTerminatingOrTerminated() {
        return isTerminating() || isTerminated();
    }

    public boolean isRunningOrStarting() {
        return this == ActorStatus.RUNNING || this == ActorStatus.STARTING;
    }
}
