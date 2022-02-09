package com.exametrika.tests.instrument.instrumentors.data;

public class TestFieldSetClass {
    public long f1;
    public static int f2;
    public Object f3;
    public static Object f4;

    public long testPrimitive() {
        f1 = 1;
        return f1;
    }

    public int testStaticPrimitive() {
        f2 = 2;
        return f2;
    }

    public Object testObject() {
        f3 = "test3";
        return f3;
    }

    public Object testStaticObject() {
        f4 = "test4";
        return f4;
    }
}
