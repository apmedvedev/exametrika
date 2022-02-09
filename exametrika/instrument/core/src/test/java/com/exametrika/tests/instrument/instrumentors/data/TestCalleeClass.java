package com.exametrika.tests.instrument.instrumentors.data;

import com.exametrika.common.utils.Assert;

public class TestCalleeClass {
    public static int f1;
    public int f2;
    public int f3;
    public int f4;
    public int f5;
    public int f6;
    public int f7;

    public TestCalleeClass(int p1, String p2) {
        Assert.notNull(p2);

        f3 = 123;
    }

    public static void testStatic(int p1, String p2) {
        Assert.notNull(p2);
        f1 = 123;
    }

    public long test1(int p1, String p2) {
        Assert.notNull(p2);
        f4 = 123;

        return 123;
    }

    public String test2(long p1) {
        try {
            if (p1 == 0)
                throw new RuntimeException("test exception2");

            f4 = 123;

            return "return2";
        } catch (RuntimeException e) {
            f6 = 123;
            throw e;
        } finally {
            f5 = 123;
        }
    }

    public void test3() {
        f7 = 123;
    }
}
