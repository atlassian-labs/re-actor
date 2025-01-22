package com.atlassian.actor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The envelope latch which is meant to be used only once by one single thread.
 */
public class Responder {
    private final AtomicReference<Object> reference = new AtomicReference<>();
    private final AtomicBoolean waitCalled = new AtomicBoolean(false);
    private final AtomicBoolean setCalled = new AtomicBoolean(false);
    private final CountDownLatch latch;
    private static final long MAX_WAIT_TIME_IN_MILLIS = 60_000L;

    public Responder() {
        latch = new CountDownLatch(1);
    }

    /**
     * This method is supposed to be invoked only once by any thread. Any subsequent access by the same or any other thread,
     * will result in {@link IllegalStateException}
     * @param timeOut the time to wait for the object
     * @return the object
     * @throws Exception if an error occurs during the operation
     */
    public Object waitForObject(Long timeOut) throws Exception {
        if (waitCalled.compareAndSet(false, true)) {
            /*
                We do not want to hold the connection for infinitely.
                As of now control plane proxy sets a 10-second waiting time, but we ensure that it can't be set
                more than MAX_WAIT_TIME_IN_MILLIS.
             */
            long latchTimeout = (timeOut < 1) ? MAX_WAIT_TIME_IN_MILLIS : Math.min(timeOut, MAX_WAIT_TIME_IN_MILLIS);

            Exception exception = null;
            try {
                boolean successful = latch.await(latchTimeout, TimeUnit.MILLISECONDS);
                if (!successful) {
                    exception = new TimeoutException("Timed out after $latchTimeout");
                }
            } catch (InterruptedException ie) {
                exception = new RuntimeException(ie);
            }

            // if there's not been any exception but a null object is set, we treat this as an error
            if (exception == null && reference.get() == null) {
                exception = new Exception("Null object set on data latch");
            }

            // if there's been any exception so far, we throw that exception
            if (exception != null) {
                throw exception;
            }

            return reference.get();
        } else {
            throw new IllegalStateException("method should only be called once");
        }
    }

    /**
     * This method is supposed to be invoked only once by any thread. Any subsequent access by the same or any other thread,
     * will result in {@link IllegalStateException}
     * @param obj the object
     */
    public void setObject(Object obj) {
        if (setCalled.compareAndSet(false, true)) {
            reference.set(obj);
            latch.countDown();
        } else {
            throw new IllegalStateException("method should only be called once");
        }
    }

    public Object getObject() {
        return reference.get();
    }
}
