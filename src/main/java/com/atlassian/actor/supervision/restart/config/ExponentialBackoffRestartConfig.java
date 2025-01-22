package com.atlassian.actor.supervision.restart.config;

import java.time.Duration;

public class ExponentialBackoffRestartConfig implements BackoffRestartConfig {

    private final int maxRetries;
    private final Duration minBackoff;
    private final Duration maxBackoff;
    private final Duration autoResetDuration;

    public ExponentialBackoffRestartConfig(int maxRetries, Duration minBackoff, Duration maxBackoff, Duration autoResetDuration) {
        this.maxRetries = maxRetries;
        this.minBackoff = minBackoff;
        this.maxBackoff = maxBackoff;
        this.autoResetDuration = autoResetDuration;
    }

    public ExponentialBackoffRestartConfig(int maxRetries, Duration minBackoff, Duration maxBackoff) {
        this.maxRetries = maxRetries;
        this.minBackoff = minBackoff;
        this.maxBackoff = maxBackoff;
        this.autoResetDuration = maxBackoff;
    }

    @Override
    public Duration backoffInterval(int restartCount) {
        return Duration.ofMillis(Math.min(maxBackoff.toMillis(), minBackoff.toMillis() * (long) Math.pow(2, (restartCount - 1))));
    }

    @Override
    public boolean shouldRestart(int restartCount) {
        return restartCount <= maxRetries;
    }

    @Override
    public Duration getResetInterval() {
        return autoResetDuration;
    }

}