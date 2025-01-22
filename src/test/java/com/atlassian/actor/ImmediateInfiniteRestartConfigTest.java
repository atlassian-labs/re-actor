package com.atlassian.actor;

import com.atlassian.actor.supervision.restart.config.ImmediateInfiniteRestartConfig;
import com.atlassian.actor.supervision.restart.config.RestartConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImmediateInfiniteRestartConfigTest {
    private RestartConfig restartConfig;

    @BeforeEach
    void setUp() {
        restartConfig = new ImmediateInfiniteRestartConfig();
    }

    @Test
    void shouldRetry() {
        assertTrue(restartConfig.shouldRestart(1));
        assertTrue(restartConfig.shouldRestart(1000000));
    }
}