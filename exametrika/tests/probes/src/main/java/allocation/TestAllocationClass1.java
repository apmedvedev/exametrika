package allocation;

/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class TestAllocationClass1 {
    private static int i = 0;

    public static void main(String[] args) throws Throwable {

        while (true) {
            i = test();
            Thread.sleep(1000);
            System.out.println("------------------" + i);
        }
    }

    public static int test() throws Throwable {
        test1();
        test2();
        test3();
        Thread.sleep(100);
        return i++;
    }

    private static Integer test1() {
        return new Integer(10);
    }

    private static Object test2() throws Throwable {
        return String.class.newInstance();
    }

    private static Object test3() throws Throwable {
        return Integer.class.getConstructor(int.class).newInstance(10);
    }
}
