package com.exametrika.tests.instrument.instrumentors.data;

@SuppressWarnings("unused")
public class TestArrayGetClass {
    public long[] testSingle() {
        long[] array = new long[10];
        array[1] = 123L;

        long l = array[1];

        return array;
    }

    public int[][] testMulti() {
        int[][] array = new int[10][];
        array[1] = new int[10];
        array[1][1] = 123;

        int l = array[1][1];

        return array;
    }

    public String[] testObjectSingle() {
        String[] array = new String[10];
        array[1] = "test";

        String l = array[1];

        return array;
    }

    public String[][] testObjectMulti() {
        String[][] array = new String[10][];
        array[1] = new String[10];
        array[1][1] = "test";

        String l = array[1][1];

        return array;
    }
}
