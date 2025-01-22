package com.atlassian.actor;

import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.ActorFailureHandler;
import com.atlassian.actor.supervision.ActorFailureHandlerFactory;
import com.atlassian.actor.supervision.strategy.RootSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.Duration;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

abstract class BaseActorFailureHandlerTest {
    protected SupervisorStrategy parentSupervisorStrategy;
    protected SupervisorStrategy parentParentSupervisorStrategy;
    @Mock
    protected ActorRef actorRef;
    @Mock
    protected ActorCore parentActorCoreSystem;
    @Mock
    protected ActorCore parentParentActorCoreSystem;
    @Mock
    protected InternalActor internalActor;
    protected ActorFailureHandler actorFailureHandler;
    protected ActorFailureHandler parentActorFailureHandler;
    protected ActorFailureHandler parentParentActorFailureHandler;
    protected SupervisorStrategyDirective parentSupervisorStrategyDirective;
    protected SupervisorStrategyDirective parentParentSupervisorStrategyDirective;
    protected Duration waitIntervalForRestart = Duration.ofMillis(50);

    abstract void setupSupervisorStrategies();

    void setUp() {
        setupSupervisorStrategies();

        doNothing().when(internalActor).pause();
        doNothing().when(internalActor).terminate();

        when(internalActor.getParentActorCore()).thenReturn(parentActorCoreSystem);
        when(parentActorCoreSystem.getParentActorCore()).thenReturn(parentParentActorCoreSystem);
        when(parentParentActorCoreSystem.getParentActorCore()).thenReturn(null);

        when(parentActorCoreSystem.getSupervisorStrategy()).thenReturn(parentSupervisorStrategy);
        when(parentParentActorCoreSystem.getSupervisorStrategy()).thenReturn(parentParentSupervisorStrategy);

        when(internalActor.getParentSupervisorStrategy()).thenReturn(parentSupervisorStrategy);
        when(parentActorCoreSystem.getParentSupervisorStrategy()).thenReturn(parentParentSupervisorStrategy);
        when(parentParentActorCoreSystem.getParentSupervisorStrategy()).thenReturn(RootSupervisorStrategy.getInstance());

        actorFailureHandler = ActorFailureHandlerFactory.create(internalActor);
        parentActorFailureHandler = ActorFailureHandlerFactory.create(parentActorCoreSystem);
        parentParentActorFailureHandler = ActorFailureHandlerFactory.create(parentParentActorCoreSystem);

        when(internalActor.getActorFailureHandler()).thenReturn(actorFailureHandler);
        when(parentActorCoreSystem.getActorFailureHandler()).thenReturn(parentActorFailureHandler);
        when(parentParentActorCoreSystem.getActorFailureHandler()).thenReturn(parentParentActorFailureHandler);
    }

    protected void waitForRestart() throws InterruptedException {
        Thread.sleep(waitIntervalForRestart.toMillis());
    }

    @Test
    void handleErrorWithResume() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESUME;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);

        actorFailureHandler.handle(errorData);
        verify(internalActor, times(0)).pause();
        verify(internalActor, times(0)).terminate();
    }

    @Test
    void handleErrorWithStop() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.STOP;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);

        actorFailureHandler.handle(errorData);
        verify(internalActor, times(1)).pause();
        verify(internalActor, times(1)).terminate();
    }

    @Test
    void handleErrorWithReStart() throws InterruptedException {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);

        actorFailureHandler.handle(errorData);
        waitForRestart();
        verify(internalActor, times(1)).pause();
        verify(internalActor, times(1)).restart(errorData);
    }

    @Test
    void handleErrorWithReStartAndMaxRetries() throws InterruptedException {
        setUp();
        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);

        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;
        actorFailureHandler.handle(errorData);
        waitForRestart();

        parentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;
        actorFailureHandler.handle(errorData);
        waitForRestart();

        parentSupervisorStrategyDirective = SupervisorStrategyDirective.STOP;
        actorFailureHandler.handle(errorData);

        verify(internalActor, times(3)).pause();
        verify(internalActor, times(2)).restart(errorData);
        verify(internalActor, times(1)).terminate();
    }

    @Test
    void handleErrorWithEscalateAndStop() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.ESCALATE;
        parentParentSupervisorStrategyDirective = SupervisorStrategyDirective.STOP;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);

        actorFailureHandler.handle(errorData);
        verify(parentActorCoreSystem, times(1)).pause();
        verify(parentActorCoreSystem, times(1)).terminate();
    }

    @Test
    void handleErrorWithEscalateAndResume() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.ESCALATE;
        parentParentSupervisorStrategyDirective = SupervisorStrategyDirective.RESUME;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);

        actorFailureHandler.handle(errorData);
        verify(internalActor, times(0)).pause();
        verify(parentActorCoreSystem, times(0)).pause();
        verify(parentActorCoreSystem, times(0)).terminate();
    }

    @Test
    void handleErrorWithEscalateAndRestart() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.ESCALATE;
        parentParentSupervisorStrategyDirective = SupervisorStrategyDirective.RESTART;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);

        actorFailureHandler.handle(errorData);
        verify(parentActorCoreSystem, timeout(waitIntervalForRestart.toMillis()).times(1)).pause();
        verify(parentActorCoreSystem, timeout(waitIntervalForRestart.toMillis()).times(1)).restart(errorData);
    }

    @Test
    void handleErrorWithEscalateAndEscalateAndRoot() {
        setUp();
        parentSupervisorStrategyDirective = SupervisorStrategyDirective.ESCALATE;
        parentParentSupervisorStrategyDirective = SupervisorStrategyDirective.ESCALATE;

        Throwable throwable = new Exception("Test Exception");
        ErrorData errorData = new ErrorData(throwable, "message", actorRef);
        doNothing().when(internalActor).restart(errorData);

        actorFailureHandler.handle(errorData);
        verify(parentParentActorCoreSystem, times(1)).pause();
        verify(parentParentActorCoreSystem, times(1)).terminate();
    }
}