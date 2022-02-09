/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;


/**
 * The {@link Constants} contains various DB constants.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Constants {
    public static final byte VERSION = 0x1;
    public static final short LAST_MAGIC = 0x171D;
    public static final int PAGE_SHIFT = 14;
    public static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    public static final long PAGE_OFFSET_MASK = PAGE_SIZE - 1;
    public static final long PAGE_MASK = ~PAGE_OFFSET_MASK;
    public static final int BLOCK_SHIFT = 5;
    public static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    public static final int BLOCK_OFFSET_MASK = BLOCK_SIZE - 1;
    public static final int BLOCK_MASK = ~BLOCK_OFFSET_MASK;
    public static final long BLOCK_OFFSET_MASK_LONG = BLOCK_SIZE - 1;
    public static final long BLOCK_MASK_LONG = ~BLOCK_OFFSET_MASK_LONG;
    public static final int PAGE_BLOCK_SHIFT = PAGE_SHIFT - BLOCK_SHIFT;
    public static final int BLOCKS_PER_PAGE_COUNT = 1 << PAGE_BLOCK_SHIFT;
    public static final long PAGE_BLOCK_OFFSET_MASK = BLOCKS_PER_PAGE_COUNT - 1;
    public static final long PAGE_BLOCK_MASK = ~PAGE_BLOCK_OFFSET_MASK;
    public static final int MAX_SPACE_NODE_SCHEMA_COUNT = (PAGE_SIZE - 2 * BLOCK_SIZE) >>> 3;
    public static final int COMPLEX_FIELD_AREA_BLOCK_COUNT = 2;
    public static final int COMPLEX_FIELD_AREA_DATA_SIZE = COMPLEX_FIELD_AREA_BLOCK_COUNT << BLOCK_SHIFT;
    public static final int COMPLEX_FIELD_AREA_SIZE = (COMPLEX_FIELD_AREA_BLOCK_COUNT + 1) << BLOCK_SHIFT;
    public static final int MAX_NODE_SIZE = PAGE_SIZE >>> 3;
    public static final int NORMAL_PAGE_TYPE = 0;
    public static final int NORMAL_PAGE_SHIFT = PAGE_SHIFT;
    public static final int NORMAL_PAGE_SIZE = PAGE_SIZE;
    public static final int SMALL_PAGE_TYPE = 1;
    public static final int SMALL_PAGE_SHIFT = 11;
    public static final int SMALL_PAGE_SIZE = RawPageTypeConfiguration.MIN_PAGE_SIZE;
    public static final int SMALL_MEDIUM_PAGE_TYPE = 2;
    public static final int SMALL_MEDIUM_PAGE_SHIFT = 16;
    public static final int SMALL_MEDIUM_PAGE_SIZE = 0x10000;
    public static final int MEDIUM_PAGE_TYPE = 3;
    public static final int MEDIUM_PAGE_SHIFT = 17;
    public static final int MEDIUM_PAGE_SIZE = 0x20000;
    public static final int LARGE_MEDIUM_PAGE_TYPE = 4;
    public static final int LARGE_MEDIUM_PAGE_SHIFT = 18;
    public static final int LARGE_MEDIUM_PAGE_SIZE = 0x40000;
    public static final int LARGE_PAGE_TYPE = 5;
    public static final int LARGE_PAGE_SHIFT = 20;
    public static final int LARGE_PAGE_SIZE = 0x100000;
    public static final int EXTRA_LARGE_PAGE_TYPE = 6;
    public static final int EXTRA_LARGE_PAGE_SHIFT = 26;
    public static final int EXTRA_LARGE_PAGE_SIZE = 0x4000000;
    public static final int[] pageSizes = new int[]{NORMAL_PAGE_SIZE, SMALL_PAGE_SIZE, SMALL_MEDIUM_PAGE_SIZE, MEDIUM_PAGE_SIZE,
            LARGE_MEDIUM_PAGE_SIZE, LARGE_PAGE_SIZE, EXTRA_LARGE_PAGE_SIZE};
    public static final int[] pageOffsetMasks = new int[]{NORMAL_PAGE_SIZE - 1, SMALL_PAGE_SIZE - 1, SMALL_MEDIUM_PAGE_SIZE - 1,
            MEDIUM_PAGE_SIZE - 1, LARGE_MEDIUM_PAGE_SIZE - 1, LARGE_PAGE_SIZE - 1, EXTRA_LARGE_PAGE_SIZE - 1};
    public static final int[] pageShifts = new int[]{NORMAL_PAGE_SHIFT, SMALL_PAGE_SHIFT, SMALL_MEDIUM_PAGE_SHIFT, MEDIUM_PAGE_SHIFT,
            LARGE_MEDIUM_PAGE_SHIFT, LARGE_PAGE_SHIFT, EXTRA_LARGE_PAGE_SHIFT};

    public static long pageIndexByFileOffset(long fileOffset) {
        return (fileOffset & PAGE_MASK) >>> PAGE_SHIFT;
    }

    public static int pageOffsetByFileOffset(long fileOffset) {
        return (int) (fileOffset & PAGE_OFFSET_MASK);
    }

    public static long pageIndexByBlockIndex(long blockIndex) {
        return (blockIndex & PAGE_BLOCK_MASK) >>> PAGE_BLOCK_SHIFT;
    }

    public static int pageOffsetByBlockIndex(long blockIndex) {
        return (int) ((blockIndex & PAGE_BLOCK_OFFSET_MASK) << BLOCK_SHIFT);
    }

    public static long fileOffset(long pageIndex, int pageOffset) {
        return (pageIndex << PAGE_SHIFT) + pageOffset;
    }

    public static long alignBlock(long fileOffset) {
        long blockFileOffset = fileOffset(blockIndex(fileOffset));
        if (blockFileOffset < fileOffset)
            blockFileOffset += BLOCK_SIZE;

        return blockFileOffset;
    }

    public static long blockIndex(long pageIndex, int pageOffset) {
        return (pageIndex << PAGE_BLOCK_SHIFT) + (pageOffset >>> BLOCK_SHIFT);
    }

    public static long blockIndex(long fileOffset) {
        return ((fileOffset & BLOCK_MASK_LONG) >>> BLOCK_SHIFT);
    }

    public static long fileOffset(long blockIndex) {
        return blockIndex << BLOCK_SHIFT;
    }

    public static int blockCount(int size) {
        int blockCount = ((size & BLOCK_MASK) >>> BLOCK_SHIFT);
        if ((size & BLOCK_OFFSET_MASK) != 0)
            blockCount++;

        return blockCount;
    }

    public static int dataSize(int blockCount) {
        return blockCount << BLOCK_SHIFT;
    }

    private Constants() {
    }
}
