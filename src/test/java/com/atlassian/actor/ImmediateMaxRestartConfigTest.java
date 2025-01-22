package com.atlassian.actor;

import com.atlassian.actor.supervision.restart.config.ImmediateMaxRestartConfig;
import com.atlassian.actor.supervision.restart.config.RestartConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImmediateMaxRestartConfigTest {
    private RestartConfig restartConfig;

    @BeforeEach
    void setUp() {
        restartConfig = new ImmediateMaxRestartConfig(3);
    }

    @Test
    void shouldRetry() {
        assertTrue(restartConfig.shouldRestart(1));
        assertTrue(restartConfig.shouldRestart(3));
        assertFalse(restartConfig.shouldRestart(4));
    }
}