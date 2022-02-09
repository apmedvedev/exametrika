/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.common.utils.Times;


public class TestStackClass3 implements Runnable {
    public static void main(String[] args) {
        System.out.println("---------------------------------- Main ");
        TestStackClass3 test = new TestStackClass3();
        test.run();
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            System.out.println("----------------------------------");
            long t = System.currentTimeMillis();
            slow1();
            slow2();
            t = System.currentTimeMillis() - t;
            System.out.println("Instrumented pass " + i + ":" + t);
        }
    }

    private void slow1() {
        for (int i = 0; i < 1000; i++)
            normal();
    }

    private void slow2() {
        for (int i = 0; i < 2000; i++)
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
        Times.getTickCount();
    }

    private void delay2() {
        Times.getTickCount();
        Times.getTickCount();
    }
}
