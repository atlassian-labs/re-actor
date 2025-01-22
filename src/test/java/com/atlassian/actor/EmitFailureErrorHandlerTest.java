package com.atlassian.actor;

import com.atlassian.actor.exceptions.QueueOverflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmitFailureErrorHandlerTest {
    private EmitFailureErrorHandler emitFailureErrorHandler;

    @BeforeEach
    void setUp() {
        emitFailureErrorHandler = new EmitFailureErrorHandler(Duration.ofMillis(50L));
    }

    @Test
    void onEmitFailureWithFailNonSerializedWhenDeadlineNotReached() {
        assertEquals(true, emitFailureErrorHandler.onEmitFailure(SignalType.ON_NEXT, Sinks.EmitResult.FAIL_NON_SERIALIZED));
    }

    @Test
    void onEmitFailureWhenFailNonSerializedWhenDeadlineReached() throws InterruptedException {
        Thread.sleep(80L);
        assertEquals(false, emitFailureErrorHandler.onEmitFailure(SignalType.ON_NEXT, Sinks.EmitResult.FAIL_NON_SERIALIZED));
    }

    @Test
    void onEmitFailureWithFailOverflowWhenDeadlineReached() {
        assertThrows(QueueOverflowException.class, () -> emitFailureErrorHandler.onEmitFailure(SignalType.ON_NEXT, Sinks.EmitResult.FAIL_OVERFLOW));
    }

    @Test
    void onEmitFailureWithOtherError() {
        assertEquals(false, emitFailureErrorHandler.onEmitFailure(SignalType.ON_NEXT, Sinks.EmitResult.FAIL_TERMINATED));
    }
}