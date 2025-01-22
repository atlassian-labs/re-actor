package com.atlassian.actor.config;

import com.atlassian.actor.ActorCore;
import com.atlassian.actor.model.Pair;
import reactor.core.observability.SignalListenerFactory;
import reactor.core.scheduler.Scheduler;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Configuration to be used internally for creating an actor. It combines the configuration from ActorSystemConfig and ActorConfig.
 */
public class ActorCreationConfig {
    private final String name;
    private final ActorCore parentActor;
    private final Scheduler scheduler;
    private final Integer queueSize;
    private SignalListenerFactory<Object, ?> signalListenerFactory;
    private final List<Pair<String, String>> tags;

    public ActorCreationConfig(String name, ActorCore parentActor,
                               Scheduler scheduler, Integer queueSize) {
        this.name = name;
        this.parentActor = parentActor;
        this.scheduler = scheduler;
        this.queueSize = queueSize;
        this.tags = emptyList();
    }

    public ActorCreationConfig(String name, ActorCore parentActor,
                               Scheduler scheduler, Integer queueSize,
                               SignalListenerFactory<Object, ?> signalListenerFactory,
                               List<Pair<String, String>> tags) {
        this.name = name;
        this.parentActor = parentActor;
        this.scheduler = scheduler;
        this.queueSize = queueSize;
        this.signalListenerFactory = signalListenerFactory;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public ActorCore getParentActor() {
        return parentActor;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public SignalListenerFactory<Object, ?> getSignalListenerFactory() {
        return signalListenerFactory;
    }

    public List<Pair<String, String>> getTags() {
        return tags;
    }
}