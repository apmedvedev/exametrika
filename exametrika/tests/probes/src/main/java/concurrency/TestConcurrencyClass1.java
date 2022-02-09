/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package concurrency;

import com.exametrika.common.utils.Profiler;


public class TestConcurrencyClass1 implements Runnable {
    private static final int COUNT = Profiler.getOverhead() < 200 ? 1000 : 10;

    public static void main(String[] args) throws Throwable {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new TestConcurrencyClass1(), "thread" + i);
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++)
            threads[i].join();
    }

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            long t = System.currentTimeMillis();
            slow1();
            slow2();
            t = System.currentTimeMillis() - t;
            StringBuilder builder = new StringBuilder("---------------- " + Thread.currentThread().getName() + ":" + i + ": " + t);
            System.out.println(builder);
        }
    }

    private void slow1() {
        for (int i = 0; i < COUNT; i++)
            normal();
    }

    private void slow2() {
        for (int i = 0; i < 2 * COUNT; i++)
            normal();
    }

    private void normal() {
        for (int i = 0; i < 50; i++)
            normal2();

        exception();
    }

    private void normal2() {
        for (int i = 0; i < 20; i++)
            fast();
    }

    private void fast() {
        for (int i = 0; i < 15; i++)
            delay1();
        for (int i = 0; i < 15; i++)
            delay2();
    }

    private void delay1() {
        Profiler.rdtsc();
    }

    private void delay2() {
        Profiler.rdtsc();
        Profiler.rdtsc();
    }

    private void exception() {
        try {
            exception2();
        } catch (Throwable e) {
        }
    }

    private void exception2() {
        throw new RuntimeException("Test exception.");
    }
}
