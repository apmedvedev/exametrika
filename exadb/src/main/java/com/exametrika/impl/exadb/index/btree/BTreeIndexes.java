/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import com.exametrika.common.utils.ByteArray;


/**
 * The {@link BTreeIndexes} contains different utility methods for work with BTree index keys and values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BTreeIndexes {
    public static int getKeyDigest(ByteArray key) {
        int digest = 0;
        if (key.getLength() >= 1)
            digest |= ((key.getBuffer()[key.getOffset()] & 0xFF) << 8);
        if (key.getLength() >= 2)
            digest |= ((key.getBuffer()[key.getOffset() + 1] & 0xFF));

        return digest;
    }

    public static int getCommonPrefixLength(ByteArray firstKey, ByteArray secondKey) {
        if (firstKey.isEmpty() || secondKey.isEmpty())
            return 0;

        byte[] firstBuffer = firstKey.getBuffer();
        byte[] secondBuffer = secondKey.getBuffer();
        int firstOffset = firstKey.getOffset();
        int secondOffset = secondKey.getOffset();

        int count = Math.min(firstKey.getLength(), secondKey.getLength());
        for (int i = 0; i < count; i++) {
            if ((firstBuffer[firstOffset + i] & 0xFF) != (secondBuffer[secondOffset + i] & 0xFF))
                return i;
        }

        return count;
    }

    public static ByteArray getCommonPrefix(ByteArray firstKey, ByteArray secondKey) {
        int length = getCommonPrefixLength(firstKey, secondKey);
        if (length == 0)
            return ByteArray.EMPTY;

        return firstKey.subArray(0, length);
    }

    public static ByteArray getSeparator(ByteArray prevKey, ByteArray nextKey) {
        byte[] prevBuffer = prevKey.getBuffer();
        byte[] nextBuffer = nextKey.getBuffer();
        int prevOffset = prevKey.getOffset();
        int nextOffset = nextKey.getOffset();
        int prevLength = prevKey.getLength();
        int nextLength = nextKey.getLength();

        for (int i = 0; i < prevLength; i++) {
            int prev = prevBuffer[prevOffset + i] & 0xFF;
            int next;
            if (i < nextLength)
                next = nextBuffer[nextOffset + i] & 0xFF;
            else
                next = 0xFF;

            if (prev == next || (prev == next - 1 && i == nextLength - 1))
                continue;

            byte[] separator = new byte[i + 1];
            System.arraycopy(prevBuffer, prevOffset, separator, 0, i);
            separator[i] = (byte) (prev + 1);

            return new ByteArray(separator);
        }

        return prevKey;
    }

    private BTreeIndexes() {
    }
}
