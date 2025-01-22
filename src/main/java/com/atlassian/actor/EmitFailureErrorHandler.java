package com.atlassian.actor;

import com.atlassian.actor.exceptions.QueueOverflowException;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.time.Duration;

/**
 * This class is used to handle the failure of emitting a signal to a sink. It is used to handle the case when there
 * is some error in publishing the message to the sink.
 */
public class EmitFailureErrorHandler implements Sinks.EmitFailureHandler {
    private final long deadline;

    public EmitFailureErrorHandler(Duration duration) {
        deadline = System.nanoTime() + duration.toNanos();
    }

    @Override
    public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
        if (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            return System.nanoTime() < deadline;
        } else if (emitResult == Sinks.EmitResult.FAIL_OVERFLOW) {
            throw new QueueOverflowException("Sink queue size is full, looks like subscriber is slow");
        } else {
            return false;
        }
    }
}