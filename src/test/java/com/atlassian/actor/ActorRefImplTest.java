package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorSystemConfig;
import com.atlassian.actor.exceptions.ActorTerminatingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActorRefImplTest {
    @Mock
    private ActorSystemConfig actorSystemConfig;
    @Mock
    private Supplier<AbstractActor> reactorSupplier;
    @Mock
    private ActorConfig actorConfig;
    @Mock
    private InternalActor internalActor;
    @Mock
    private ActorRef otherActorRef;
    private ActorRef actorRef;

    @BeforeEach
    void setUp() {
        when(internalActor.getName()).thenReturn("test");
        actorRef = ActorRefImpl.create(internalActor);
    }

    @Test
    void create() {
        assertNotNull(actorRef);
        assertEquals("test", actorRef.getName());
    }

    @Test
    void getName() {
        assertEquals("test", actorRef.getName());
    }

    @Test
    void tell() {

        String message = "message";
        doNothing().when(internalActor).publish(message);
        actorRef.tell(message);
        verify(internalActor, times(1)).publish(message);
    }

    @Test
    void tellWithDelay() {
        String message = "message";
        Duration delay = Duration.ofMillis(10L);
        doNothing().when(internalActor).publish(message, delay);

        actorRef.tell(message, delay);
        verify(internalActor, times(1)).publish(message, delay);
    }

    @Test
    void isTerminating() {
        when(internalActor.isTerminating()).thenReturn(true);
        assertTrue(actorRef.isTerminating());
        verify(internalActor, times(1)).isTerminating();
    }

    @Test
    void isRunning() {
        when(internalActor.isRunning()).thenReturn(true);
        assertTrue(actorRef.isRunning());
        verify(internalActor, times(1)).isRunning();
    }

    @Test
    void watch() {
        doNothing().when(otherActorRef).addWatcher(actorRef);
        actorRef.watch(otherActorRef);
        verify(otherActorRef, times(1)).addWatcher(actorRef);
    }

    @Test
    void unWatch() {
        doNothing().when(otherActorRef).removeWatcher(actorRef);
        actorRef.unWatch(otherActorRef);
        verify(otherActorRef, times(1)).removeWatcher(actorRef);
    }

    @Test
    void addWatcher() {
        doNothing().when(internalActor).addWatcher(otherActorRef);
        actorRef.addWatcher(otherActorRef);
        verify(internalActor, times(1)).addWatcher(otherActorRef);
    }

    @Test
    void removeWatcher() {
        doNothing().when(internalActor).removeWatcher(otherActorRef);
        actorRef.removeWatcher(otherActorRef);
        verify(internalActor, times(1)).removeWatcher(otherActorRef);
    }

    @Test
    void actorOf() {
        when(internalActor.isTerminating()).thenReturn(false);
        when(internalActor.createActor(reactorSupplier, actorConfig)).thenReturn(otherActorRef);

        ActorRef resultActorRef = actorRef.actorOf(reactorSupplier, actorConfig);
        assertEquals(otherActorRef, resultActorRef);
        verify(internalActor, times(1)).createActor(reactorSupplier, actorConfig);
    }

    @Test
    void actorOfWhenTerminating() {
        when(internalActor.isTerminating()).thenReturn(true);
        assertThrows(ActorTerminatingException.class, () -> actorRef.actorOf(reactorSupplier, actorConfig));
    }
}