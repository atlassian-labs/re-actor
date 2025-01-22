package com.atlassian.actor;

import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;

/**
 * AbstractActor - Abstract class to be implemented to create Actor Instance.
 * You won't have access to ActorRef via self() in constructor of this class. It would only be accessed when preStart() is called.
 */
public abstract class AbstractActor {
    private ActorRef self = null;
    private static final SupervisorStrategy defaultSupervisorStrategy = new OneForOneSupervisorStrategy(3, e -> SupervisorStrategyDirective.RESTART);

    /**
     * preStart - Called after Actor is ready to process messages.
     * This method is called on the thread in which messages are processed.
     * @throws Exception if an error occurs during the restart process.
     */
    @SuppressWarnings("java:S112")
    public abstract void preStart() throws Exception;

    /**
     * receiveBuilder - Create a ReceiveBuilder to define message handlers.
     * @return {@link ReceiveBuilder}
     */
    public final ReceiveBuilder receiveBuilder() {
        return ReceiveBuilder.create();
    }

    /**
     * createReceive - Create a {@link Receive} to define message handlers.
     * @return {@link Receive}
     */
    public abstract Receive createReceive();

    /**
     * postStop - Called when Actor is terminating &amp; no further messages can be published/processed.
     * @throws Exception if an error occurs in the postStop process.
     */
    @SuppressWarnings("java:S112")
    public abstract void postStop() throws Exception;

    /**
     * supervisorStrategy - SupervisorStrategy to be used by this Actor for handling failures in child actors.
     * @return {@link SupervisorStrategy}
     */
    public SupervisorStrategy supervisorStrategy() {
        return defaultSupervisorStrategy;
    }

    /**
     * preRestart - Called before restarting the Actor.
     * @param reason {@link Throwable} - Reason for restarting the Actor.
     * @param message {@link Object} - Message which caused the Actor to restart.
     * @throws Exception if an error occurs during the reRestart process.
     */

    public void preRestart(Throwable reason, Object message) throws Exception {
        postStop();
    }

    /**
     * postRestart - Called after restarting the Actor.
     * @param reason {@link Throwable} - Reason for restarting the Actor.
     * @throws Exception if an error occurs during the restart process.
     */

    public void postRestart(Throwable reason) throws Exception {
        preStart();
    }

    public void setSelf(ActorRef actorRef) {
        self = actorRef;
    }

    public ActorRef self() {
        return self;
    }
}