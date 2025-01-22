package com.atlassian.actor.config;

import com.atlassian.actor.model.Pair;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Configuration for creating an actor.
 */
public class ActorConfig {
    /**
     * Name of the actor.
     */
    private final String name;
    /**
     * Tags to be added in all the metrics emitted by this actor.
     */
    private final List<Pair<String, String>> tags;
    /**
     * Max Size of the queue for this actor. Default is Integer.MAX_VALUE. If the queue is full, no further messages can be published.
     */
    private Integer queueSize;

    public ActorConfig(String name) {
        this.name = name;
        this.tags = emptyList();
        this.queueSize = Integer.MAX_VALUE;
    }

    public ActorConfig(String name, List<Pair<String, String>> tags) {
        this.name = name;
        this.tags = tags;
        this.queueSize = Integer.MAX_VALUE;
    }

    public ActorConfig(String name, List<Pair<String, String>> tags, Integer queueSize) {
        this.name = name;
        this.tags = tags;
        this.queueSize = queueSize;
    }

    public String getName() {
        return name;
    }

    public List<Pair<String, String>> getTags() {
        return tags;
    }

    public Integer getQueueSize() {
        return queueSize;
    }
}