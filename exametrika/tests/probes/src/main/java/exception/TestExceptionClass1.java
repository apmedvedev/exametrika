package exception;


/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class TestExceptionClass1 {
    private int i;

    public static void main(String[] args) throws Throwable {
        System.out.println("---------------------------------- Main ");
        TestExceptionClass1 test = new TestExceptionClass1();
        test.run();
    }

    public void run() throws Throwable {
        test1();
        test2();
    }

    private void test1() throws Throwable {
        for (i = 0; i < 1000; i++) {
            System.out.println("----------------------------------");
            long t = System.currentTimeMillis();
            //for (int i = 0; i < 100000; i++)
            {
                slow1();
                slow2();
                slow3();
                slow4();
            }
            t = System.currentTimeMillis() - t;
            Thread.sleep(1000);
            System.out.println("Instrumented pass " + i + ":" + t);
        }
    }

    private void test2() throws Throwable {
        for (i = 0; i < 1000; i++) {
            System.out.println("----------------------------------");
            long t = System.currentTimeMillis();
            //for (int i = 0; i < 100000; i++)
            {
                try {
                    test(0);
                } catch (Exception e) {
                }
            }
            t = System.currentTimeMillis() - t;
            Thread.sleep(1000);
            System.out.println("Instrumented pass " + i + ":" + t);
        }
    }

    private void slow1() {
        try {
            normal(0);
        } catch (Throwable e) {
        }
    }

    private void slow2() {
        try {
            normal(1);
        } catch (Throwable e) {
        }
    }

    private void slow3() {
        try {
            normal(2);
        } catch (Throwable e) {
        }
    }

    private void slow4() {
        try {
            normal(3);
        } catch (Throwable e) {
        }
    }

    private void normal(int index) {
        try {
            normal2(index);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void normal2(int index) {
        fast(index);
    }

    private void fast(int index) {
        switch (index) {
            case 0:
                throw new NullPointerException("Test null pointer exception " + i);
            case 1:
                throw new AssertionError("Test assertion error " + i);
            case 2:
                throw new CException(i);
            case 3:
                throw new BException("Test B exception " + i);
        }
    }

    private void test(int level) {
        if (level == 100)
            throw new CException(i);
        else
            test(level + 1);
    }
}
