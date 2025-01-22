package com.atlassian.actor.model;

import java.io.Serializable;

public class Pair<A, B> implements Serializable {
    public A first;
    public B second;

    @Override
    public String toString() {
        return "(" + first.toString() + ", " + second.toString() + ")";
    }

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public A component1() {
        return first;
    }

    public B component2() {
        return second;
    }
}