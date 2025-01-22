package com.atlassian.actor.model;

import com.atlassian.actor.ActorConsumer;

import java.util.function.Predicate;

public class MatchTuple {
    private final Predicate<Object> predicate;
    private final ActorConsumer<Object> action;

    public MatchTuple(Predicate<Object> predicate, ActorConsumer<Object> action) {
        this.predicate = predicate;
        this.action = action;
    }

    public Predicate<Object> getPredicate() {
        return predicate;
    }

    public ActorConsumer<Object> getAction() {
        return action;
    }
}
