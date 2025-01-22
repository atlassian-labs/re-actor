package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorSystemConfig;
import com.atlassian.actor.exceptions.ActorSystemTerminatingException;
import com.atlassian.actor.model.ActorSystemStatus;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActorSystemTest {
    @Mock
    private ActorSystemConfig actorSystemConfig;
    @Mock
    private Supplier<AbstractActor> actorSupplier;
    @Mock
    private ActorConfig actorConfig;
    @Mock
    private InternalActorSystem internalActorSystem;
    @Mock
    private Runnable runnable;
    @Mock
    private SupervisorStrategy supervisorStrategy;
    @Mock
    private ActorRef actorRef;
    private ActorSystem actorSystem;
    private MockedStatic<InternalActorSystem> mockInternalActorSystem;

    @BeforeEach
    void setUp() {
        when(actorSystemConfig.getSupervisorStrategy()).thenReturn(supervisorStrategy);
        mockInternalActorSystem = mockStatic(InternalActorSystem.class);
        mockInternalActorSystem
                .when(() -> InternalActorSystem.create(any(), any(), any(), any()))
                .thenReturn(internalActorSystem);
        actorSystem = ActorSystem.create("test", actorSystemConfig);
        lenient().when(internalActorSystem.getStatus()).thenReturn(ActorSystemStatus.CREATED);
    }

    @AfterEach
    void cleanup() {
        mockInternalActorSystem.close();
    }

    @Test
    void create() {
        assertNotNull(actorSystem);
        assertEquals("test", actorSystem.getName());
    }

    @Test
    void getName() {
        assertEquals("test", actorSystem.getName());
    }

    @Test
    void createActor() {
        when(internalActorSystem.createActor(actorSupplier, actorConfig))
                .thenReturn(actorRef);
        actorSystem.actorOf(actorSupplier, actorConfig);
        verify(internalActorSystem, times(1)).createActor(actorSupplier, actorConfig);
    }

    @Test
    void terminateWhenAlreadyTerminating() {
        when(internalActorSystem.getStatus()).thenReturn(ActorSystemStatus.TERMINATING);
        assertThrows(ActorSystemTerminatingException.class, () -> actorSystem.terminate());
    }

    @Test
    void terminateSystem() {
        Runnable runnable = () -> {
        };
        actorSystem.terminate(runnable);
        verify(internalActorSystem, times(1)).getStatus();
        verify(internalActorSystem, times(1)).setTerminateRunnable(runnable);
        verify(internalActorSystem, times(1)).terminate();
    }

    @Test
    void registerOnTermination() {
        actorSystem.registerOnTermination(runnable);
        verify(internalActorSystem, times(1)).setTerminateRunnable(runnable);
        verify(runnable, never()).run();
    }

    @Test
    void getSupervisionStrategy() {
        assertEquals(supervisorStrategy, actorSystem.getSupervisionStrategy());
    }
}