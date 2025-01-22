package com.atlassian.actor.supervision.restart.config;

import java.time.Duration;

public class FixedBackoffRestartConfig implements BackoffRestartConfig {

    private final int maxRetries;
    private final Duration minBackoff;
    private final Duration maxBackoff;
    private final Duration interval;
    private final Duration autoResetDuration;

    public FixedBackoffRestartConfig(int maxRetries, Duration minBackoff, Duration maxBackoff, Duration interval, Duration autoResetDuration) {
        this.maxRetries = maxRetries;
        this.minBackoff = minBackoff;
        this.maxBackoff = maxBackoff;
        this.interval = interval;
        this.autoResetDuration = autoResetDuration;
    }

    public FixedBackoffRestartConfig(int maxRetries, Duration minBackoff, Duration maxBackoff, Duration interval) {
        this.maxRetries = maxRetries;
        this.minBackoff = minBackoff;
        this.maxBackoff = maxBackoff;
        this.interval = interval;
        this.autoResetDuration = maxBackoff;
    }

    @Override
    public Duration backoffInterval(int restartCount) {
        return Duration.ofMillis(Math.min(maxBackoff.toMillis(), minBackoff.toMillis() + interval.toMillis() * (restartCount - 1)));
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
