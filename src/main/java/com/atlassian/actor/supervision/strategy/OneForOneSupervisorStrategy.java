package com.atlassian.actor.supervision.strategy;

import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.restart.config.ImmediateInfiniteRestartConfig;
import com.atlassian.actor.supervision.restart.config.ImmediateMaxRestartConfig;
import com.atlassian.actor.supervision.restart.config.RestartConfig;

import java.util.function.Function;

public class OneForOneSupervisorStrategy implements SupervisorStrategy {
    private final RestartConfig restartConfig;
    private final Function<ErrorData, SupervisorStrategyDirective> handler;

    public OneForOneSupervisorStrategy(Function<ErrorData, SupervisorStrategyDirective> handler) {
        this.handler = handler;
        this.restartConfig = new ImmediateInfiniteRestartConfig();
    }

    public OneForOneSupervisorStrategy(int maxRetries, Function<ErrorData, SupervisorStrategyDirective> handler) {
        this.handler = handler;
        this.restartConfig = new ImmediateMaxRestartConfig(maxRetries);
    }

    // For normal SupervisorStrategy (without backoff) we don't use lastRestartCount.
    @Override
    public SupervisorStrategyDirective handle(Integer lastRestartCount, ErrorData errorData) {
        return handle(errorData);
    }

    @Override
    public SupervisorStrategyDirective handle(ErrorData errorData) {
        return handler.apply(errorData);
    }

    public RestartConfig getRestartConfig() {
        return restartConfig;
    }
}