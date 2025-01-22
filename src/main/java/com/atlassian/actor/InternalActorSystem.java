package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorCreationConfig;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.supervision.ActorFailureHandler;
import com.atlassian.actor.supervision.ActorFailureHandlerFactory;
import com.atlassian.actor.model.ActorSystemStatus;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import com.google.common.annotations.VisibleForTesting;
import reactor.core.observability.SignalListenerFactory;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * InternalActorSystem is internally used by ActorSystem to create, restart, terminate and pause child actors in actorSystem.
 */
public class InternalActorSystem extends ActorCore {

    private final Scheduler scheduler;
    private final SignalListenerFactory<Object, ?> signalListenerFactory;
    private final SupervisorStrategy supervisorStrategy;
    private final ActorFailureHandler actorFailureHandler;
    @VisibleForTesting
    protected final AtomicReference<ActorSystemStatus> status = new AtomicReference<>(ActorSystemStatus.CREATED);
    private Runnable terminateRunnable = () -> {
    };

    public void setTerminateRunnable(Runnable terminateRunnable) {
        this.terminateRunnable = terminateRunnable;
    }

    private InternalActorSystem(String name, Scheduler scheduler, SignalListenerFactory<Object, ?> signalListenerFactory, SupervisorStrategy supervisorStrategy) {
        super(name);
        this.scheduler = scheduler;
        this.signalListenerFactory = signalListenerFactory;
        this.supervisorStrategy = supervisorStrategy;
        this.actorFailureHandler = ActorFailureHandlerFactory.create(this);
    }

    public static InternalActorSystem create(String name, Scheduler scheduler, SignalListenerFactory<Object, ?> signalListenerFactory, SupervisorStrategy supervisorStrategy) {
        return new InternalActorSystem(name, scheduler, signalListenerFactory, supervisorStrategy);
    }

    public ActorRef createActor(Supplier<AbstractActor> reactorSupplier, ActorConfig actorConfig) {
        return createActor(
                reactorSupplier,
                new ActorCreationConfig(
                        actorConfig.getName(),
                        this,
                        scheduler,
                        actorConfig.getQueueSize(),
                        signalListenerFactory,
                        actorConfig.getTags()
                )
        );
    }

    @Override
    public ActorFailureHandler getActorFailureHandler() {
        return actorFailureHandler;
    }

    @Override
    public ActorCore getParentActorCore() {
        return null;
    }

    @Override
    public void restart(ErrorData errorData) {
        restartAllChildren(errorData);
    }

    @Override
    public synchronized void terminate() {
        if (status.get().canTerminate()) {
            status.set(ActorSystemStatus.TERMINATING);
            logger.info("Actor system {} is now terminating", getName());
            terminateAllChildren();
        } else {
            logger.warn("Can't terminate actorSystem {} because current status is {}", getName(), status.get());
        }
    }

    /**
     * Pauses all children of the actorSystem.
     */
    @Override
    public void pause() {
        pauseAllChildren();
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public SupervisorStrategy getSupervisorStrategy() {
        return supervisorStrategy;
    }

    @Override
    protected void childrenTerminateCallback() {
        terminateRunnable.run();
        status.set(ActorSystemStatus.TERMINATED);
    }

    public ActorSystemStatus getStatus() {
        return status.get();
    }
}