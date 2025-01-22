package com.atlassian.actor.model;

import com.atlassian.actor.InternalActor;

public class ActorCell {
    private final InternalActor actor;
    private final boolean isTerminating;

    public ActorCell(InternalActor actor, boolean isTerminating) {
        this.actor = actor;
        this.isTerminating = isTerminating;
    }

    public InternalActor getActor() {
        return actor;
    }

    public boolean isTerminating() {
        return isTerminating;
    }
}