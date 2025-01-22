package com.atlassian.actor.examples.actors;

import com.atlassian.actor.AbstractActor;
import com.atlassian.actor.Receive;
import com.atlassian.actor.ReceiveBuilder;
import com.atlassian.actor.utils.ActorLifecycleCatcher;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupervisionActor extends AbstractActor {

    private final ActorLifecycleCatcher lifecycleCatcher;
    private final SupervisorStrategy supervisorStrategy;

    public SupervisionActor(ActorLifecycleCatcher lifecycleCatcher, SupervisorStrategy supervisorStrategy) {
        this.lifecycleCatcher = lifecycleCatcher;
        this.supervisorStrategy = supervisorStrategy;
    }

    private static final Logger log = LoggerFactory.getLogger(SupervisionActor.class);

    @Override
    public void preStart() {
        lifecycleCatcher.preStart();
    }

    @Override
    public Receive createReceive() {
        lifecycleCatcher.createReceive();
        return ReceiveBuilder.create()
                .match(Message.class, (m, r) -> processMessage(m))
                .match(Exception1.class, (e, r) -> {
                    throw new Exception1();
                })
                .matchAny((m, r) -> log.info("Message type didn't match, ignoring it {}", m))
                .build();
    }

    @Override
    public void postStop() {
        lifecycleCatcher.postStop();
    }

    @Override
    public void preRestart(Throwable reason, Object message) throws Exception {
        lifecycleCatcher.preRestart(reason, message);
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        lifecycleCatcher.postRestart(reason);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
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

        public Message(String msg) {
            this.delay = 0L;
            this.msg = msg;
        }
    }
}