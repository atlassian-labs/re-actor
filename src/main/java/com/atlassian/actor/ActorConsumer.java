package com.atlassian.actor;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result.
 *
 * <p>This is a functional interface
 * whose functional method is {@link #accept(Object, Responder)}.
 *
 * @param <T> the type of the input to the operation
 *
 */
@FunctionalInterface
public interface ActorConsumer<T> {

    /**
     * Performs this operation on the given argument.
     * @param t the input argument
     * @param responder the responder to send the response
     * @throws Exception if an error occurs during the operation
     */
    @SuppressWarnings("java:S112")
    void accept(T t, Responder responder) throws Exception;
}
