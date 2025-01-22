package com.atlassian.actor;

import com.atlassian.actor.supervision.restart.config.BackoffRestartConfig;
import com.atlassian.actor.supervision.restart.config.FixedBackoffRestartConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FixedBackoffRestartConfigTest {
    private BackoffRestartConfig restartConfig;

    @BeforeEach
    void setUp() {
        restartConfig = new FixedBackoffRestartConfig(6, Duration.ofMillis(100),
                Duration.ofMillis(500),
                Duration.ofMillis(100));
    }

    @Test
    void shouldRetry() {
        assertTrue(restartConfig.shouldRestart(1));
        assertTrue(restartConfig.shouldRestart(6));
        assertFalse(restartConfig.shouldRestart(7));
    }

    @Test
    void waitInterval() {
        assertEquals(100, restartConfig.backoffInterval(1).toMillis());
        assertEquals(200, restartConfig.backoffInterval(2).toMillis());
        assertEquals(300, restartConfig.backoffInterval(3).toMillis());
        assertEquals(400, restartConfig.backoffInterval(4).toMillis());
        assertEquals(500, restartConfig.backoffInterval(5).toMillis());
        assertEquals(500, restartConfig.backoffInterval(6).toMillis());
    }
}