/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link TestProbeInterceptor} represents a static interceptor of thread pool executors probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class TestProbeInterceptor extends StaticInterceptor {
    public static Runnable onExecute(int index, int version, Object instance) {
        return new TestRunnable((Runnable) instance);
    }

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        return "hello";
    }

    public static InputStream onReturnExit(Object instance, Object param) {
        assertTrue(param.equals("hello"));
        return new TestInputStream((Runnable) instance);
    }

    public static class TestInputStream extends InputStream {
        public final Runnable runnable;

        public TestInputStream(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public int read() throws IOException {
            return 0;
        }
    }

    public static class TestRunnable implements Runnable {
        public final Runnable runnable;

        public TestRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
