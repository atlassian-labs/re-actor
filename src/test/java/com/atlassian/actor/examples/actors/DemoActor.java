package com.atlassian.actor.examples.actors;

import com.atlassian.actor.AbstractActor;
import com.atlassian.actor.Receive;
import com.atlassian.actor.ReceiveBuilder;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoActor extends AbstractActor {

    private static final Logger log = LoggerFactory.getLogger(DemoActor.class);

    @Override
    public void preStart() {
        log.info("initialised ");
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Integer.class, (m, r) -> log.info("Processing Integer {}", m))
                .match(String.class, (m, r) -> log.info("Processing String {}", m))
                .match(Message.class, (m, r) -> processMessage(m))
                .matchAny((m, r) -> log.info("Message type didn't match, ignoring it {}", m))
                .build();
    }

    @Override
    public void postStop() {
        log.info("Inside postStop");
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

    private void processMessage(Message m) throws InterruptedException {
        log.info("Processing Message {}", m.msg);
        Thread.sleep(m.delay);
        log.info("Processed Message {}", m.msg);

    }

    public static class Exception1 extends Exception {
    }

    public static class Message {
        private String msg;
        private long delay;

        public Message(String msg, long delay) {
            this.delay = delay;
            this.msg = msg;
        }
    }
}