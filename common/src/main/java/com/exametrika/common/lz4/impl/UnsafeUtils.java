/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import static java.lang.Integer.reverseBytes;

import java.nio.ByteOrder;

import sun.misc.Unsafe;

import com.exametrika.common.utils.Classes;

public final class UnsafeUtils {
    private static final Unsafe UNSAFE = Classes.getUnsafe();
    private static final long BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    private static final int BYTE_ARRAY_SCALE = UNSAFE.arrayIndexScale(byte[].class);
    private static final long INT_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
    private static final int INT_ARRAY_SCALE = UNSAFE.arrayIndexScale(int[].class);
    private static final long SHORT_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(short[].class);
    private static final int SHORT_ARRAY_SCALE = UNSAFE.arrayIndexScale(short[].class);

    public static byte readByte(byte[] src, int srcOff) {
        return UNSAFE.getByte(src, BYTE_ARRAY_OFFSET + BYTE_ARRAY_SCALE * srcOff);
    }

    public static void writeByte(byte[] src, int srcOff, byte value) {
        UNSAFE.putByte(src, BYTE_ARRAY_OFFSET + BYTE_ARRAY_SCALE * srcOff, value);
    }

    public static void writeByte(byte[] src, int srcOff, int value) {
        writeByte(src, srcOff, (byte) value);
    }

    public static long readLong(byte[] src, int srcOff) {
        return UNSAFE.getLong(src, BYTE_ARRAY_OFFSET + srcOff);
    }

    public static void writeLong(byte[] dest, int destOff, long value) {
        UNSAFE.putLong(dest, BYTE_ARRAY_OFFSET + destOff, value);
    }

    public static int readInt(byte[] src, int srcOff) {
        return UNSAFE.getInt(src, BYTE_ARRAY_OFFSET + srcOff);
    }

    public static int readIntLE(byte[] src, int srcOff) {
        int i = readInt(src, srcOff);
        if (Utils.NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN)
            i = reverseBytes(i);

        return i;
    }

    public static void writeInt(byte[] dest, int destOff, int value) {
        UNSAFE.putInt(dest, BYTE_ARRAY_OFFSET + destOff, value);
    }

    public static short readShort(byte[] src, int srcOff) {
        return UNSAFE.getShort(src, BYTE_ARRAY_OFFSET + srcOff);
    }

    public static void writeShort(byte[] dest, int destOff, short value) {
        UNSAFE.putShort(dest, BYTE_ARRAY_OFFSET + destOff, value);
    }

    public static int readInt(int[] src, int srcOff) {
        return UNSAFE.getInt(src, INT_ARRAY_OFFSET + INT_ARRAY_SCALE * srcOff);
    }

    public static void writeInt(int[] dest, int destOff, int value) {
        UNSAFE.putInt(dest, INT_ARRAY_OFFSET + INT_ARRAY_SCALE * destOff, value);
    }

    public static int readShort(short[] src, int srcOff) {
        return UNSAFE.getShort(src, SHORT_ARRAY_OFFSET + SHORT_ARRAY_SCALE * srcOff) & 0xFFFF;
    }

    public static void writeShort(short[] dest, int destOff, int value) {
        UNSAFE.putShort(dest, SHORT_ARRAY_OFFSET + SHORT_ARRAY_SCALE * destOff, (short) value);
    }

    private UnsafeUtils() {
    }
}
