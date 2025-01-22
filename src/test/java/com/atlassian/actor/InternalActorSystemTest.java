package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.model.ActorCell;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.PoisonPill;
import com.atlassian.actor.supervision.ActorFailureHandler;
import com.atlassian.actor.supervision.OneForOneActorFailureHandler;
import com.atlassian.actor.exceptions.ActorNameExistsException;
import com.atlassian.actor.model.ActorSystemStatus;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.observability.SignalListenerFactory;
import reactor.core.scheduler.Scheduler;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalActorSystemTest {
    @Mock
    private Scheduler scheduler;
    @Mock
    private SignalListenerFactory<Object, ?> signalListenerFactory;
    @Mock
    private SupervisorStrategy supervisorStrategy;
    @Mock
    private Runnable runnable;
    @Mock
    private Supplier<AbstractActor> actorSupplier;
    @Mock
    private ActorConfig actorConfig;

    @Mock
    private ActorRef actorRef;

    @Mock
    private InternalActor internalActor;

    private InternalActorSystem internalActorSystem;

    private MockedStatic<InternalActor> mockInternalActor;

    @BeforeEach
    void setUp() {
        mockInternalActor = mockStatic(InternalActor.class);
        mockInternalActor
                .when(() -> InternalActor.create(any(), any()))
                .thenReturn(internalActor);
        internalActorSystem = InternalActorSystem.create("test", scheduler, signalListenerFactory, supervisorStrategy);
    }

    @AfterEach
    void cleanup() {
        mockInternalActor.close();
    }

    @Test
    void create() {
        assertNotNull(internalActorSystem);
        assertEquals("test", internalActorSystem.getName());
    }

    @Test
    void getName() {
        assertEquals("test", internalActorSystem.getName());
    }

    @Test
    void createActor() {
        when(actorConfig.getName()).thenReturn("test");
        when(internalActor.getName()).thenReturn("test");
        when(internalActor.getActorRef()).thenReturn(actorRef);
        ActorRef actorRefOutput = internalActorSystem.createActor(actorSupplier, actorConfig);
        assertEquals(actorRefOutput, actorRef);
        verify(internalActor, times(1)).getActorRef();
    }

    @Test
    void createActorWhenAlreadyExists() {
        when(actorConfig.getName()).thenReturn("test");
        internalActorSystem.actors.put("test", new ActorCell(internalActor, false));
        assertThrows(ActorNameExistsException.class, () -> internalActorSystem.createActor(actorSupplier, actorConfig));
    }

    @Test
    void getActorFailureHandler() {
        ActorFailureHandler failureHandler = internalActorSystem.getActorFailureHandler();
        assertEquals(failureHandler.getClass(), OneForOneActorFailureHandler.class);
    }

    @Test
    void getParentActorCore() {
        assertEquals(null, internalActorSystem.getParentActorCore());
    }

    @Test
    void reStart() {
        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);
        internalActorSystem.actors.put("test", new ActorCell(internalActor, false));
        internalActorSystem.restart(errorData);
        verify(internalActor, times(1)).restart(errorData);
    }

    @Test
    void terminate() {
        when(internalActor.getActorRef()).thenReturn(actorRef);
        when(actorRef.isTerminating()).thenReturn(false);
        when(actorRef.getName()).thenReturn("test");
        doNothing().when(actorRef).tell(PoisonPill.getInstance());
        internalActorSystem.actors.put("test", new ActorCell(internalActor, false));
        internalActorSystem.isTerminating.set(false);
        internalActorSystem.terminate();
        internalActorSystem.terminate(); // terminating again should not terminate again

        assertEquals(ActorSystemStatus.TERMINATING, internalActorSystem.status.get());
        verify(actorRef, times(1)).isTerminating();
        verify(actorRef, times(1)).tell(PoisonPill.getInstance());
        assertTrue(internalActorSystem.actors.get("test").isTerminating());
        assertTrue(internalActorSystem.isTerminating.get());
    }

    @Test
    void terminateWhenNoChildren() {
        Runnable terminateRunnable = mock();
        doNothing().when(terminateRunnable).run();
        internalActorSystem.isTerminating.set(false);
        internalActorSystem.setTerminateRunnable(terminateRunnable);
        internalActorSystem.terminate();
        verify(terminateRunnable, times(1)).run();
        assertTrue(internalActorSystem.isTerminating.get());
        assertTrue(internalActorSystem.actors.isEmpty());
    }

    @Test
    void terminatedCallback() {
        when(actorRef.getName()).thenReturn("test");
        internalActorSystem.actors.put("test", new ActorCell(internalActor, false));
        internalActorSystem.actors.put("test2", new ActorCell(internalActor, false));
        internalActorSystem.isTerminating.set(true);
        Runnable terminateRunnable = mock();
        internalActorSystem.setTerminateRunnable(terminateRunnable);

        internalActorSystem.terminated(actorRef);

        verify(actorRef, times(3)).getName();
        verify(terminateRunnable, times(0)).run();
    }

    @Test
    void terminatedCallbackWhenAllChildrenAreTerminated() {
        when(actorRef.getName()).thenReturn("test");
        internalActorSystem.actors.put("test", new ActorCell(internalActor, false));
        Runnable terminateRunnable = mock();
        internalActorSystem.isTerminating.set(true);
        internalActorSystem.setTerminateRunnable(terminateRunnable);

        internalActorSystem.terminated(actorRef);

        verify(actorRef, times(3)).getName();
        verify(terminateRunnable, times(1)).run();
    }

    @Test
    void pause() {
        doNothing().when(internalActor).pause();
        internalActorSystem.actors.put("test", new ActorCell(internalActor, false));
        internalActorSystem.pause();
        verify(internalActor, times(1)).pause();

    }

    @Test
    void isRoot() {
        assertTrue(internalActorSystem.isRoot());
    }

    @Test
    void getSupervisorStrategy() {
        assertEquals(internalActorSystem.getSupervisorStrategy(), supervisorStrategy);
    }
}