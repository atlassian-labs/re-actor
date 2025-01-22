package com.atlassian.actor.supervision;

import com.atlassian.actor.ActorCore;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * OneForOneActorFailureHandler is the implementation of ActorFailureHandler for OneForOneSupervisorStrategy.
 * It provides the implementation of handling exceptions thrown while processing messages in actor.
 */
public class OneForOneActorFailureHandler extends ActorFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(OneForOneActorFailureHandler.class);
    private final ConcurrentHashMap<String, Integer> restartCountMap = new ConcurrentHashMap<>();
    private final OneForOneSupervisorStrategy parentSupervisorStrategy; // supervisorStrategy of parent actor

    private OneForOneActorFailureHandler(ActorCore actorCore, OneForOneSupervisorStrategy parentSupervisorStrategy) {
        super(actorCore, parentSupervisorStrategy);
        this.parentSupervisorStrategy = parentSupervisorStrategy;
    }

    public static OneForOneActorFailureHandler create(ActorCore actorCore, OneForOneSupervisorStrategy parentSupervisorStrategy) {
        return new OneForOneActorFailureHandler(actorCore, parentSupervisorStrategy);
    }

    @Override
    protected void refreshRestartHistory(ErrorData e) {
        // do nothing
    }

    @Override
    protected int lastRestartCount(ErrorData e) {
        String error = e.getError().toString();
        return restartCountMap.get(error) == null ? 0 : restartCountMap.get(error);
    }

    @Override
    protected void updateRestartHistory(ErrorData e) {
        log.warn("Actor got restarted till now {}", lastRestartCount(e));
        restartCountMap.put(e.getError().toString(), currentRestartCount(e));
    }

    @Override
    protected SupervisorStrategyDirective getSupervisorDirective(ErrorData errorData) {
        SupervisorStrategyDirective directive = parentSupervisorStrategy.handle(errorData);
        if (directive == SupervisorStrategyDirective.RESTART) {
            boolean shouldRestart = parentSupervisorStrategy.getRestartConfig().shouldRestart(currentRestartCount(errorData));
            return shouldRestart ? directive : SupervisorStrategyDirective.STOP;
        } else {
            return directive;
        }
    }

    /**
     * This method is used to restart the actor without any backoff interval.
     * During restart actor is paused &amp; it keeps incoming messages in queue until actor is restarted.
     *
     * @param errorData {@link ErrorData} - error data
     */
    @Override
    protected void restart(ErrorData errorData) {
        log.info("Restarting actor {} due to error in errorActor {} with exception {} ", actorCore.getName(), errorData.getActorRef().getName(), errorData.getError().toString());
        actorCore.pause();
        updateRestartHistory(errorData);
        actorCore.restart(errorData);
    }
}