package com.atlassian.actor.supervision.strategy;

import com.atlassian.actor.model.SupervisorStrategyDirective;

/**
 * RootSupervisorStrategy is a strategy that is applied to the root actor(i.e. ActorSystem).
 * In case when ActorSystem supervising child actor ESCALATE, this RootSupervisorStrategy kicks in.
 * This would apply STOP supervision at ActorSystem, then ActorSystem would be terminated along with its child actors.
 */
public class RootSupervisorStrategy {

    private static final OneForOneSupervisorStrategy INSTANCE =
            new OneForOneSupervisorStrategy(t -> SupervisorStrategyDirective.STOP);

    private RootSupervisorStrategy() {
    }

    public static OneForOneSupervisorStrategy getInstance() {
        return INSTANCE;
    }
}