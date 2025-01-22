package com.atlassian.actor.utils;

import java.util.ArrayList;
import java.util.List;

public class ActorLifecycleCatcher {
    List<String> lifecycleEvents = new ArrayList<>();
    public static final String PRE_START = "preStart";
    public static final String POST_STOP = "postStop";
    public static final String PRE_RESTART = "preRestart";
    public static final String POST_RESTART = "postRestart";
    public static final String CREATE_RECEIVE = "createReceive";

    public void preStart() {
        lifecycleEvents.add(PRE_START);
        System.out.println(PRE_START);
    }

    public void postStop() {
        lifecycleEvents.add(POST_STOP);
        System.out.println(POST_STOP);
    }

    public void preRestart(Throwable reason, Object message) {
        lifecycleEvents.add(PRE_RESTART);
        System.out.println(PRE_RESTART);
    }

    public void postRestart(Throwable reason) {
        lifecycleEvents.add(POST_RESTART);
        System.out.println(POST_RESTART);
    }

    public void createReceive() {
        lifecycleEvents.add(CREATE_RECEIVE);
        System.out.println(CREATE_RECEIVE);
    }

    public List<String> getLifecycleEvents() {
        return lifecycleEvents;
    }

    public static List<String> getLifecycleEventsWithRestarts(int restartCount) {
        List<String> events = new ArrayList<>();
        events.add(PRE_START);
        events.add(CREATE_RECEIVE);
        for (int i = 0; i < restartCount; i++) {
            events.add(PRE_RESTART);
            events.add(CREATE_RECEIVE);
            events.add(POST_RESTART);
        }
        events.add(POST_STOP);
        return events;
    }
}
