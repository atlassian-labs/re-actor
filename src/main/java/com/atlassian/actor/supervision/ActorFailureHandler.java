package com.atlassian.actor.supervision;

import com.atlassian.actor.ActorCore;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ActorFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(ActorFailureHandler.class);

    private final SupervisorStrategy parentSupervisorStrategy;
    protected final ActorCore actorCore;

    protected ActorFailureHandler(ActorCore actorCore, SupervisorStrategy parentSupervisorStrategy) {
        this.actorCore = actorCore;
        this.parentSupervisorStrategy = parentSupervisorStrategy;
    }

    protected abstract void refreshRestartHistory(ErrorData e);

    protected abstract int lastRestartCount(ErrorData e);

    protected abstract void updateRestartHistory(ErrorData e);

    protected abstract SupervisorStrategyDirective getSupervisorDirective(ErrorData errorData);

    /**
     * This method is used to restart the actor.
     * @param errorData {@link ErrorData} - error data
     */
    protected abstract void restart(ErrorData errorData);

    public void handle(ErrorData errorData) {
        log.error("Actor {} handling error {}, in actor {}", actorCore.getName(), errorData.getError().getMessage(),
                errorData.getActorRef().getName(), errorData.getError());
        refreshRestartHistory(errorData);
        switch (getSupervisorDirective(errorData)) {
            case RESUME:
                resume(errorData);
                break;
            case STOP:
                stop(errorData);
                break;
            case RESTART:
                restart(errorData);
                break;
            case ESCALATE:
                // actorCore.getParentActorCore() won't be null here, as for ActorSystem directive won't be ESCALATE, it would be STOP
                if (actorCore.getParentActorCore() != null) {
                    ActorFailureHandler parentActorFailureHandler = actorCore.getParentActorCore().getActorFailureHandler();
                    parentActorFailureHandler.handle(errorData);
                } else {
                    log.error("Parent actor is null for actor {}, errorActor {}", actorCore.getName(), errorData.getActorRef().getName());
                }
                break;
            default:
                break;
        }
    }

    /**
     * This method is used to stop the actor.
     * @param errorData {@link ErrorData} - error data
     */
    private void stop(ErrorData errorData) {
        log.info("Stopping actor {} due to error in errorActor {} with exception {} ", actorCore.getName(), errorData.getActorRef().getName(), errorData.getError().toString());
        actorCore.pause();
        actorCore.terminate();
    }

    /**
     * This method is used to resume the actor. It doesn't do anything.
     * @param errorData {@link ErrorData} - error data
     */
    private void resume(ErrorData errorData) {
        log.info("Resuming after exception {}", errorData.getError().toString());
    }

    protected int currentRestartCount(ErrorData e) {
        return lastRestartCount(e) + 1;
    }
}