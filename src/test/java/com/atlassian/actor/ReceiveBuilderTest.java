package com.atlassian.actor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceiveBuilderTest {
    private ReceiveBuilder receiveBuilder;
    private Receive receiver;

    @BeforeEach
    void setUp() {
        receiveBuilder = ReceiveBuilder.create();
    }

    @Test
    void matchAndBuild() {
        receiver = receiveBuilder.match(String.class, (m, r) -> {
        }).match(Integer.class, (m, r) -> {
        }).match(Long.class, (m, r) -> {
        }).matchAny((m, r) -> {
        }).build();
        assertEquals(4, receiveBuilder.matches.size());
    }
}