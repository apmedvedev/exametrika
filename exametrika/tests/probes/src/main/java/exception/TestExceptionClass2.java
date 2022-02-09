package exception;


/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class TestExceptionClass2 {
    public static void main(String[] args) throws Throwable {
        System.out.println("---------------------------------- Main ");
        TestExceptionClass2 test = new TestExceptionClass2();
        test.run();
    }

    public void run() throws Throwable {
        test2();
    }

    private void test2() throws Throwable {
        for (int i = 0; i < 1000; i++) {
            System.out.println("----------------------------------");
            long t = System.currentTimeMillis();
            for (int k = 0; k < 10; k++) {
                try {
                    test(i * 10 + k);
                } catch (Exception e) {
                }
            }
            t = System.currentTimeMillis() - t;
            Thread.sleep(1000);
            System.out.println("Instrumented pass " + i + ":" + t);
        }
    }

    private void test(int i) {
        throw new CException(i);
    }
}
