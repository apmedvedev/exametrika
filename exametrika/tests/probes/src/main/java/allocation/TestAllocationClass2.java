package allocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class TestAllocationClass2 {
    private static int i = 0;
    private static Deque<Object> queue = new ArrayDeque<Object>();
    private static Random random = new Random();

    public static void main(String[] args) throws Throwable {
        while (true) {
            test();
            Thread.sleep(1000);
            System.out.println("------------------" + i++);
        }
    }

    public static void test() throws Throwable {
        for (int i = 0; i < 10; i++)
            queue.push(new byte[random.nextInt(1000000)]);

        if (queue.size() == 1000) {
            int count = random.nextInt(500);
            for (int i = 0; i < count; i++)
                queue.pop();
        }
    }
}
