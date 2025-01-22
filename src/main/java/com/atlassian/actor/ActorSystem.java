package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorSystemConfig;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.exceptions.ActorSystemTerminatingException;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import reactor.core.observability.SignalListenerFactory;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * ActorSystem is the top level actor in the hierarchy. It is responsible for creating and supervising all the
 * actors in the hierarchy. It is also responsible for terminating all the actors in the hierarchy.
 */
public class ActorSystem {

    private final String name;

    private final InternalActorSystem internalActorSystem;

    private final SupervisorStrategy supervisorStrategy;

    private ActorSystem(String name, ActorSystemConfig actorSystemConfig, InternalActorSystem internalActorSystem) {
        this.name = name;
        this.internalActorSystem = internalActorSystem;
        this.supervisorStrategy = actorSystemConfig.getSupervisorStrategy();
    }

    public static ActorSystem create(String name, ActorSystemConfig actorSystemConfig) {
        SignalListenerFactory<Object, ?> signalListenerFactory =
                actorSystemConfig.getMeterRegistry() != null
                        ? Micrometer.metrics(actorSystemConfig.getMeterRegistry())
                        : null;
        return new ActorSystem(
                name,
                actorSystemConfig,
                InternalActorSystem.create(
                        name,
                        actorSystemConfig.getScheduler(),
                        signalListenerFactory,
                        actorSystemConfig.getSupervisorStrategy()
                )
        );
    }

    public static ActorSystem testSystem(String name) {
        return create(name, new ActorSystemConfig(Schedulers.immediate(), new OneForOneSupervisorStrategy(0, e -> SupervisorStrategyDirective.RESTART)));
    }

    public static ActorSystem testSystem() {
        return testSystem("TestSystem");
    }

    public String getName() {
        return name;
    }

    public ActorRef actorOf(Supplier<AbstractActor> actorSupplier, ActorConfig actorConfig) {
        return internalActorSystem.createActor(actorSupplier, actorConfig);
    }

    public synchronized void terminate(Runnable runnable) {
        if (internalActorSystem.getStatus().isTerminatingOrTerminated()) {
            throw new ActorSystemTerminatingException("ActorSystem " + internalActorSystem.getName() + " is already terminating or Terminated");
        }
        if (runnable != null) {
            internalActorSystem.setTerminateRunnable(runnable);
        }
        internalActorSystem.terminate();
    }

    public void terminate() {
        terminate(null);
    }

    public void registerOnTermination(Runnable runnable) {
        internalActorSystem.setTerminateRunnable(runnable);
    }

    public SupervisorStrategy getSupervisionStrategy() {
        return supervisorStrategy;
    }

    public boolean isTerminatingOrTerminated() {
        return internalActorSystem.getStatus().isTerminatingOrTerminated();
    }

    public boolean isTerminated() {
        return internalActorSystem.getStatus().isTerminated();
    }
}