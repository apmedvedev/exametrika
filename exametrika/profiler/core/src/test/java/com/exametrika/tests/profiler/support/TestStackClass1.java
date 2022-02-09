/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.utils.Threads;
import com.exametrika.perftests.common.lz4.LZ4PerfTests;

public class TestStackClass1 implements ITestStackClass1 {
    public long i;

    public static void main(String[] args) {
        TestStackClass1 clazz = new TestStackClass1();
        clazz.testLZ4();
    }

    @Override
    public long get() {
        return i;
    }

    @Override
    public void run() {
        slow();
    }

    @Override
    public void stalled() {
        Threads.sleep(100000000000l);
    }

    @Override
    public void testLZ4() {
        for (int i = 0; i < 100; i++) {
            System.out.println("----------------------------------");
            new Benchmark(new Probe() {
                @Override
                public void runOnce() {
                    LZ4PerfTests tests = new LZ4PerfTests();
                    tests.testFastCompression();
                    tests.testHighCompression();
                }
            }, 1, 0).print("Instrumented pass " + i + ":");
        }
    }

    private void slow() {
        for (int i = 0; i < 1000; i++)
            normal();
        fast();
    }

    private void normal() {
        for (int i = 0; i < 5000; i++)
            fast();
        ultraFast();
    }

    private void fast() {
        for (int i = 0; i < 10000; i++)
            ultraFast();
    }

    private void ultraFast() {
        ultraFast1();
        ultraFast2();
        ultraFast3();
    }

    private void ultraFast1() {
        i++;
    }

    private void ultraFast2() {
        i++;
    }

    private void ultraFast3() {
        i++;
    }
}
