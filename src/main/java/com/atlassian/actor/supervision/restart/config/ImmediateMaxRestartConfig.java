package com.atlassian.actor.supervision.restart.config;

public class ImmediateMaxRestartConfig implements RestartConfig {

    private final int maxRetries;

    public ImmediateMaxRestartConfig(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean shouldRestart(int restartCount) {
        return restartCount <= maxRetries;
    }
}
