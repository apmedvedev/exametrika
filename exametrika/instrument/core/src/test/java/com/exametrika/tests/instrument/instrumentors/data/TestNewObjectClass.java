package com.exametrika.tests.instrument.instrumentors.data;

public class TestNewObjectClass {
    public int f1;

    public Object testNew(int param) {
        f1 = 123;
        if (param == 0)
            return new Object();
        else
            return new Integer(10);
    }
}
