package com.exametrika.tests.instrument.instrumentors.data;

public class TestNewArrayClass {
    public int f1;

    public Object testNew(int param) {
        @SuppressWarnings("unused")
        Object[] o = new Object[10];
        f1 = 123;
        if (param == 0)
            return new Object[10][10];
        else
            return new long[10];
    }
}
