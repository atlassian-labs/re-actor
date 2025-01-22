package com.atlassian.actor.model;

/**
 * An enum class that represents the different directives that can be given to the supervisor strategy.
 */
public enum SupervisorStrategyDirective {
    STOP, RESUME, RESTART, ESCALATE
}