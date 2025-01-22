package com.atlassian.actor;

import com.atlassian.actor.supervision.ActorFailureHandler;
import com.atlassian.actor.supervision.ActorFailureHandlerFactory;
import com.atlassian.actor.supervision.OneForOneActorFailureHandler;
import com.atlassian.actor.supervision.OneForOneBackoffActorFailureHandler;
import com.atlassian.actor.supervision.strategy.OneForOneBackoffSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActorFailureHandlerFactoryTest {
    @Mock
    private OneForOneSupervisorStrategy oneForOneSupervisorStrategy;
    @Mock
    private OneForOneBackoffSupervisorStrategy oneForOneBackoffSupervisorStrategy;
    @Mock
    private ActorCore actorCore;

    @Test
    void createOneForOneSupervisorStrategyActorFailureHandler() {
        when(actorCore.getParentSupervisorStrategy()).thenReturn(oneForOneSupervisorStrategy);
        ActorFailureHandler oneForOneActorFailureHandler = ActorFailureHandlerFactory.create(actorCore);
        assertInstanceOf(OneForOneActorFailureHandler.class, oneForOneActorFailureHandler);
    }

    @Test
    void createOneForOneBackoffSupervisorStrategyActorFailureHandler() {
        when(actorCore.getParentSupervisorStrategy()).thenReturn(oneForOneBackoffSupervisorStrategy);
        ActorFailureHandler oneForOneBackoffActorFailureHandler = ActorFailureHandlerFactory.create(actorCore);
        assertInstanceOf(OneForOneBackoffActorFailureHandler.class, oneForOneBackoffActorFailureHandler);
    }
}