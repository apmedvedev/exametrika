/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;


public class TestClass1 implements ITestClass1 {
    public Runnable runnable;

    @Override
    public Runnable execute(Runnable runnable) {
        this.runnable = runnable;
        return runnable;
    }

    @Override
    public void execute() {
    }
}