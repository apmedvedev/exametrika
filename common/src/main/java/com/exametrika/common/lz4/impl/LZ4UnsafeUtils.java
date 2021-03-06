/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import static com.exametrika.common.lz4.impl.LZ4Utils.COPY_LENGTH;
import static com.exametrika.common.lz4.impl.LZ4Utils.LAST_LITERALS;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_BITS;
import static com.exametrika.common.lz4.impl.LZ4Utils.ML_MASK;
import static com.exametrika.common.lz4.impl.LZ4Utils.RUN_MASK;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readByte;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readInt;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readLong;
import static com.exametrika.common.lz4.impl.UnsafeUtils.readShort;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeByte;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeInt;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeLong;
import static com.exametrika.common.lz4.impl.UnsafeUtils.writeShort;
import static com.exametrika.common.lz4.impl.Utils.NATIVE_BYTE_ORDER;

import java.nio.ByteOrder;

public final class LZ4UnsafeUtils {
    static void safeArraycopy(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        final int fastLen = len & 0xFFFFFFF8;
        wildArraycopy(src, srcOff, dest, destOff, fastLen);
        for (int i = 0, slowLen = len & 0x7; i < slowLen; i += 1)
            writeByte(dest, destOff + fastLen + i, readByte(src, srcOff + fastLen + i));
    }

    static void wildArraycopy(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        for (int i = 0; i < len; i += 8)
            writeLong(dest, destOff + i, readLong(src, srcOff + i));
    }

    static void wildIncrementalCopy(byte[] dest, int matchOff, int dOff, int matchCopyEnd) {
        if (dOff - matchOff < 4) {
            for (int i = 0; i < 4; ++i)
                writeByte(dest, dOff + i, readByte(dest, matchOff + i));

            dOff += 4;
            matchOff += 4;
            int dec = 0;
            assert dOff >= matchOff && dOff - matchOff < 8;
            switch (dOff - matchOff) {
                case 1:
                    matchOff -= 3;
                    break;
                case 2:
                    matchOff -= 2;
                    break;
                case 3:
                    matchOff -= 3;
                    dec = -1;
                    break;
                case 5:
                    dec = 1;
                    break;
                case 6:
                    dec = 2;
                    break;
                case 7:
                    dec = 3;
                    break;
                default:
                    break;
            }
            writeInt(dest, dOff, readInt(dest, matchOff));
            dOff += 4;
            matchOff -= dec;
        } else if (dOff - matchOff < COPY_LENGTH) {
            writeLong(dest, dOff, readLong(dest, matchOff));
            dOff += dOff - matchOff;
        }
        while (dOff < matchCopyEnd) {
            writeLong(dest, dOff, readLong(dest, matchOff));
            dOff += 8;
            matchOff += 8;
        }
    }

    static int readShortLittleEndian(byte[] src, int srcOff) {
        short s = readShort(src, srcOff);
        if (NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN)
            s = Short.reverseBytes(s);

        return s & 0xFFFF;
    }

    static void writeShortLittleEndian(byte[] dest, int destOff, int value) {
        short s = (short) value;
        if (NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN)
            s = Short.reverseBytes(s);

        writeShort(dest, destOff, s);
    }

    static int hash(byte[] buf, int off) {
        return LZ4Utils.hash(readInt(buf, off));
    }

    static int hash64k(byte[] buf, int off) {
        return LZ4Utils.hash64k(readInt(buf, off));
    }

    static boolean readIntEquals(byte[] src, int ref, int sOff) {
        return readInt(src, ref) == readInt(src, sOff);
    }

    static int commonBytes(byte[] src, int ref, int sOff, int srcLimit) {
        int matchLen = 0;
        while (sOff <= srcLimit - 8) {
            if (readLong(src, sOff) == readLong(src, ref)) {
                matchLen += 8;
                ref += 8;
                sOff += 8;
            } else {
                final int zeroBits;
                if (NATIVE_BYTE_ORDER == ByteOrder.BIG_ENDIAN)
                    zeroBits = Long.numberOfLeadingZeros(readLong(src, sOff) ^ readLong(src, ref));
                else
                    zeroBits = Long.numberOfTrailingZeros(readLong(src, sOff) ^ readLong(src, ref));

                return matchLen + (zeroBits >>> 3);
            }
        }
        while (sOff < srcLimit && readByte(src, ref++) == readByte(src, sOff++))
            ++matchLen;

        return matchLen;
    }

    static int writeLen(int len, byte[] dest, int dOff) {
        while (len >= 0xFF) {
            writeByte(dest, dOff++, 0xFF);
            len -= 0xFF;
        }
        writeByte(dest, dOff++, len);
        return dOff;
    }

    static int encodeSequence(byte[] src, int anchor, int matchOff, int matchRef, int matchLen, byte[] dest, int dOff,
                              int destEnd) {
        final int runLen = matchOff - anchor;
        final int tokenOff = dOff++;
        int token;

        if (runLen >= RUN_MASK) {
            token = (byte) (RUN_MASK << ML_BITS);
            dOff = writeLen(runLen - RUN_MASK, dest, dOff);
        } else
            token = runLen << ML_BITS;

        // copy literals
        wildArraycopy(src, anchor, dest, dOff, runLen);
        dOff += runLen;

        // encode offset
        final int matchDec = matchOff - matchRef;
        dest[dOff++] = (byte) matchDec;
        dest[dOff++] = (byte) (matchDec >>> 8);

        // encode match len
        matchLen -= 4;
        if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd)
            throw new LZ4Exception("maxDestLen is too small");

        if (matchLen >= ML_MASK) {
            token |= ML_MASK;
            dOff = writeLen(matchLen - RUN_MASK, dest, dOff);
        } else
            token |= matchLen;

        dest[tokenOff] = (byte) token;

        return dOff;
    }

    static int commonBytesBackward(byte[] b, int o1, int o2, int l1, int l2) {
        int count = 0;
        while (o1 > l1 && o2 > l2 && readByte(b, --o1) == readByte(b, --o2))
            ++count;

        return count;
    }

    private LZ4UnsafeUtils() {
    }
}
