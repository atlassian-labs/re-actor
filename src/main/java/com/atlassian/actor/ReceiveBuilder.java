package com.atlassian.actor;

import com.atlassian.actor.model.MatchTuple;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for {@link Receive} instances. Use {@link #create()} to get a new builder.
 */
public class ReceiveBuilder {

    @VisibleForTesting
    protected final List<MatchTuple> matches = new ArrayList<>();

    private ReceiveBuilder() {
    }

    /**
     * Creates a new {@link ReceiveBuilder} instance.
     * @return {@link ReceiveBuilder}
     */
    public static ReceiveBuilder create() {
        return new ReceiveBuilder();
    }

    /**
     * Adds a match clause to the builder. The match clause will match any message of the given type and apply the given consumer function.
     * @param <P> Type of message to match.
     * @param type {@link Class} - Type of message to match.
     * @param apply {@link ActorConsumer} - Consumer function to be applied on the message.
     * @return {@link ReceiveBuilder}
     */
    public <P> ReceiveBuilder match(final Class<P> type, final ActorConsumer<P> apply) {
        MatchTuple matchTuple = new MatchTuple(
                type::isInstance, (ActorConsumer<Object>) apply
        );
        matches.add(matchTuple);
        return this;
    }

    /**
     * Adds a matchAny clause to the builder. This would match any message and apply the given consumer function.
     * @param apply {@link Consumer} - Consumer function to be applied on the message.
     * @return {@link ReceiveBuilder}
     */
    public ReceiveBuilder matchAny(final ActorConsumer<Object> apply) {
        return match(Object.class, apply);
    }

    /**
     * Builds a {@link Receive} instance from the current state of the builder.
     * @return {@link Receive}
     */
    public Receive build() {
        return new Receive(matches);
    }
}


