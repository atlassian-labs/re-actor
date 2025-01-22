package com.atlassian.actor.supervision.strategy;

import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;

/**
 * SupervisorStrategy is a strategy that is used to handle failures in a reactor while processing messages.
 */
public interface SupervisorStrategy {

    SupervisorStrategyDirective handle(Integer lastRestartCount, ErrorData data);

    SupervisorStrategyDirective handle(ErrorData data);
}