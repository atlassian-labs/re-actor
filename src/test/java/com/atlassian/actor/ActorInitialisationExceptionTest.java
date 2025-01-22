package com.atlassian.actor;

import com.atlassian.actor.config.ActorCreationConfig;
import com.atlassian.actor.exceptions.InvalidMessageException;
import com.atlassian.actor.supervision.ActorFailureHandlerFactory;
import com.atlassian.actor.supervision.OneForOneBackoffActorFailureHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActorInitialisationExceptionTest {
    private Scheduler scheduler = Schedulers.immediate();
    @Mock
    private Supplier<AbstractActor> actorSupplier;
    @Mock
    private AbstractActor abstractActor;
    @Mock
    private Receive receiver;
    @Mock
    private ActorCreationConfig actorCreationConfig;
    @Mock
    private ActorRef actorRef;
    @Mock
    private ActorRef childActorRef;
    @Mock
    private ActorCore parentActorCoreSystem;
    @Mock
    private InternalActor childInternalActor;
    @Mock
    private OneForOneBackoffActorFailureHandler oneForOneBackoffActorFailureHandler;
    private InternalActor internalActor;
    private MockedStatic<ActorRefImpl> mockActorRefImpl;
    private MockedStatic<ActorFailureHandlerFactory> mockDefaultActorFailureHandler;
    private static final String ACTOR_NAME = "test";
    private static final String CHILD_ACTOR_NAME = "childActor";

    @BeforeEach
    void setUp() {
        when(actorCreationConfig.getName()).thenReturn(ACTOR_NAME);
        when(actorCreationConfig.getQueueSize()).thenReturn(Integer.MAX_VALUE);
        when(actorCreationConfig.getScheduler()).thenReturn(scheduler);
        when(actorCreationConfig.getParentActor()).thenReturn(parentActorCoreSystem);
        doNothing().when(oneForOneBackoffActorFailureHandler).handle(any());
        when(childInternalActor.getName()).thenReturn(CHILD_ACTOR_NAME);
        when(childInternalActor.getActorRef()).thenReturn(childActorRef);

        mockActorRefImpl = mockStatic(ActorRefImpl.class);
        mockActorRefImpl
                .when(() -> ActorRefImpl.create(any()))
                .thenReturn(actorRef);
        mockDefaultActorFailureHandler = mockStatic(ActorFailureHandlerFactory.class);
        mockDefaultActorFailureHandler
                .when(() -> ActorFailureHandlerFactory.create(any()))
                .thenReturn(oneForOneBackoffActorFailureHandler);
    }

    void mockCreateNewActorInstance() {
        when(actorSupplier.get()).thenThrow(new RuntimeException());
        doNothing().when(abstractActor).setSelf(actorRef);
        when(abstractActor.createReceive()).thenReturn(receiver);
    }

    @AfterEach
    void cleanup() {
        mockActorRefImpl.close();
        mockDefaultActorFailureHandler.close();
    }

    @Test
    void checkIfActorThrowsExceptionIfInitFails() throws Exception {
        mockCreateNewActorInstance();
        doNothing().when(abstractActor).preStart();
        internalActor = InternalActor.create(actorSupplier, actorCreationConfig);

        verify(abstractActor, never()).preStart();
        verify(abstractActor, never()).createReceive();
        verify(abstractActor, never()).postStop();
        verify(oneForOneBackoffActorFailureHandler).handle(any());

        assertThrows(RuntimeException.class, () -> actorSupplier.get());
        assertThrows(InvalidMessageException.class, () -> internalActor.publish(null));
    }

}