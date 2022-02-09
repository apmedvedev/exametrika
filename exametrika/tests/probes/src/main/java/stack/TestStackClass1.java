/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package stack;

import com.exametrika.common.utils.Profiler;


public class TestStackClass1 implements Runnable {
    private static final int COUNT = Profiler.getOverhead() < 200 ? 1000 : 10;

    public static void main(String[] args) {
        System.out.println("---------------------------------- Main ");
        TestStackClass1 test = new TestStackClass1();
        test.run();
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000000000; i++) {
            System.out.println("----------------------------------");
            long t = System.currentTimeMillis();
            slow1();
            slow2();
            t = System.currentTimeMillis() - t;
            System.out.println("Instrumented pass " + i + ":" + t);
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
}
