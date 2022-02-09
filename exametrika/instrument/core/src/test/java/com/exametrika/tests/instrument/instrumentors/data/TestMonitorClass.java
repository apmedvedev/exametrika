package com.exametrika.tests.instrument.instrumentors.data;


public class TestMonitorClass {
    public int f4;

    public void test4(boolean b) {
        synchronized (this) {
            f4 = 123;

            if (b)
                throw new IllegalArgumentException();
        }
    }
}
