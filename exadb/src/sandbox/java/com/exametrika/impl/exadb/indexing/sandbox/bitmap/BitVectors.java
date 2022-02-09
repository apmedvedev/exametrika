/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;


/**
 * The {@link BitVectors} contains different utility methods for work with bit vectors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BitVectors {
    public static class Result {
        public long count;
        public final List<Long> positions = new ArrayList<Long>();
    }

    public static List<IRawPage> preload(IRawTransaction transaction, int fileIndex, long pageIndex, int pageCount) {
        List<IRawPage> pages = new ArrayList<IRawPage>();
        for (int i = 0; i < pageCount; i++)
            pages.add(transaction.getPage(fileIndex, pageIndex + i));

        return pages;
    }

    public static void not(long[] destination) {
        for (int i = 0; i < destination.length; i++)
            destination[i] = ~destination[i];
    }

    public static void and(IRawPage source, long[] destination) {
        IRawReadRegion region = source.getReadRegion();
        int pageSize = source.getSize();
        int k = 0, i = 0;
        for (; i < pageSize; i += 8, k++) {
            long value = region.readLong(i);
            destination[k] &= value;
        }
    }

    public static void andNot(IRawPage source, long[] destination) {
        IRawReadRegion region = source.getReadRegion();
        int pageSize = source.getSize();
        int k = 0, i = 0;
        for (; i < pageSize; i += 8, k++) {
            long value = region.readLong(i);
            destination[k] &= ~value;
        }
    }

    public static void or(long[] source, long[] destination) {
        for (int i = 0; i < source.length; i++)
            destination[i] |= source[i];
    }

    public static void updateCount(Result result, long[] bitVector) {
        for (int i = 0; i < bitVector.length; i++)
            result.count += Long.bitCount(bitVector[i]);
    }

    public static void updateResult(Result result, long startIndex, long count, long[] bitVector) {
        for (int i = 0; i < bitVector.length; i++) {
            long value = bitVector[i];

            long mask = 1;
            for (int k = 0; k < 64; k++) {
                if ((value & mask) != 0) {
                    if (result.count >= startIndex && result.count < startIndex + count)
                        result.positions.add(result.count + k);

                    result.count++;
                }

                mask <<= 1;
            }
        }
    }

    private BitVectors() {
    }
}
