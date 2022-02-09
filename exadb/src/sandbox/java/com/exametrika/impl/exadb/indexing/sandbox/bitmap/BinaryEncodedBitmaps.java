/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link BinaryEncodedBitmaps} contains different utility methods for work with binary encoded bitmaps.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BinaryEncodedBitmaps {
    public static ByteArray normalizeSignedByte(byte value) {
        byte[] buffer = new byte[1];
        buffer[0] = (byte) ((value & 0xFF) ^ 0x80);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeUnsignedByte(byte value) {
        byte[] buffer = new byte[1];
        buffer[0] = (byte) ((value & 0xFF) + 2);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeChar(char value) {
        value += 2;
        byte[] buffer = new byte[2];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeSignedShort(short value) {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (((value >>> 8) & 0xFF) ^ 0x80);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeUnsignedShort(short value) {
        int v = (value & 0xFFFF) + 2;
        byte[] buffer = new byte[2];
        buffer[0] = (byte) v;
        buffer[1] = (byte) (v >>> 8);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeInt(int value) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        buffer[2] = (byte) (value >>> 16);
        buffer[3] = (byte) (((value >>> 24) & 0xFF) ^ 0x80);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeNonNegativeInt(int value) {
        value += 2;
        byte[] buffer = new byte[4];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        buffer[2] = (byte) (value >>> 16);
        buffer[3] = (byte) (value >>> 24);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeLong(long value) {
        byte[] buffer = new byte[8];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        buffer[2] = (byte) (value >>> 16);
        buffer[3] = (byte) (value >>> 24);
        buffer[4] = (byte) (value >>> 32);
        buffer[5] = (byte) (value >>> 40);
        buffer[6] = (byte) (value >>> 48);
        buffer[7] = (byte) (((value >>> 56) & 0xFF) ^ 0x80);

        return new ByteArray(buffer);
    }

    public static ByteArray normalizeNonNegativeLong(long value) {
        value += 2;
        byte[] buffer = new byte[8];
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >>> 8);
        buffer[2] = (byte) (value >>> 16);
        buffer[3] = (byte) (value >>> 24);
        buffer[4] = (byte) (value >>> 32);
        buffer[5] = (byte) (value >>> 40);
        buffer[6] = (byte) (value >>> 48);
        buffer[7] = (byte) (value >>> 56);

        return new ByteArray(buffer);
    }

    public static ByteArray normalizeFloat(float value) {
        int rawBits = Float.floatToRawIntBits(value);
        if (rawBits < 0)
            rawBits = 0x80000000 - rawBits;

        return normalizeInt(rawBits);
    }

    public static ByteArray normalizeDouble(double value) {
        long rawBits = Double.doubleToRawLongBits(value);
        if (rawBits < 0)
            rawBits = 0x8000000000000000l - rawBits;

        return normalizeLong(rawBits);
    }

    public static IKeyNormalizer<Number> createSignedByteKeyNormalizer() {
        return new SignedByteKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createUnsignedByteKeyNormalizer() {
        return new UnsignedByteKeyNormalizer();
    }

    public static IKeyNormalizer<Character> createCharKeyNormalizer() {
        return new CharKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createSignedShortKeyNormalizer() {
        return new SignedShortKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createUnsignedShortKeyNormalizer() {
        return new UnsignedShortKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createIntKeyNormalizer() {
        return new IntKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createNonNegativeIntKeyNormalizer() {
        return new NonNegativeIntKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createLongKeyNormalizer() {
        return new LongKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createNonNegativeLongKeyNormalizer() {
        return new NonNegativeLongKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createFloatKeyNormalizer() {
        return new FloatKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createDoubleKeyNormalizer() {
        return new DoubleKeyNormalizer();
    }

    private static class SignedByteKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeSignedByte(key.byteValue());
        }
    }

    private static class UnsignedByteKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeUnsignedByte(key.byteValue());
        }
    }

    private static class CharKeyNormalizer implements IKeyNormalizer<Character> {
        @Override
        public ByteArray normalize(Character key) {
            return normalizeChar(key.charValue());
        }
    }

    private static class SignedShortKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeSignedShort(key.shortValue());
        }
    }

    private static class UnsignedShortKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeUnsignedShort(key.shortValue());
        }
    }

    private static class IntKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeInt(key.intValue());
        }
    }

    private static class NonNegativeIntKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeNonNegativeInt(key.intValue());
        }
    }

    private static class LongKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeLong(key.longValue());
        }
    }

    private static class NonNegativeLongKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeNonNegativeLong(key.longValue());
        }
    }

    private static class FloatKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeFloat(key.floatValue());
        }
    }

    private static class DoubleKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeDouble(key.doubleValue());
        }
    }

    private BinaryEncodedBitmaps() {
    }
}
