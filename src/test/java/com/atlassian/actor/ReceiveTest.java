package com.atlassian.actor;

import com.atlassian.actor.model.MatchTuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceiveTest {
    @Mock
    private ActorConsumer<Object> stringConsumer;
    @Mock
    private ActorConsumer<Object> integerConsumer;
    private Receive receiver;

    @BeforeEach
    void setUp() throws Exception {
        doNothing().when(stringConsumer).accept(any(), ArgumentMatchers.any(Responder.class));
        doNothing().when(integerConsumer).accept(any(), ArgumentMatchers.any(Responder.class));

        List<MatchTuple> matchTuples = new ArrayList<>();
        matchTuples.add(new MatchTuple(String.class::isInstance, stringConsumer));
        matchTuples.add(new MatchTuple(Integer.class::isInstance, integerConsumer));
        receiver = new Receive(matchTuples);
    }

    @Test
    void process() throws Exception {
        String stringMessage = "stringMessage";
        Integer integerMessage = 123;
        Responder responder = new Responder();
        receiver.process(stringMessage, responder);
        receiver.process(integerMessage, null);

        verify(stringConsumer, times(1)).accept(stringMessage, responder);
        verify(integerConsumer, times(1)).accept(integerMessage, null);
    }

    @Test
    void defaultProcess() throws Exception {
        Long longMessage = 100L;
        receiver.process(longMessage, null);
        verify(stringConsumer, times(0)).accept(any(), eq(null));
    }
}