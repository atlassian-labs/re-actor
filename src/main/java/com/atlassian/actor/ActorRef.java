package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Reference to a reactor. This is used to publish data to the reactor &amp; creating child reactors.
 */
public interface ActorRef {

    /**
     * Get the name of the reactor.
     *
     * @return name of the reactor
     */
    String getName();

    /**
     * Publish data to Actor. This method is thread-safe.
     *
     * @param message Actor message to be published
     */
    void tell(Object message);

    /**
     * Publish data to Actor with some delay. This method is thread-safe.
     *
     * @param message Actor message to be published
     * @param delay   Duration after which this message will be published
     */
    void tell(Object message, Duration delay);

    /**
     * Publish data to Actor and wait for it to return
     * a response.
     *
     * @param message   Actor message to be published
     * @param timeoutMs the number of milliseconds to wait for the response
     * @return the response data
     * @throws Exception if an error occurs during the operation
     */
    Object ask(Object message, long timeoutMs) throws Exception;

    /**
     * Check if the reactor is terminating.
     * @return true if the actor is terminating else false
     */
    boolean isTerminating();

    /**
     * Check if the reactor is terminated.
     *
     * @return true if the reactor is terminated else false
     */
    boolean isTerminated();

    /**
     * Check if the reactor is running.
     *
     * @return true if the reactor is running else false
     */
    boolean isRunning();

    /**
     * Enable notification to current reactor when reactor(passed in argument) terminates.
     *
     * @param actorRef {@link ActorRef} reactor which is being watched
     */
    void watch(ActorRef actorRef);

    /**
     * Disable notification to current reactor when reactor(passed in argument) terminates.
     *
     * @param actorRef {@link ActorRef} reactor which is to be removed from watch list
     */
    void unWatch(ActorRef actorRef);

    /**
     * Add watcher to current reactor.
     *
     * @param actorRef {@link ActorRef} reactor which will be notified when current reactor terminates
     */
    void addWatcher(ActorRef actorRef);

    /**
     * Remove watcher from current reactor.
     *
     * @param actorRef {@link ActorRef} reactor which will be removed from watchers list
     */
    void removeWatcher(ActorRef actorRef);

    /**
     * Create a child actor with supplier function and reactor config.
     *
     * @param reactor     {@link AbstractActor} child reactor supplier
     * @param actorConfig {@link ActorConfig} child reactor config
     * @return {@link ActorRef} reference to child reactor
     */
    ActorRef actorOf(Supplier<AbstractActor> reactor, ActorConfig actorConfig);

    /**
     * Returns the reference to the child reactor with the given name.
     * @param name name of the child reactor
     * @return reference to the child reactor
     */
    ActorRef getChildByName(String name);
}
