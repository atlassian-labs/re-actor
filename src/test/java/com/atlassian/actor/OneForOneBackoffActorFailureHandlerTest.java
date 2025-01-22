package com.atlassian.actor;

import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.restart.config.ExponentialBackoffRestartConfig;
import com.atlassian.actor.supervision.restart.config.FixedBackoffRestartConfig;
import com.atlassian.actor.supervision.strategy.OneForOneBackoffSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OneForOneBackoffActorFailureHandlerTest extends BaseActorFailureHandlerTest {

    private final ExponentialBackoffRestartConfig exponentialBackoffRestartConfig = new ExponentialBackoffRestartConfig(
            3, Duration.ofMillis(1), Duration.ofMillis(2), Duration.ofMillis(1000));

    private SupervisorStrategy parentOneForOneBackoffSupervisorStrategy =
            new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (lastRestartCount, e) -> parentSupervisorStrategyDirective);
    private final SupervisorStrategy parentParentOneForOneBackoffSupervisorStrategy =
            new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (lastRestartCount, e) -> parentParentSupervisorStrategyDirective);

    @Override
    void setupSupervisorStrategies() {
        waitIntervalForRestart = Duration.ofMillis(300);
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;
        parentParentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;

        parentSupervisorStrategy  = parentOneForOneBackoffSupervisorStrategy;
        parentParentSupervisorStrategy = parentParentOneForOneBackoffSupervisorStrategy;
    }

    @Test
    void testOneForOneSupervisorStrategyWithExponentialBackoff() throws InterruptedException {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;
        ErrorData e = new ErrorData(new RuntimeException(), "errorMessage", actorRef);

        int i;
        for (i = 1; i <= 3; i++) {
            e = new ErrorData(new Throwable("message for error: " + i), "errorMessage", actorRef);
            actorFailureHandler.handle(e);
            Thread.sleep(10);
            verify(internalActor, times(i)).pause();
            // number of times would be one because the message is unique
            verify(internalActor, times(1)).restart(e);
        }

        actorFailureHandler.handle(e);
        verify(internalActor, times(i)).pause();
        verify(internalActor, times(1)).terminate();
    }

    @Test
    void testOneForOneSupervisorStrategyWithFixedBackoff() throws InterruptedException {
        FixedBackoffRestartConfig fixedBackoffRestartConfig = new FixedBackoffRestartConfig(
                3, Duration.ofMillis(1), Duration.ofMillis(2), Duration.ofMillis(1), Duration.ofMillis(1000));
        parentOneForOneBackoffSupervisorStrategy = new OneForOneBackoffSupervisorStrategy(fixedBackoffRestartConfig, (lastRestartCount, e) -> parentSupervisorStrategyDirective);
        setUp();
        ErrorData e = new ErrorData(new RuntimeException(), "errorMessage", actorRef);

        int i;
        for (i = 1; i <= 3; i++) {
            e = new ErrorData(new Throwable("message for error: " + i), "errorMessage", actorRef);
            actorFailureHandler.handle(e);
            Thread.sleep(4);
            verify(internalActor, times(i)).pause();
            // number of times would be one because the message is unique
            verify(internalActor, times(1)).restart(e);
        }

        actorFailureHandler.handle(e);
        verify(internalActor, times(i)).pause();
        verify(internalActor, times(1)).terminate();
    }

    @Test
    void testOneForOneSupervisorStrategyWithExponentialBackoffAndResetRestartCount() throws InterruptedException {
        ExponentialBackoffRestartConfig exponentialBackoffRestartConfig = new ExponentialBackoffRestartConfig(
                3, Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(1));

        parentOneForOneBackoffSupervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (lastRestartCount, e) -> parentSupervisorStrategyDirective);
        setUp();
        ErrorData e;

        int i;
        for (i = 1; i <= 4; i++) {
            e = new ErrorData(new Throwable("message for error: " + i), "errorMessage", actorRef);
            actorFailureHandler.handle(e);
            Thread.sleep(50);
            verify(internalActor, times(i)).pause();
            // number of times would be one because the message is unique
            verify(internalActor, times(1)).restart(e);
        }
    }
}