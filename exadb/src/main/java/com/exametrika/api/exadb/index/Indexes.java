/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.ibm.icu.text.Collator;


/**
 * The {@link Indexes} contains different utility methods for work with index keys and values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Indexes {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static ByteArray normalizeBoolean(boolean value) {
        return normalizeByte(value ? (byte) 1 : (byte) 0);
    }

    public static ByteArray normalizeByte(byte value) {
        byte[] buffer = new byte[1];
        buffer[0] = (byte) ((value & 0xFF) ^ 0x80);
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeChar(char value) {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) (value >>> 8);
        buffer[1] = (byte) value;
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeShort(short value) {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) (((value >>> 8) & 0xFF) ^ 0x80);
        buffer[1] = (byte) value;
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeInt(int value) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) (((value >>> 24) & 0xFF) ^ 0x80);
        buffer[1] = (byte) (value >>> 16);
        buffer[2] = (byte) (value >>> 8);
        buffer[3] = (byte) value;
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeLong(long value) {
        byte[] buffer = new byte[8];
        normalizeLong(buffer, 0, value);
        return new ByteArray(buffer);
    }

    public static void normalizeLong(byte[] buffer, int offset, long value) {
        buffer[offset + 0] = (byte) (((value >>> 56) & 0xFF) ^ 0x80);
        buffer[offset + 1] = (byte) (value >>> 48);
        buffer[offset + 2] = (byte) (value >>> 40);
        buffer[offset + 3] = (byte) (value >>> 32);
        buffer[offset + 4] = (byte) (value >>> 24);
        buffer[offset + 5] = (byte) (value >>> 16);
        buffer[offset + 6] = (byte) (value >>> 8);
        buffer[offset + 7] = (byte) value;
    }

    public static int denormalizeInt(byte[] buffer, int offset) {
        return (((buffer[offset + 0] & 0xFF) ^ 0x80) << 24) + ((buffer[offset + 1] & 0xFF) << 16) +
                ((buffer[offset + 2] & 0xFF) << 8) + (buffer[offset + 3] & 0xFF);
    }

    public static long denormalizeLong(byte[] buffer, int offset) {
        return ((long) ((buffer[offset + 0] & 0xFF) ^ 0x80) << 56) + ((long) (buffer[offset + 1] & 0xFF) << 48) +
                ((long) (buffer[offset + 2] & 0xFF) << 40) + ((long) (buffer[offset + 3] & 0xFF) << 32) + ((long) (buffer[offset + 4] & 0xFF) << 24) +
                ((long) (buffer[offset + 5] & 0xFF) << 16) + ((long) (buffer[offset + 6] & 0xFF) << 8) + (buffer[offset + 7] & 0xFF);
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

    public static ByteArray normalizeUUID(UUID value) {
        byte[] buffer = new byte[16];
        normalizeLong(buffer, 0, value.getMostSignificantBits());
        normalizeLong(buffer, 8, value.getLeastSignificantBits());
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeFixedString(String value) {
        byte[] buffer = new byte[2 * value.length()];
        int offset = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            buffer[offset++] = (byte) (ch >>> 8);
            buffer[offset++] = (byte) ch;
        }
        return new ByteArray(buffer);
    }

    public static ByteArray normalizeString(String value) {
        return new ByteArray(value.getBytes(UTF8_CHARSET));
    }

    public static ByteArray normalizeCollationKey(Collator collator, String value) {
        byte[] buffer = collator.getCollationKey(value).toByteArray();
        return new ByteArray(buffer);
    }

    public static void normalizeComposite(ByteOutputStream stream, ByteArray key) {
        for (int k = 0; k < key.getLength(); k++) {
            int b = key.get(k);
            if (b != 0)
                stream.write(b);
            else {
                stream.write(0x0);
                stream.write(0xFF);
            }
        }
    }

    public static void normalizeFixedComposite(ByteOutputStream stream, ByteArray key) {
        stream.write(key.getBuffer(), key.getOffset(), key.getLength());
    }

    public static IKeyNormalizer<Boolean> createBooleanKeyNormalizer() {
        return new BooleanKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createByteKeyNormalizer() {
        return new ByteKeyNormalizer();
    }

    public static IKeyNormalizer<Character> createCharKeyNormalizer() {
        return new CharKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createShortKeyNormalizer() {
        return new ShortKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createIntKeyNormalizer() {
        return new IntKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createLongKeyNormalizer() {
        return new LongKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createFloatKeyNormalizer() {
        return new FloatKeyNormalizer();
    }

    public static IKeyNormalizer<Number> createDoubleKeyNormalizer() {
        return new DoubleKeyNormalizer();
    }

    public static IKeyNormalizer<UUID> createUUIDKeyNormalizer() {
        return new UUIDKeyNormalizer();
    }

    public static IKeyNormalizer<String> createFixedStringKeyNormalizer() {
        return new FixedStringKeyNormalizer();
    }

    public static IKeyNormalizer<String> createStringKeyNormalizer() {
        return new StringKeyNormalizer();
    }

    public static IKeyNormalizer<String> createCollationKeyNormalizer(Collator collator) {
        return new CollationKeyNormalizer(collator);
    }

    public static IKeyNormalizer<ByteArray> createByteArrayKeyNormalizer() {
        return new ByteArrayKeyNormalizer();
    }

    public static IKeyNormalizer<List> createCompositeNormalizer(List<? extends IKeyNormalizer> normalizers) {
        return new CompositeKeyNormalizer((List<IKeyNormalizer>) normalizers);
    }

    public static IKeyNormalizer<List> createFixedCompositeNormalizer(List<? extends IKeyNormalizer> normalizers) {
        return new FixedCompositeKeyNormalizer((List<IKeyNormalizer>) normalizers);
    }

    public static IKeyNormalizer<Object> createDescendingNormalizer(IKeyNormalizer<Object> normalizer) {
        return new DescendingKeyNormalizer(normalizer);
    }

    public static IValueConverter<Long> createLongValueConverter() {
        return new LongValueConverter();
    }

    public static IValueConverter<Integer> createIntValueConverter() {
        return new IntValueConverter();
    }

    public static IValueConverter<ByteArray> createByteArrayValueConverter() {
        return new ByteArrayValueConverter();
    }

    private Indexes() {
    }

    private static class BooleanKeyNormalizer implements IKeyNormalizer<Boolean> {
        @Override
        public ByteArray normalize(Boolean key) {
            return normalizeBoolean(key);
        }
    }

    private static class ByteKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeByte(key.byteValue());
        }
    }

    private static class CharKeyNormalizer implements IKeyNormalizer<Character> {
        @Override
        public ByteArray normalize(Character key) {
            return normalizeChar(key.charValue());
        }
    }

    private static class ShortKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeShort(key.shortValue());
        }
    }

    private static class IntKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeInt(key.intValue());
        }
    }

    private static class LongKeyNormalizer implements IKeyNormalizer<Number> {
        @Override
        public ByteArray normalize(Number key) {
            return normalizeLong(key.longValue());
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

    private static class UUIDKeyNormalizer implements IKeyNormalizer<UUID> {
        @Override
        public ByteArray normalize(UUID key) {
            return normalizeUUID(key);
        }
    }

    private static class FixedStringKeyNormalizer implements IKeyNormalizer<String> {
        @Override
        public ByteArray normalize(String key) {
            return normalizeFixedString(key);
        }
    }

    private static class StringKeyNormalizer implements IKeyNormalizer<String> {
        @Override
        public ByteArray normalize(String key) {
            return normalizeString(key);
        }
    }

    private static class CollationKeyNormalizer implements IKeyNormalizer<String> {
        private final Collator collator;

        public CollationKeyNormalizer(Collator collator) {
            Assert.notNull(collator);

            this.collator = collator;
        }

        @Override
        public ByteArray normalize(String key) {
            return normalizeCollationKey(collator, key);
        }
    }

    private static class ByteArrayKeyNormalizer implements IKeyNormalizer<ByteArray> {
        @Override
        public ByteArray normalize(ByteArray key) {
            return key;
        }
    }

    private static class CompositeKeyNormalizer implements IKeyNormalizer<List> {
        private final List<IKeyNormalizer> normalizers;

        public CompositeKeyNormalizer(List<IKeyNormalizer> normalizers) {
            Assert.notNull(normalizers);

            this.normalizers = normalizers;
        }

        @Override
        public ByteArray normalize(List key) {
            Assert.notNull(key);
            Assert.isTrue(key.size() == normalizers.size());

            ByteOutputStream stream = new ByteOutputStream();
            boolean first = true;
            for (int i = 0; i < key.size(); i++) {
                if (first)
                    first = false;
                else {
                    stream.write(0x0);
                    stream.write(0x0);
                }

                ByteArray byteKey = normalizers.get(i).normalize(key.get(i));
                normalizeComposite(stream, byteKey);
            }

            return new ByteArray(stream.getBuffer(), 0, stream.getLength());
        }
    }

    private static class FixedCompositeKeyNormalizer implements IKeyNormalizer<List> {
        private final List<IKeyNormalizer> normalizers;

        public FixedCompositeKeyNormalizer(List<IKeyNormalizer> normalizers) {
            Assert.notNull(normalizers);

            this.normalizers = normalizers;
        }

        @Override
        public ByteArray normalize(List key) {
            Assert.notNull(key);
            Assert.isTrue(key.size() == normalizers.size());

            ByteOutputStream stream = new ByteOutputStream();
            for (int i = 0; i < key.size(); i++) {
                ByteArray byteKey = normalizers.get(i).normalize(key.get(i));
                normalizeFixedComposite(stream, byteKey);
            }

            return new ByteArray(stream.getBuffer(), 0, stream.getLength());
        }
    }

    private static class DescendingKeyNormalizer implements IKeyNormalizer<Object> {
        private final IKeyNormalizer<Object> normalizer;

        public DescendingKeyNormalizer(IKeyNormalizer<Object> normalizer) {
            Assert.notNull(normalizer);

            this.normalizer = normalizer;
        }

        @Override
        public ByteArray normalize(Object key) {
            ByteArray normalizedKey = normalizer.normalize(key);
            byte[] buffer = new byte[normalizedKey.getLength()];
            for (int k = 0; k < normalizedKey.getLength(); k++) {
                int b = normalizedKey.get(k);
                buffer[k] = (byte) (~b & 0xFF);
            }

            return new ByteArray(buffer);
        }
    }

    private static class LongValueConverter implements IValueConverter<Long> {
        @Override
        public ByteArray toByteArray(Long value) {
            return normalizeLong(value.longValue());
        }

        @Override
        public Long toValue(ByteArray buffer) {
            return denormalizeLong(buffer.getBuffer(), buffer.getOffset());
        }
    }

    private static class IntValueConverter implements IValueConverter<Integer> {
        @Override
        public ByteArray toByteArray(Integer value) {
            return normalizeInt(value.intValue());
        }

        @Override
        public Integer toValue(ByteArray buffer) {
            return denormalizeInt(buffer.getBuffer(), buffer.getOffset());
        }
    }

    private static class ByteArrayValueConverter implements IValueConverter<ByteArray> {
        @Override
        public ByteArray toByteArray(ByteArray value) {
            return value;
        }

        @Override
        public ByteArray toValue(ByteArray buffer) {
            return buffer;
        }
    }
}
