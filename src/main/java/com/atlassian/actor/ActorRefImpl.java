package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.exceptions.ActorTerminatingException;

import java.time.Duration;
import java.util.function.Supplier;

public class ActorRefImpl implements ActorRef {

    private final InternalActor actor;

    private ActorRefImpl(InternalActor actor) {
        this.actor = actor;
    }

    public static ActorRef create(InternalActor actor) {
        return new ActorRefImpl(actor);
    }

    @Override
    public String getName() {
        return actor.getName();
    }

    /**
     * Publish data to Reactor Sink which creates Flux publisher. This method is thread-safe.
     *
     * @param message Actor message to be published
     */
    @Override
    public void tell(Object message) {
        actor.publish(message);
    }

    /**
     * Publish data to Reactor Sink with some delay. This method is thread-safe.
     *
     * @param message Actor message to be published
     * @param delay   Duration after which this message will be published
     */
    @Override
    public void tell(Object message, Duration delay) {
        actor.publish(message, delay);
    }

    @Override
    public boolean isTerminating() {
        return actor.isTerminating();
    }

    @Override
    public boolean isTerminated() {
        return actor.isTerminated();
    }

    @Override
    public boolean isRunning() {
        return actor.isRunning();
    }

    @Override
    public void watch(ActorRef actorRef) {
        actorRef.addWatcher(this);
    }

    @Override
    public void unWatch(ActorRef actorRef) {
        actorRef.removeWatcher(this);
    }

    @Override
    public void addWatcher(ActorRef actorRef) {
        actor.addWatcher(actorRef);
    }

    @Override
    public void removeWatcher(ActorRef actorRef) {
        actor.removeWatcher(actorRef);
    }

    @Override
    public ActorRef actorOf(Supplier<AbstractActor> actorSupplier, ActorConfig actorConfig) {
        if (actor.isTerminating()) {
            throw new ActorTerminatingException("Actor " + getName() + " is terminating. Can't create child actor");
        }
        return actor.createActor(actorSupplier, actorConfig);
    }

    @Override
    public ActorRef getChildByName(String name) {
        return actor.getChildByName(name);
    }

    @Override
    public Object ask(Object message, long timeoutMs) throws Exception {
        Responder responder = new Responder();
        actor.publish(message, responder);
        return responder.waitForObject(timeoutMs);
    }
}
