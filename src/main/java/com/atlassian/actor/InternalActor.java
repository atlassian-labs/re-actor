package com.atlassian.actor;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorCreationConfig;
import com.atlassian.actor.model.ActorStatus;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.Pair;
import com.atlassian.actor.model.PoisonPill;
import com.atlassian.actor.model.Terminated;
import com.atlassian.actor.supervision.ActorFailureHandler;
import com.atlassian.actor.supervision.ActorFailureHandlerFactory;
import com.atlassian.actor.exceptions.ActorInitialisationException;
import com.atlassian.actor.exceptions.ActorKilledException;
import com.atlassian.actor.exceptions.ActorPostStopException;
import com.atlassian.actor.exceptions.ActorTerminatingException;
import com.atlassian.actor.exceptions.InvalidMessageException;
import com.atlassian.actor.model.Kill;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import com.google.common.annotations.VisibleForTesting;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * InternalActor is internally used by ActorRefImpl. It contains the core logic of the actor which is created using the reactor features
 * like Sinks, Flux, Scheduler, etc. It provides the methods to publish message, restart, terminate and pause the actor.
 */
public class InternalActor extends ActorCore {
    private final Supplier<AbstractActor> actorSupplier;
    private final ActorCreationConfig actorCreationConfig;
    private final Queue<Object> queue;
    private final Queue<Object> tempQueueForExistingMessages = new LinkedBlockingQueue<>();
    private final Queue<Object> tempQueueForNewMessages = new LinkedBlockingQueue<>();

    @VisibleForTesting
    protected final Sinks.Many<Object> sink;

    @VisibleForTesting
    protected Disposable disposable;
    @VisibleForTesting
    protected final Set<ActorRef> watchers = new HashSet<>();
    private final CountDownLatch childTerminateCountDownLatch = new CountDownLatch(1);
    private AbstractActor actor;

    @VisibleForTesting
    protected final AtomicReference<ActorStatus> actorStatus = new AtomicReference<>(ActorStatus.CREATED);
    private final ActorRef actorRef;
    private final ActorFailureHandler actorFailureHandler;
    private final ActorCore parentActorCore;
    private Receive receiver;

    private InternalActor(Supplier<AbstractActor> actorSupplier, ActorCreationConfig actorCreationConfig) {
        super(actorCreationConfig.getName());
        this.actorSupplier = actorSupplier;
        this.actorCreationConfig = actorCreationConfig;
        this.queue = new LinkedBlockingQueue<>(actorCreationConfig.getQueueSize());
        this.sink = Sinks.many().unicast().onBackpressureBuffer(queue);
        Flux<Object> flux = sink.asFlux().publishOn(actorCreationConfig.getScheduler());
        subscribeToFlux(flux);
        actorStatus.set(ActorStatus.STARTING);
        actorRef = ActorRefImpl.create(this);
        parentActorCore = actorCreationConfig.getParentActor();
        actorFailureHandler = ActorFailureHandlerFactory.create(this);
        publish(Init.getInstance());
    }

    public static InternalActor create(Supplier<AbstractActor> actorSupplier, ActorCreationConfig actorCreationConfig) {
        return new InternalActor(actorSupplier, actorCreationConfig);
    }

    public ActorRef getActorRef() {
        return actorRef;
    }

    public void publish(Object message) {
        if (message == null) {
            logger.error("null message received in {}", getName());
            throw new InvalidMessageException("Message cannot be null in actor " + getName());
        } else if (acceptExternalMessage() || isActorInternalMessage(message)) {
            sink.emitNext(message, new EmitFailureErrorHandler(Duration.ofSeconds(2)));
        } else if (actorStatus.get().isTerminatingOrTerminated()) {
            deadLetterMessage(message);
        } else {
            logger.info("Actor is paused, adding message to a temporary queue: {}", message);
            tempQueueForNewMessages.add(message);
        }
    }

    private static class LatchedMessage {
        private final Object obj;
        private final Responder responder;

        LatchedMessage(Object obj, Responder latch) {
            this.obj = obj;
            this.responder = latch;
        }

        public Object getObj() {
            return obj;
        }

        public Responder getResponder() {
            return responder;
        }
    }

    public void publish(Object obj, Responder latch) {
        publish(new LatchedMessage(obj, latch));
    }

    public void publish(Object message, Duration delay) {
        Mono.delay(delay)
                .map(ignored -> {
                    publish(message);
                    return message;
                })
                .subscribe();

    }

