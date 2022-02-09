package com.exametrika.tests.instrument.instrumentors.data;

public class TestArraySetClass {
    public long[] testSingle() {
        long[] array = new long[10];
        array[1] = 123L;

        return array;
    }

    public int[][] testMulti() {
        int[][] array = new int[10][];
        array[1] = new int[10];
        array[1][1] = 123;

        return array;
    }

    public String[] testObjectSingle() {
        String[] array = new String[10];
        array[1] = "test";

        return array;
    }

    public String[][] testObjectMulti() {
        String[][] array = new String[10][];
        array[1] = new String[10];
        array[1][1] = "test";

        return array;
    }
}
