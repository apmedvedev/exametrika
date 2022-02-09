/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package stack;


public class TestStackClass3 {
    public static void main(String[] args) throws Throwable {
        System.out.println("---------------------------------- Main ");
        TestStackClass3 test = new TestStackClass3();
        test.run();
    }

    public void run() throws Throwable {
        for (int i = 0; i < 1000000000; i++) {
            System.out.println("----------------------------------");
            Thread.sleep(1000);
            System.out.println("Instrumented pass " + i);
        }
    }
}
