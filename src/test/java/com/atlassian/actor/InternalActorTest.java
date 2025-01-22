package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorCreationConfig;
import com.atlassian.actor.model.ActorCell;
import com.atlassian.actor.model.ActorStatus;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.PoisonPill;
import com.atlassian.actor.supervision.ActorFailureHandlerFactory;
import com.atlassian.actor.supervision.OneForOneBackoffActorFailureHandler;
import com.atlassian.actor.exceptions.ActorNameExistsException;
import com.atlassian.actor.exceptions.InvalidMessageException;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.observability.SignalListenerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InternalActorTest {
    private Scheduler scheduler = Schedulers.immediate();
    @Mock
    private SignalListenerFactory signalListenerFactory;
    @Mock
    private SupervisorStrategy supervisorStrategy;
    @Mock
    private Runnable runnable;
    @Mock
    private Supplier<AbstractActor> actorSupplier;
    @Mock
    private Supplier<AbstractActor> actorSupplier2;
    @Mock
    private AbstractActor abstractActor;
    @Mock
    private Receive receiver;
    @Mock
    private ActorCreationConfig actorCreationConfig;
    @Mock
    private ActorConfig actorConfig;
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
    private MockedStatic<InternalActor> mockInternalActor;
    private MockedStatic<ActorRefImpl> mockActorRefImpl;
    private MockedStatic<ActorFailureHandlerFactory> mockDefaultActorFailureHandler;
    private static final String ACTOR_NAME = "test";
    private static final String CHILD_ACTOR_NAME = "childActor";
    private static final String MESSAGE = "message";
    private static final String MESSAGE_WHEN_PAUSED = "Message when paused";

    @BeforeEach
    void setUp() throws Exception {
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

        mockCreateNewActorInstance();
        doNothing().when(abstractActor).preStart();
        internalActor = InternalActor.create(actorSupplier, actorCreationConfig);

        verifyCreateNewActorInstance(1);
        verify(abstractActor, times(1)).preStart();
    }

    void mockCreateNewActorInstance() {
        when(actorSupplier.get()).thenReturn(abstractActor);
        doNothing().when(abstractActor).setSelf(actorRef);
        when(abstractActor.createReceive()).thenReturn(receiver);
    }

    void verifyCreateNewActorInstance(Integer count) {
        verify(actorSupplier, times(count)).get();
        verify(abstractActor, times(count)).setSelf(actorRef);
        verify(abstractActor, times(count)).createReceive();
    }

    @AfterEach
    void cleanup() {
        if (mockInternalActor != null) {
            mockInternalActor.close();
        }
        mockActorRefImpl.close();
        mockDefaultActorFailureHandler.close();
    }

    @Test
    void create() {
        assertNotNull(internalActor);
        assertEquals(ACTOR_NAME, internalActor.getName());
    }

    @Test
    void getName() {
        assertEquals(ACTOR_NAME, internalActor.getName());
    }

    @Test
    void publish() throws Exception {
        doNothing().when(receiver).process(MESSAGE, null);
        internalActor.publish(MESSAGE);
        verify(receiver, times(1)).process(MESSAGE, null);
    }

    @Test
    void publishWhenActorProcessThrowsException() throws Exception {
        doThrow(new RuntimeException("Error in Processing")).when(receiver).process(MESSAGE, null);
        internalActor.publish(MESSAGE);
        verify(receiver, times(1)).process(MESSAGE, null);
        verify(oneForOneBackoffActorFailureHandler, times(1)).handle(any());
    }

    @Test
    void publishWhenReactiveStreamIsStopped() throws Exception {
        doNothing().when(abstractActor).postStop();
        internalActor.sink.tryEmitComplete();
        internalActor.publish(MESSAGE); // This message won't be processed

        verify(abstractActor, times(1)).postStop();
        verify(receiver, times(0)).process(MESSAGE, null);
        assertEquals(ActorStatus.TERMINATED, internalActor.actorStatus.get());
    }

    @Test
    void publishWithDelay() throws Exception {
        doNothing().when(receiver).process(MESSAGE, null);
        internalActor.publish(MESSAGE, Duration.ofMillis(100));
        verify(receiver, timeout(250).times(1)).process(MESSAGE, null);
    }

    @Test
    void terminateWhenThereAreNoChildren() throws Exception {

        doNothing().when(abstractActor).postStop();
        internalActor.terminate();
        internalActor.publish(MESSAGE); // This message won't be processed
        internalActor.terminate(); // Terminating again should not try to terminate again

        verify(abstractActor, times(1)).postStop();
        verify(receiver, times(0)).process(MESSAGE, null);
        assertEquals(ActorStatus.TERMINATED, internalActor.actorStatus.get());
    }

    @Test
    void terminateWhenThereAreChildren() throws Exception {
        when(childActorRef.isTerminating()).thenReturn(false);
        when(childActorRef.getName()).thenReturn(CHILD_ACTOR_NAME);
        doNothing().when(childActorRef).tell(PoisonPill.getInstance());
        internalActor.actors.put(CHILD_ACTOR_NAME, new ActorCell(childInternalActor, false));
        doNothing().when(abstractActor).postStop();

        internalActor.terminate();
        internalActor.publish(MESSAGE); // This message won't be processed

        verify(childActorRef, times(1)).isTerminating();
        verify(childActorRef, times(1)).tell(PoisonPill.getInstance());
        verify(receiver, times(0)).process(MESSAGE, null);
        assertEquals(ActorStatus.TERMINATING, internalActor.actorStatus.get());
        assertTrue(internalActor.actors.get(CHILD_ACTOR_NAME).isTerminating());
    }

    @Test
    void testGetChildByName() {
        when(childActorRef.getName()).thenReturn(CHILD_ACTOR_NAME);
        internalActor.actors.put(CHILD_ACTOR_NAME, new ActorCell(childInternalActor, false));
        ActorRef childActor = internalActor.getChildByName(CHILD_ACTOR_NAME);
        assertNotNull(childActor);
        assertEquals(childActor, childActorRef);

        assertNull(internalActor.getChildByName("Some other actor"));
    }

    @Test
    void createActor() {
        mockInternalActor = mockStatic(InternalActor.class);
        mockInternalActor
                .when(() -> InternalActor.create(any(), any()))
                .thenReturn(childInternalActor);
        when(actorCreationConfig.getScheduler()).thenReturn(scheduler);
        when(actorConfig.getName()).thenReturn(ACTOR_NAME);
        when(actorConfig.getQueueSize()).thenReturn(null);
        when(actorConfig.getTags()).thenReturn(Collections.emptyList());

        ActorRef actorRefOutput = internalActor.createActor(actorSupplier, actorConfig);
        assertEquals(actorRefOutput, childActorRef);
        verify(childInternalActor, times(1)).getActorRef();
    }

    @Test
    void createActorWhenAlreadyExists() {
        when(actorConfig.getName()).thenReturn(ACTOR_NAME);
        when(actorCreationConfig.getScheduler()).thenReturn(scheduler);
        when(actorCreationConfig.getScheduler()).thenReturn(scheduler);
        internalActor.actors.put(ACTOR_NAME, new ActorCell(internalActor, false));
        assertThrows(ActorNameExistsException.class, () -> internalActor.createActor(actorSupplier, actorConfig));
    }

    @Test
    void addWatcher() {
        internalActor.addWatcher(childActorRef);
        assertEquals(1, internalActor.watchers.size());
        assertTrue(internalActor.watchers.contains(childActorRef));
    }

    @Test
    void removeWatcher() {
        internalActor.addWatcher(childActorRef);
        internalActor.removeWatcher(childActorRef);
        assertEquals(0, internalActor.watchers.size());
        assertFalse(internalActor.watchers.contains(childActorRef));
    }

    @Test
    void getActorFailureHandler() {
        assertEquals(internalActor.getActorFailureHandler(), oneForOneBackoffActorFailureHandler);
    }

    @Test
    void getParentActorCore() {
        assertEquals(internalActor.getParentActorCore(), parentActorCoreSystem);
    }

    @Test
    void pause() {
        internalActor.actors.put(ACTOR_NAME, new ActorCell(childInternalActor, false));
        internalActor.pause();
        internalActor.pause(); // This should not pause again

        verify(childInternalActor, times(1)).pause();
        assertEquals(ActorStatus.PAUSED, internalActor.actorStatus.get());
    }

    @Test
    void reStart() throws Exception {
        pause();

        internalActor.publish(MESSAGE_WHEN_PAUSED);
        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, MESSAGE, actorRef);

        doNothing().when(abstractActor).preRestart(errorData.getError(), errorData.getMessage());
        doNothing().when(abstractActor).postRestart(errorData.getError());

        doNothing().when(childInternalActor).restart(errorData);

        internalActor.restart(errorData);

        verify(abstractActor, times(1)).preRestart(errorData.getError(), errorData.getMessage());
        verifyCreateNewActorInstance(2);
        verify(abstractActor, times(1)).postRestart(errorData.getError());
        verify(childInternalActor, times(1)).restart(errorData);
        assertEquals(ActorStatus.RUNNING, internalActor.actorStatus.get());
        verify(receiver, times(1)).process(MESSAGE_WHEN_PAUSED, null);
    }

    @Test
    void reStartWhenErrorIsInChildActor() throws Exception {
        pause();

        internalActor.publish(MESSAGE_WHEN_PAUSED);
        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, MESSAGE, childActorRef);

        doNothing().when(abstractActor).preRestart(errorData.getError(), null);
        doNothing().when(abstractActor).postRestart(errorData.getError());

        doNothing().when(childInternalActor).restart(errorData);

        internalActor.restart(errorData);

        verify(abstractActor, times(1)).preRestart(errorData.getError(), null);
        verifyCreateNewActorInstance(2);
        verify(abstractActor, times(1)).postRestart(errorData.getError());
        verify(childInternalActor, times(1)).restart(errorData);
        assertEquals(ActorStatus.RUNNING, internalActor.actorStatus.get());
        verify(receiver, times(1)).process(MESSAGE_WHEN_PAUSED, null);
    }
    
    @Test
    void isRoot() {
        assertFalse(internalActor.isRoot());
    }

    @Test
    void getSupervisorStrategy() {
        when(abstractActor.supervisorStrategy()).thenReturn(supervisorStrategy);
        assertEquals(internalActor.getSupervisorStrategy(), supervisorStrategy);
        verify(abstractActor, times(1)).supervisorStrategy();
    }

    @Test
    void throwInvalidMessageException() {
        assertThrows(InvalidMessageException.class, () -> internalActor.publish(null));
    }
}