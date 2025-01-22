package com.atlassian.actor.supervision.restart.config;

import java.time.Duration;

public interface BackoffRestartConfig {

    Duration backoffInterval(int restartCount);

    boolean shouldRestart(int restartCount);

    Duration getResetInterval();
}