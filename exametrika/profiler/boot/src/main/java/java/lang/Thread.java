/**
 * Copyright 2012 Andrey Medvedev. All rights reserved.
 */
package java.lang;


/**
 * Dummy class for compilation of fast thread local accessors.
 *
 * @author medvedev
 */
public class Thread {
    public Object _exaTls;

    public static Thread currentThread() {
        return null;
    }

    public long getId() {
        return 0;
    }

    public String getName() {
        return null;
    }

    public boolean isAlive() {
        return false;
    }

    public boolean isDaemon() {
        return false;
    }

    public boolean isInterrupted() {
        return false;
    }

    public StackTraceElement[] getStackTrace() {
        return null;
    }

    public void run() {
    }

    public void suspend() {
    }

    public void resume() {
    }

    public static void sleep(long time) throws InterruptedException {
    }
}
