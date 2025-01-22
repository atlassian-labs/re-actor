package com.atlassian.actor;

import com.atlassian.actor.model.MatchTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * A {@link Receive} instance is a function that processes messages of a certain type.
 * It is used to define the behavior of an actor.
 */
public class Receive {

    private final Logger log = LoggerFactory.getLogger(Receive.class);
    private final List<MatchTuple> matches;
    private final ActorConsumer<Object> defaultConsumer = (m, responder) -> log.info("couldn't process the message {}", m);

    public Receive(List<MatchTuple> matches) {
        this.matches = matches;
    }

    public void process(Object message, Responder responder) throws Exception {
        findConsumer(message).accept(message, responder);
    }

    private ActorConsumer<Object> findConsumer(Object message) {
        Optional<MatchTuple> match = matches.stream()
                .filter(matchTuple -> matchTuple.getPredicate().test(message))
                .findFirst();
        if (match.isPresent()) {
            return match.get().getAction();
        } else {
            return defaultConsumer;
        }
    }
}
