package com.atlassian.actor;

import com.atlassian.actor.config.ActorCreationConfig;
import com.atlassian.actor.model.ActorCell;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.PoisonPill;
import com.atlassian.actor.supervision.ActorFailureHandler;
import com.atlassian.actor.exceptions.ActorNameExistsException;
import com.atlassian.actor.supervision.strategy.RootSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * ActorCore is the base class for all actors or ActorSystem.
 * It provides the basic functionality of creating, terminating, pausing and restarting child actors.
 */
public abstract class ActorCore {

    private final String name;
    @VisibleForTesting
    protected final ConcurrentHashMap<String, ActorCell> actors = new ConcurrentHashMap<>();

    @VisibleForTesting
    protected final AtomicBoolean isTerminating = new AtomicBoolean(false);
    protected Logger logger = LoggerFactory.getLogger(ActorCore.class);

    protected ActorCore(String name) {
        this.name = name;
    }

    public abstract ActorFailureHandler getActorFailureHandler();

    public abstract ActorCore getParentActorCore();

    public String getName() {
        return name;
    }

    public ActorRef createActor(Supplier<AbstractActor> actorSupplier, ActorCreationConfig actorCreationConfig) {
        if (actors.containsKey(actorCreationConfig.getName())) {
            throw new ActorNameExistsException("This actor name " + actorCreationConfig.getName() + " already exists in this system");
        }
        InternalActor actorCore = InternalActor.create(actorSupplier, actorCreationConfig);
        actors.put(actorCore.getName(), new ActorCell(actorCore, false));
        logger.info("New Actor {} is created under parent actor {}, total children now is {} ", actorCore.getName(), getName(), actors.size());
        return actorCore.getActorRef();
    }

    public void terminateAllChildren() {
        if (!isTerminating.get()) {
            isTerminating.set(true);
            if (actors.isEmpty()) {
                logger.info("Actor {} has no children to terminate", getName());
                childrenTerminateCallback();
            } else {
                logger.info("Sending termination request to all children of actor {}", getName());
                actors.values().forEach(actorCell -> {
                    if (!actorCell.getActor().getActorRef().isTerminating()) {
                        actorCell.getActor().getActorRef().tell(PoisonPill.getInstance());
                    }
                    actors.put(actorCell.getActor().getActorRef().getName(), new ActorCell(actorCell.getActor(), true));
                });
            }
        }
    }

    public synchronized void terminated(ActorRef actorRef) {
        if (actors.containsKey(actorRef.getName())) {
            actors.remove(actorRef.getName());
            logger.info("{} children are removed from parent actor {}, isTerminating {}, remaining children to be terminated {}", actorRef.getName(), name, isTerminating.get(), actors.size());

            if (isTerminating.get() && actors.isEmpty()) {
                childrenTerminateCallback();
            }
        }
    }

    public void pauseAllChildren() {
        actors.values().forEach(actorCell -> actorCell.getActor().pause());
    }

    public void restartAllChildren(ErrorData errorData) {
        actors.values().forEach(actorCell -> actorCell.getActor().restart(errorData));
    }

    public ActorRef getChildByName(String name) {
        if (actors.containsKey(name)) {
            return actors.get(name).getActor().getActorRef();
        } else {
            return null;
        }
    }

    public SupervisorStrategy getParentSupervisorStrategy() {
        return this.getParentActorCore() != null
                ? this.getParentActorCore().getSupervisorStrategy()
                : RootSupervisorStrategy.getInstance();
    }

    public abstract void restart(ErrorData errorData);

    public abstract void terminate();

    /**
     * Pause the actor or actor system.
     */
    public abstract void pause();

    public abstract boolean isRoot();

    public abstract SupervisorStrategy getSupervisorStrategy();

    protected abstract void childrenTerminateCallback();
}