    @Override
    public void terminate() {
        if (actorStatus.get().canTerminate()) {
            logger.info("Initiating termination process for actor {}. Current status: {}", getName(), actorStatus.get());
            publish(PoisonPill.getInstance());
        } else {
            logger.warn("Actor {} can't be terminated due to status {}", getName(), actorStatus.get());
        }
    }

    public boolean isTerminating() {
        return actorStatus.get().isTerminating();
    }

    public boolean isTerminated() {
        return actorStatus.get().isTerminated();
    }

    public boolean isRunning() {
        return actorStatus.get().isRunningOrStarting();
    }

    private boolean acceptExternalMessage() {
        return actorStatus.get().isRunningOrStarting();
    }

    public ActorRef createActor(Supplier<AbstractActor> actorSupplier, ActorConfig actorConfig) {
        if (isTerminating()) {
            throw new ActorTerminatingException("Actor " + getName() + " is terminating. Can't create child actor");
        }
        return createActor(
                actorSupplier,
                new ActorCreationConfig(
                        actorConfig.getName(),
                        this,
                        actorCreationConfig.getScheduler(),
                        actorConfig.getQueueSize(),
                        actorCreationConfig.getSignalListenerFactory(),
                        actorConfig.getTags()
                )
        );
    }

    public void addWatcher(ActorRef actorRef) {
        watchers.add(actorRef);
    }

    public void removeWatcher(ActorRef actorRef) {
        watchers.remove(actorRef);
    }

    @Override
    public ActorFailureHandler getActorFailureHandler() {
        return actorFailureHandler;
    }

    @Override
    public ActorCore getParentActorCore() {
        return parentActorCore;
    }

    @Override
    public void restart(ErrorData errorData) {
        publish(new Restart(errorData));
    }

