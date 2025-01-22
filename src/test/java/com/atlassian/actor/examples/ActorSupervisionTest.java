package com.atlassian.actor.examples;

import com.atlassian.actor.AbstractActor;
import com.atlassian.actor.ActorRef;
import com.atlassian.actor.ActorSystem;
import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorSystemConfig;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.restart.config.ExponentialBackoffRestartConfig;
import com.atlassian.actor.utils.ActorLifecycleCatcher;
import com.atlassian.actor.examples.actors.SupervisionActor;
import com.atlassian.actor.supervision.strategy.OneForOneBackoffSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActorSupervisionTest {
    private static final Logger log = Logger.getLogger(ActorSupervisionTest.class.getName());

    private ActorLifecycleCatcher lifecycleCatcher;
    private SupervisorStrategy supervisorStrategy;
    private final Scheduler scheduler = Schedulers.immediate();
    private ExponentialBackoffRestartConfig exponentialBackoffRestartConfig;
    private ActorSystem actorSystem;
    private ActorRef actor;

    @BeforeEach
    void setUp() {
        exponentialBackoffRestartConfig = new ExponentialBackoffRestartConfig(
                10, Duration.ofMillis(1), Duration.ofMillis(4), Duration.ofSeconds(100));
        lifecycleCatcher = new ActorLifecycleCatcher();
        supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> SupervisorStrategyDirective.STOP);
    }

    private void setUpActorSystemAndActor() {
        actorSystem = ActorSystem.create("ControlPlaneSystem", new ActorSystemConfig(scheduler, supervisorStrategy));
        actorSystem.registerOnTermination(() -> log.info("actorSystem " + actorSystem.getName() + " is TERMINATED"));

        actor = actorSystem.actorOf(
                () -> new SupervisionActor(lifecycleCatcher, supervisorStrategy),
                new ActorConfig("SupervisionActor")
        );
        actor.tell(new SupervisionActor.Message("Hello message"));
    }

    private void setUpActorSystem() {
        supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> {
            return SupervisorStrategyDirective.RESTART;
        });
        actorSystem = ActorSystem.create("ControlPlaneSystem", new ActorSystemConfig(scheduler, supervisorStrategy));
        actorSystem.registerOnTermination(() -> log.info("actorSystem " + actorSystem.getName() + " is TERMINATED"));
    }

    private void terminateActorSystem() {
        if (!actorSystem.isTerminatingOrTerminated()) {
            actorSystem.terminate();
        }
    }

    @Test
    void testMaxRestarts() throws InterruptedException {
        supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> {
            if (ex.getError() instanceof SupervisionActor.Exception1) {
                return SupervisorStrategyDirective.RESTART;
            }
            return SupervisorStrategyDirective.STOP;
        });

        testSupervision(10);
    }

    @Test
    void testSupervisionChangesBeforeMaxRestart() throws InterruptedException {
        supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> {
            if (ex.getError() instanceof SupervisionActor.Exception1) {
                return count < 3 ? SupervisorStrategyDirective.RESTART : SupervisorStrategyDirective.ESCALATE;
            }
            return SupervisorStrategyDirective.STOP;
        });

        testSupervision(3);
    }

    @Test
    void testSupervisionChangesWithActorInitException() throws InterruptedException {
        supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> {
            if (ex.getError() instanceof SupervisionActor.Exception1) {
                return count < 3 ? SupervisorStrategyDirective.RESTART : SupervisorStrategyDirective.ESCALATE;
            }
            return SupervisorStrategyDirective.STOP;
        });

        testSupervisionWithActorInitializationFailure();
    }

    @Test
    void testSupervisionBackoffReset() throws InterruptedException {
        exponentialBackoffRestartConfig = new ExponentialBackoffRestartConfig(
                10, Duration.ofMillis(1), Duration.ofMillis(4), Duration.ofMillis(10));
        supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> {
            if (ex.getError() instanceof SupervisionActor.Exception1) {
                return count < 3 ? SupervisorStrategyDirective.RESTART : SupervisorStrategyDirective.ESCALATE;
            }
            return SupervisorStrategyDirective.STOP;
        });

        setUpActorSystemAndActor();
        for (int i = 0; i < 3; i++) {
            actor.tell(new SupervisionActor.Exception1());
        }
        Thread.sleep(50); // wait for time more than autoResetTime
        actor.tell(new SupervisionActor.Exception1());
        Thread.sleep(100);

        terminateActorSystem();
        Thread.sleep(100);
        Assertions.assertEquals(ActorLifecycleCatcher.getLifecycleEventsWithRestarts(4), lifecycleCatcher.getLifecycleEvents());
    }

    private void testSupervision(int restartCount) throws InterruptedException {
        setUpActorSystemAndActor();
        actor.tell(new SupervisionActor.Message("Hello message", 100L));
        for (int i = 0; i < 12; i++) {
            actor.tell(new SupervisionActor.Exception1());
        }

        Thread.sleep(500);
        log.info("Actor Supervision Lifecycle: " + lifecycleCatcher.getLifecycleEvents().toString());

        terminateActorSystem();

        Thread.sleep(100);
        Assertions.assertEquals(ActorLifecycleCatcher.getLifecycleEventsWithRestarts(restartCount), lifecycleCatcher.getLifecycleEvents());
    }

    private void testSupervisionWithActorInitializationFailure() throws InterruptedException {
        setUpActorSystem();
        Supplier<AbstractActor> supplierMock = Mockito.mock(Supplier.class);
        when(supplierMock.get()).thenThrow(new RuntimeException()).thenReturn(new SupervisionActor(lifecycleCatcher, supervisorStrategy));
        actor = actorSystem.actorOf(
                supplierMock,
                new ActorConfig("SupervisionActor")
        );

        Thread.sleep(100);
        terminateActorSystem();
        Assertions.assertEquals(ActorLifecycleCatcher.getLifecycleEventsWithRestarts(0), lifecycleCatcher.getLifecycleEvents());
    }
}