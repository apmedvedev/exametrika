package com.exametrika.tests.instrument.instrumentors.data;


public class TestCallerClass {
    public TestCalleeClass callee;
    public int f1;
    public int f2;
    public int f3;
    public int f4;
    public int f5;
    public int f6;
    public int f7;
    public int f8;
    public int f9;

    public void testInit(int p1, String p2) {
        f1 = 123;
        callee = new TestCalleeClass(p1, p2);
        f2 = 123;
    }

    public void testStatic(int p1, String p2) {
        f3 = 123;
        TestCalleeClass.testStatic(p1, p2);
        f4 = 123;
    }

    public long test1(int p1, String p2) {
        f5 = 123;

        return callee.test1(p1, p2);
    }

    public String test2(long p1) {
        try {
            String res = callee.test2(p1);

            f6 = 123;

            return res;
        } catch (RuntimeException e) {
            f7 = 123;
            throw e;
        } finally {
            f8 = 123;
        }
    }

    public void test3() {
        callee.test3();
        f9 = 123;
    }
}
