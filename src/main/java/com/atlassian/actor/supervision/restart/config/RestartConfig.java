package com.atlassian.actor.supervision.restart.config;

public interface RestartConfig {

    boolean shouldRestart(int restartCount);
}