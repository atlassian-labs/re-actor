package com.atlassian.actor.supervision.strategy;

import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.restart.config.BackoffRestartConfig;

import java.util.function.BiFunction;

public class OneForOneBackoffSupervisorStrategy implements SupervisorStrategy {
    private final BackoffRestartConfig restartConfig;
    private final BiFunction<Integer, ErrorData, SupervisorStrategyDirective> biFunctionHandler;

    public OneForOneBackoffSupervisorStrategy(BackoffRestartConfig restartConfig, BiFunction<Integer, ErrorData, SupervisorStrategyDirective> handler) {
        this.biFunctionHandler = handler;
        this.restartConfig = restartConfig;
    }

    @Override
    public SupervisorStrategyDirective handle(Integer lastRestartCount, ErrorData data) {
        return biFunctionHandler.apply(lastRestartCount, data);
    }

    // For BackoffSupervisorStrategy, lastRestartCount has to be passed, so this method is unsupported.
    @Override
    public SupervisorStrategyDirective handle(ErrorData data) {
        throw new UnsupportedOperationException("This method is not supported for OneForOneBackoffSupervisorStrategy");
    }

    public BackoffRestartConfig getRestartConfig() {
        return restartConfig;
    }

}