    /**
     * Pause the actor and all its children. During pause, actor stores all external messages(except PoisonPill) in temporary queue.
     * And these messages are processed once actor is restarts &amp; resumes processing.
     */
    @Override
    public synchronized void pause() {
        if (actorStatus.get().canPause()) {
            pauseAllChildren();
            pauseActor();
        } else {
            logger.warn("Can't pause actor {} because current status is {}", getName(), actorStatus.get());
        }
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public SupervisorStrategy getSupervisorStrategy() {
        return actor.supervisorStrategy();
    }

    @Override
    protected void childrenTerminateCallback() {
        logger.info("All children are terminated in time for actor {}", getName());
        try {
            if (actor != null) {
                actor.postStop();
            }
        } catch (Exception e) {
            throw new ActorPostStopException(e);
        }
        if (!isReactiveStreamStopped()) {
            sink.tryEmitComplete();
        }
        childTerminateCountDownLatch.countDown();
    }

    public boolean isReactiveStreamStopped() {
        return disposable != null && disposable.isDisposed();
    }

    private void subscribeToFlux(Flux<Object> flux) {
        disposable = addMeterRegistry(flux)
                .filter(this::interceptor)
                .map(this::processWrapper)
                .doOnError(error -> logger.error("Terminating Actor due to error " + error + " for " + getName(), error))
                .onErrorStop()
                .doOnTerminate(this::onTerminate)
                .subscribe();
    }

    private Flux<Object> addMeterRegistry(Flux<Object> flux) {
        if (actorCreationConfig.getSignalListenerFactory() != null) {
            Flux<Object> tempFlux = flux;
            for (Pair<String, String> tag : actorCreationConfig.getTags()) {
                tempFlux = tempFlux.tag(tag.getFirst(), tag.getSecond());
            }
            return tempFlux.tap(actorCreationConfig.getSignalListenerFactory());
        }
        return flux;
    }

    private void poisonPillSelf() {
        actorStatus.set(ActorStatus.TERMINATING);
        logger.info("Now will terminate all children for actor {}", getName());
        terminateAllChildren();
    }

    private void onTerminate() {

        if (!isTerminating()) {
            logger.info("Actor {} is not terminating yet. Sending poison pill.", getName());
            poisonPillSelf();
        }
        boolean allChildrenTerminated = false;
        try {
            allChildrenTerminated = childTerminateCountDownLatch.await(10_000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // don't forget to handle interruption
        }
        if (!allChildrenTerminated) {
            logger.warn("All children were not terminated in time for actor {}", getName());
        } else {
            actorStatus.set(ActorStatus.TERMINATED);
            logger.info("All children & Actor successfully terminated in time {}", getName());
        }
        notifyWatchersAndActorSystem();
    }

    private void notifyWatchersAndActorSystem() {
        watchers.forEach(watcher -> watcher.tell(new Terminated(actorRef)));
        logger.info("Notifying parent actor core {} about termination", parentActorCore.getName());
        parentActorCore.terminated(actorRef);
    }

    private Object processWrapper(Object message) {
        try {
            if (message instanceof Init) {
                initWrapper();
            } else if (message instanceof PoisonPill) {
                poisonPillSelf();
            } else if (message instanceof Restart) {
                reStartProcessor((Restart) message);
            } else if (message instanceof Kill) {
                throw new ActorKilledException("Actor " + getName() + " is killed by Kill message");
            } else {
                if (receiver != null) {
                    if (message instanceof LatchedMessage) {
                        LatchedMessage lm = (LatchedMessage) message;
                        receiver.process(lm.obj, lm.responder);
                    } else {
                        receiver.process(message, null);
                    }
                }
            }
        } catch (Throwable error) {
            actorFailureHandler.handle(new ErrorData(error, message, actorRef));
        }
        return message;
    }

    private void initWrapper() {
        try {
            actor = createNewActorInstance();
            actorStatus.set(ActorStatus.RUNNING);
            actor.preStart();
            createReceiver(actor);
            logger.info("Actor {} is started", getName());
        } catch (Exception e) {
            throw new ActorInitialisationException("Error while starting actor " + getName() + " : " + e);
        }
    }

    private boolean isActorInternalMessage(Object message) {
        return message instanceof Init || message instanceof Terminated || message instanceof PoisonPill || message instanceof Restart;
    }

    private void reStartProcessor(Restart restartMessage) throws Exception {
        if (actor != null) {
            if (restartMessage.getErrorData().getActorRef().equals(actorRef)) {
                actor.preRestart(restartMessage.getErrorData().getError(), restartMessage.getErrorData().getMessage());
            } else {
                actor.preRestart(restartMessage.getErrorData().getError(), null);
            }
            actor = createNewActorInstance();
            createReceiver(actor);
            actor.postRestart(restartMessage.getErrorData().getError());
        } else {
            // if actor fails during initialization, supervision strategy on restart will try to create the actor instance again and then call preStart.
            actor = createNewActorInstance();
            actor.preStart();
            createReceiver(actor);
        }

        moveMessagesToQueueAndUpdateStatus();

        logger.info("Actor {} successfully restarted, now restarting its children( {} ) : {} ", getName(), actors.size(), actors);
        restartAllChildren(restartMessage.getErrorData());
    }

    private void moveMessagesToQueueAndUpdateStatus() {
        queue.addAll(tempQueueForExistingMessages);
        queue.addAll(tempQueueForNewMessages);
        // Once messages from both the queues are added,
        // status is changed so that new messages can be pushed to original queue.
        actorStatus.set(ActorStatus.RUNNING);
        tempQueueForExistingMessages.clear();
        tempQueueForNewMessages.clear();
    }

    private void pauseActor() {
        actorStatus.set(ActorStatus.PAUSED);
        logger.info("Paused actor {}, status {}", getName(), actorStatus.get());
    }

    private boolean interceptor(Object message) {
        if (actorStatus.get().isTerminating()) {
            deadLetterMessage(message);
            return false;
        } else if (!actorStatus.get().isPaused() || message instanceof Restart || message instanceof PoisonPill) {
            return true;
        } else {
            tempQueueForExistingMessages.add(message);
            return false;
        }
    }

    private void deadLetterMessage(Object message) {
        logger.info("Actor {} is terminating or terminated, dead letter is encounter for message {}", getName(), message);
    }

    private AbstractActor createNewActorInstance() {
        AbstractActor actorSupplied = actorSupplier.get();
        actorSupplied.setSelf(actorRef);
        return actorSupplied;
    }

    private void createReceiver(AbstractActor actor) {
        receiver = actor.createReceive();
    }

    private static class Init {
        private static final Init instance = new Init();

        public static Init getInstance() {
            return instance;
        }
    }

    private static class Restart {
        private final ErrorData errorData;

        public Restart(ErrorData errorData) {
            this.errorData = errorData;
        }

        public ErrorData getErrorData() {
            return errorData;
        }
    }
}