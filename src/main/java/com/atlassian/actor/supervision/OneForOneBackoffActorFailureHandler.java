package com.atlassian.actor.supervision;

import com.atlassian.actor.ActorCore;
import com.atlassian.actor.model.ErrorData;
import com.atlassian.actor.model.SupervisorStrategyDirective;
import com.atlassian.actor.supervision.strategy.OneForOneBackoffSupervisorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OneForOneBackoffActorFailureHandler is the implementation of ActorFailureHandler for OneForOneBackoffSuperVisorStrategy.
 * It provides the implementation of handling exceptions thrown while processing messages in actor.
 */
public class OneForOneBackoffActorFailureHandler extends ActorFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(OneForOneBackoffActorFailureHandler.class);
    private final ConcurrentHashMap<String, RestartHistory> restartCountMap = new ConcurrentHashMap<>();

    private final OneForOneBackoffSupervisorStrategy parentSupervisorStrategy; // supervisorStrategy of parent actor

    private OneForOneBackoffActorFailureHandler(ActorCore actorCore, OneForOneBackoffSupervisorStrategy parentSupervisorStrategy) {
        super(actorCore, parentSupervisorStrategy);
        this.parentSupervisorStrategy = parentSupervisorStrategy;
    }

    public static OneForOneBackoffActorFailureHandler create(ActorCore actorCore, OneForOneBackoffSupervisorStrategy parentSupervisorStrategy) {
        return new OneForOneBackoffActorFailureHandler(actorCore, parentSupervisorStrategy);
    }

    @Override
    protected void refreshRestartHistory(ErrorData e) {
        RestartHistory restartHistory = restartCountMap.get(e.getError().getClass().toString());
        if (restartHistory != null) {
            Duration durationSinceLastRestart = Duration.between(restartHistory.getLastRestartTime(), Instant.now());
            log.info("Checking lastRestartTime {}, interval {}, for actor {}, errorActor {}", restartHistory.getLastRestartTime(),
                    durationSinceLastRestart.toMillis(), actorCore.getName(), e.getActorRef().getName());
            if (durationSinceLastRestart.toMillis() > parentSupervisorStrategy.getRestartConfig().getResetInterval().toMillis()) {
                log.info("Resetting restart count for actor {}, errorActor {}", actorCore.getName(), e.getActorRef().getName());
                restartCountMap.put(e.getError().getClass().toString(), new RestartHistory(0, Instant.now()));
            }
        }
    }

    @Override
    protected int lastRestartCount(ErrorData e) {
        String error = e.getError().getClass().toString();
        return restartCountMap.get(error) == null ? 0 : restartCountMap.get(error).getRestartCount();
    }

    @Override
    protected void updateRestartHistory(ErrorData e) {
        restartCountMap.put(e.getError().getClass().toString(), new RestartHistory(currentRestartCount(e), Instant.now()));
    }

    protected Duration backoffInterval(ErrorData errorData) {
        return parentSupervisorStrategy.getRestartConfig().backoffInterval(currentRestartCount(errorData));
    }

    @Override
    protected SupervisorStrategyDirective getSupervisorDirective(ErrorData errorData) {
        SupervisorStrategyDirective directive = parentSupervisorStrategy.handle(lastRestartCount(errorData), errorData);
        if (directive == SupervisorStrategyDirective.RESTART) {
            boolean shouldRestart = parentSupervisorStrategy.getRestartConfig().shouldRestart(currentRestartCount(errorData));
            return shouldRestart ? directive : SupervisorStrategyDirective.STOP;
        } else {
            return directive;
        }
    }

    /**
     * This method is used to restart the actor with backoff interval.
     * It also checks if max retries are exhausted or not. If yes, then it stops the actor.
     * During backoff interval actor is paused &amp; it keeps incoming messages in queue until actor is restarted.
     * @param errorData {@link ErrorData} - error data
     */
    @Override
    protected void restart(ErrorData errorData) {
        log.info("Restarting actor {} due to error in errorActor {} with exception {} ", actorCore.getName(), errorData.getActorRef().getName(), errorData.getError().toString());
        actorCore.pause();
        Duration waitInterval = backoffInterval(errorData);
        log.info("Waiting for {} ms before restarting actor {}, errorActor {}", waitInterval.toMillis(), actorCore.getName(), errorData.getActorRef().getName());
        Mono.delay(waitInterval)
                .map(ignored -> {
                    updateRestartHistory(errorData);
                    actorCore.restart(errorData);
                    return errorData;
                })
                .subscribe();
    }

    private static class RestartHistory {
        private final int restartCount;
        private final Instant lastRestartTime;

        public RestartHistory(int restartCount, Instant lastRestartTime) {
            this.restartCount = restartCount;
            this.lastRestartTime = lastRestartTime;
        }

        public int getRestartCount() {
            return restartCount;
        }

        public Instant getLastRestartTime() {
            return lastRestartTime;
        }
    }
}