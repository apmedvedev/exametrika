package com.exametrika.tests.instrument.instrumentors.data;

import com.exametrika.common.utils.Assert;

@TestAnnotation1
public class TestInterceptClass1 extends TestInterceptClass2 implements ITestInterface3 {
    public static int f1;
    public int f2;
    public int f3;
    public int f4;
    public int f5;
    public int f6;
    public int f7;

    static {
        f1 = 123;
    }

    {
        f2 = 123;
    }

    public TestInterceptClass1(int p1, String p2) {
        Assert.notNull(p2);

        f3 = 123;
    }

    public static void testStatic(int p1, String p2) {
        Assert.notNull(p2);
        f1 = 321;
    }

    @TestAnnotation2
    public long test1(int p1, String p2) {
        Assert.notNull(p2);
        f4 = 123;

        return 123;
    }

    public String test2(int p1) {
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

    public static class InnerClass1 {
        public int f1;
        public int f2;

        public InnerClass1(String p1) {
            Assert.notNull(p1);
            f1 = 1234;
        }

        public void test(String p1) {
            Assert.notNull(p1);
            f2 = 1234;
        }
    }

    public class InnerClass2 {
        public int f1;
        public int f2;

        public InnerClass2(String p1) {
            Assert.notNull(p1);
            f1 = 1234;
        }

        public void test(String p1) {
            Assert.notNull(p1);
            f2 = 1234;
        }
    }
}
