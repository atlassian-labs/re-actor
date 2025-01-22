package com.atlassian.actor.examples;

import com.atlassian.actor.config.ActorConfig;
import com.atlassian.actor.config.ActorSystemConfig;
import com.atlassian.actor.examples.actors.DemoActor;
import com.atlassian.actor.ActorRef;
import com.atlassian.actor.ActorSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
class BasicActorTest {
    private static final Logger log = Logger.getLogger(BasicActorTest.class.getName());

    @Test
    void run() throws InterruptedException {
        log.info("Starting test in thread [" + Thread.currentThread().getName() + "]");
        Scheduler scheduler = Schedulers.boundedElastic();

        ActorSystem actorSystem = ActorSystem.create("ControlPlaneSystem", new ActorSystemConfig(scheduler));

        actorSystem.registerOnTermination(() -> log.info("actorSystem " + actorSystem.getName() + " is TERMINATED"));

        ActorRef actor = actorSystem.actorOf(
                DemoActor::new,
                new ActorConfig("DemoActor")
        );

        actor.tell(new DemoActor.Message("Hello message", 100L));
        actor.tell(999999);
        actor.tell("Hello message");
        actor.tell(1234.34442);

        for (int i = 0; i < 10; i++) {
            actor.tell(new DemoActor.Message("Hello message " + i, 100L));
            log.info("Published message - " + i);
        }

        log.info("Published all messages");
        Thread.sleep(3000);

        actorSystem.terminate();
    }

    @Test
    void test() {

        String fullUrl = "https://atlassian.signalfx.com/#/dashboard/GHg4LHuA4AA?groupId=F1UC9jFAwAA&configId=GHg4LTTAwAA"
                + "&variables[]=environment_type=environment_type:%s&variables[]=environment=environment:%s&variables[]=monitoring_id=monitoring_id:%s&startTimeUTC=%s&endTimeUTC=%s";
        String moniF = String.format(fullUrl,
                URLEncoder.encode("[\"staging\"]", StandardCharsets.UTF_8),
                URLEncoder.encode("[\"stg-east\"]", StandardCharsets.UTF_8),
                URLEncoder.encode("[\"558de145-eebb-4223-959b-fc7b154904f2\"]", StandardCharsets.UTF_8),
                Instant.now().toEpochMilli(),
                Instant.now().plusSeconds(10).toEpochMilli()
        );

        String fixedUrl = "https://atlassian.signalfx.com/#/dashboard/GHg4LHuA4AA?groupId=F1UC9jFAwAA&configId=GHg4LTTAwAA&";
        String queryUrl = "variables[]=environment_type=environment_type:[\"%s\"]&variables[]=environment=environment:[\"%s\"]&variables[]=monitoring_id=monitoring_id:[\"%s\"]"
                + "&startTimeUTC=%s&endTimeUTC=%s";
        String environmentType = "dev";

        String environmentTypeQueryValue = "environment_type=environment_type:[\"%s\"]";
        String environmentQueryValue = "environment=environment:[\"%s\"]";
        String monitoringIdQueryValue = "monitoring_id=monitoring_id:[\"%s\"]";

        String moni = String.format(queryUrl,
                "dev",
                "ddev",
                "bbc903c9-50a4-47bc-b2b3-d3917747aafa",
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli()
        );

        String encoded = URLEncoder.encode(moni, StandardCharsets.UTF_8);

        String finalUrl = fixedUrl + encoded;

        Instant now = Instant.now();
        long millis = now.toEpochMilli();
        log.info("Current time in millis: " + millis);
    }
}