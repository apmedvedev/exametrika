package com.exametrika.tests.instrument.instrumentors.data;

public class TestFieldGetClass {
    public long f1 = 1;
    public static int f2 = 2;
    public Object f3 = "test3";
    public static Object f4 = "test4";

    public long testPrimitive() {
        return f1;
    }

    public int testStaticPrimitive() {
        return f2;
    }

    public Object testObject() {
        return f3;
    }

    public Object testStaticObject() {
        return f4;
    }
}
