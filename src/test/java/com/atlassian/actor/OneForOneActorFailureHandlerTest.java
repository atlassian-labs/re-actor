package com.atlassian.actor;

import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OneForOneActorFailureHandlerTest extends BaseActorFailureHandlerTest {

    private SupervisorStrategy parentOneForOneSupervisorStrategy = new OneForOneSupervisorStrategy(3, e -> parentSupervisorStrategyDirective);
    private SupervisorStrategy parentParentOneForOneSupervisorStrategy = new OneForOneSupervisorStrategy(3, e -> parentParentSupervisorStrategyDirective);

    @Override
    void setupSupervisorStrategies() {
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESUME;
        parentParentSupervisorStrategyDirective = SupervisorStrategyDirective.RESUME;

        parentSupervisorStrategy  = parentOneForOneSupervisorStrategy;
        parentParentSupervisorStrategy = parentParentOneForOneSupervisorStrategy;
    }

    @Test
    void testOneForOneSupervisorStrategyWithMaxRetries() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;
        ErrorData e = new ErrorData(new RuntimeException(), "errorMessage", actorRef);
        int i;
        for (i = 1; i <= 3; i++) {
            actorFailureHandler.handle(e);
            verify(internalActor, times(i)).pause();
            verify(internalActor, times(i)).restart(e);
        }

        actorFailureHandler.handle(e);
        verify(internalActor, times(i)).pause();
        verify(internalActor, times(1)).terminate();
    }

    @Test
    void testOneForOneSupervisorStrategyWithInfiniteRetries() {
        parentOneForOneSupervisorStrategy = new OneForOneSupervisorStrategy(e -> parentSupervisorStrategyDirective);
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;
        ErrorData e = new ErrorData(new RuntimeException(), "errorMessage", actorRef);

        int i;
        for (i = 1; i <= 14; i++) {
            actorFailureHandler.handle(e);
            verify(internalActor, times(i)).pause();
            verify(internalActor, times(i)).restart(e);
        }
    }
}