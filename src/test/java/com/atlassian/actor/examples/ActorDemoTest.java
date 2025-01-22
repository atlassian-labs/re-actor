package com.atlassian.actor.examples;

import com.atlassian.actor.AbstractActor;
import com.atlassian.actor.ActorRef;
import com.atlassian.actor.ActorSystem;
import com.atlassian.actor.Receive;
import com.atlassian.actor.ReceiveBuilder;
import com.atlassian.actor.Responder;
import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorSystemConfig;
import com.atlassian.actor.examples.actors.SupervisionActor;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.restart.config.ExponentialBackoffRestartConfig;
import com.atlassian.actor.supervision.strategy.OneForOneBackoffSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class ActorDemoTest {

    public static class MyActor extends AbstractActor {

        @Override
        public void preStart() {
            print("initialised ");
        }

        @Override
        public void preRestart(Throwable reason, Object message) throws Exception {
            print("Inside preRestart");
        }

        @Override
        public void postRestart(Throwable reason) throws Exception {
            print("Inside postRestart");
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(Message.class, this::processMessage)
                    .match(GetMessage.class, this::getMessage)
                    .match(Error.class, (m, r) -> {
                        throw new Exception1();
                    })
                    .matchAny((m, r) -> print("Message type didn't match, ignoring it " + m))
                    .build();
        }

        private void getMessage(GetMessage getMessage, Responder responder) {
            print("Responding for message id : " + getMessage.id);
            responder.setObject("Response for id " + getMessage.id);
        }

        @Override
        public void postStop() {
            print("Inside postStop");
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return new OneForOneSupervisorStrategy(ex -> {
                if (ex.getError() instanceof Exception1) {
                    return SupervisorStrategyDirective.RESUME;
                }
                return SupervisorStrategyDirective.STOP;
            });
        }

        private void processMessage(Message m, Responder resp) throws InterruptedException {
            print("Processing Message " + m.msg);
            Thread.sleep(m.delay);
            print("Processed Message " + m.msg);
        }

        public static class Exception1 extends Exception {
        }

        public static class Error {
        }

        public static class Message {
            private String msg;
            private long delay;

            public Message(String msg, long delay) {
                this.delay = delay;
                this.msg = msg;
            }
        }

        public static class GetMessage {
            private String id;

            public GetMessage(String id) {
                this.id = id;
            }
        }
    }

    @Test
    void run() throws Exception {
        Scheduler scheduler = Schedulers.boundedElastic();

        ActorSystem actorSystem = ActorSystem.create("MyActorSystem", new ActorSystemConfig(scheduler));

        actorSystem.registerOnTermination(() -> print("actorSystem " + actorSystem.getName() + " is TERMINATED"));

        ActorRef actor = actorSystem.actorOf(
                MyActor::new,
                new ActorConfig("MyActor")
        );

        for (int i = 1; i <= 5; i++) {
            actor.tell(new MyActor.Message("Hello message " + i, 100L));
            print("Published message - " + i);
        }

        print("Published all messages");

        print("Getting message for id : id1");
        String response = (String) actor.ask(new MyActor.GetMessage("id1"), 2_000);
        print("Response: " + response);

        Thread.sleep(30000);

        actorSystem.terminate();
    }

    @Test
    void runWithSupervisorStrategy() throws Exception {
        Scheduler scheduler = Schedulers.boundedElastic();

        ExponentialBackoffRestartConfig exponentialBackoffRestartConfig = new ExponentialBackoffRestartConfig(
                10, Duration.ofMillis(1), Duration.ofMillis(4), Duration.ofSeconds(100));
        SupervisorStrategy supervisorStrategy = new OneForOneBackoffSupervisorStrategy(exponentialBackoffRestartConfig, (count, ex) -> {
            if (ex.getError() instanceof MyActor.Exception1) {
                return SupervisorStrategyDirective.RESTART;
            }
            return SupervisorStrategyDirective.STOP;
        });

        ActorSystem actorSystem = ActorSystem.create("MyActorSystem", new ActorSystemConfig(scheduler, supervisorStrategy));

        actorSystem.registerOnTermination(() -> print("actorSystem " + actorSystem.getName() + " is TERMINATED"));

        ActorRef actor = actorSystem.actorOf(
                MyActor::new,
                new ActorConfig("MyActor")
        );

        actor.tell(new MyActor.Error());
        for (int i = 1; i <= 5; i++) {
            actor.tell(new MyActor.Message("Hello message " + i, 100L));
            print("Published message - " + i);
        }
        print("Published all messages");

        Thread.sleep(10000);

        actorSystem.terminate();
    }


    public static void print(String msg) {
        String threadName = Thread.currentThread().getName();
        System.out.print("Thread: [" + threadName + "], " + msg + "\n");
    }

}