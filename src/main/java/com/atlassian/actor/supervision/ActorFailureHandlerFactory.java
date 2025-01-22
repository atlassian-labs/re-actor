package com.atlassian.actor.supervision;

import com.atlassian.actor.ActorCore;
import com.atlassian.actor.supervision.strategy.OneForOneBackoffSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.OneForOneSupervisorStrategy;
import com.atlassian.actor.supervision.strategy.SupervisorStrategy;

public class ActorFailureHandlerFactory {

    private ActorFailureHandlerFactory() {
    }

    public static ActorFailureHandler create(ActorCore actorCore) {
        SupervisorStrategy parentSupervisorStrategy = actorCore.getParentSupervisorStrategy();
        if (parentSupervisorStrategy instanceof OneForOneSupervisorStrategy) {
            return OneForOneActorFailureHandler.create(actorCore, (OneForOneSupervisorStrategy) parentSupervisorStrategy);
        } else if (parentSupervisorStrategy instanceof OneForOneBackoffSupervisorStrategy) {
            return OneForOneBackoffActorFailureHandler.create(actorCore, (OneForOneBackoffSupervisorStrategy) parentSupervisorStrategy);
        } else {
            throw new IllegalArgumentException("Unknown supervisor strategy: " + parentSupervisorStrategy);
        }
    }
}
