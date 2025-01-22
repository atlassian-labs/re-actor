package com.atlassian.actor.config;

import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.scheduler.Scheduler;

/**
 * Configuration for creating an actor system.
 */
public class ActorSystemConfig {
    /**
     * Scheduler to be used by the actors created in system. All the child actors will also use the same scheduler.
     */
    private final Scheduler scheduler;
    /**
     * MeterRegistry to be used by the actors created in system. All the child actors will also use the same MeterRegistry to emit metrics.
     */
    private MeterRegistry meterRegistry;
    /**
     * SupervisorStrategy to be at the actorSystem level. If not provided, DefaultSupervisorStrategy of RESTART with 3 retries will be used.
     */
    private final SupervisorStrategy supervisorStrategy;

    public ActorSystemConfig(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.supervisorStrategy = new OneForOneSupervisorStrategy(3, e -> SupervisorStrategyDirective.RESTART);
    }

    public ActorSystemConfig(Scheduler scheduler, SupervisorStrategy supervisorStrategy) {
        this.scheduler = scheduler;
        this.supervisorStrategy = supervisorStrategy;
    }

    public ActorSystemConfig(Scheduler scheduler, MeterRegistry meterRegistry) {
        this.scheduler = scheduler;
        this.meterRegistry = meterRegistry;
        this.supervisorStrategy = new OneForOneSupervisorStrategy(3, e -> SupervisorStrategyDirective.RESTART);
    }

    public ActorSystemConfig(Scheduler scheduler, MeterRegistry meterRegistry,
                             SupervisorStrategy supervisorStrategy) {
        this.scheduler = scheduler;
        this.meterRegistry = meterRegistry;
        this.supervisorStrategy = supervisorStrategy;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public SupervisorStrategy getSupervisorStrategy() {
        return supervisorStrategy;
    }
}