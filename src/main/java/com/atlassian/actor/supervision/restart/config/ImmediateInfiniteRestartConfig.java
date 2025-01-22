package com.atlassian.actor.supervision.restart.config;

public class ImmediateInfiniteRestartConfig implements RestartConfig {

    public ImmediateInfiniteRestartConfig() {
    }

    @Override
    public boolean shouldRestart(int restartCount) {
        return true;
    }
}
