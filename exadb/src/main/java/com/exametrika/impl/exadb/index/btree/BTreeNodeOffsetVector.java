/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.ibm.icu.text.MessageFormat;


/**
 * The {@link BTreeIndexSpace} is an offset vector of B+ tree node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeNodeOffsetVector {
    private static final int COMMON_PREFIX_LENGTH_OFFSET = 22;
    private static final int COMMON_PREFIX_OFFSET_OFFSET = 24;
    private static final int ELEMENT_COUNT_OFFSET = 26;
    private static final int ELEMENT_OFFSETS_OFFSET = 28;
    public static final int OFFSET_ELEMENT_SIZE = 4; // offset(short) + keyDigest(short)
    private final IRawPage page;

    public BTreeNodeOffsetVector(IRawPage page) {
        Assert.notNull(page);

        this.page = page;
    }

    public void create() {
        IRawWriteRegion region = page.getWriteRegion();
        region.writeShort(ELEMENT_COUNT_OFFSET, (short) 0);
        region.writeShort(COMMON_PREFIX_LENGTH_OFFSET, (short) 0);
        region.writeShort(COMMON_PREFIX_OFFSET_OFFSET, (short) 0);
    }

    public int getCount() {
        return page.getReadRegion().readShort(ELEMENT_COUNT_OFFSET);
    }

    public void setCount(int count) {
        page.getWriteRegion().writeShort(ELEMENT_COUNT_OFFSET, (short) count);
    }

    public int getCommonPrefixLength() {
        return page.getReadRegion().readShort(COMMON_PREFIX_LENGTH_OFFSET);
    }

    public void setCommonPrefixLength(int length) {
        page.getWriteRegion().writeShort(COMMON_PREFIX_LENGTH_OFFSET, (short) length);
    }

    public int getCommonPrefixOffset() {
        return page.getReadRegion().readShort(COMMON_PREFIX_OFFSET_OFFSET);
    }

    public void setCommonPrefixOffset(int offset) {
        page.getWriteRegion().writeShort(COMMON_PREFIX_OFFSET_OFFSET, (short) offset);
    }

    public int getSize() {
        return getCount() * OFFSET_ELEMENT_SIZE;
    }

    public int getOffset(int index) {
        IRawReadRegion region = page.getReadRegion();
        int vectorOffset = ELEMENT_OFFSETS_OFFSET + (index * OFFSET_ELEMENT_SIZE);
        return region.readShort(vectorOffset);
    }

    public int getKeyDigest(int index) {
        IRawReadRegion region = page.getReadRegion();
        int vectorOffset = ELEMENT_OFFSETS_OFFSET + (index * OFFSET_ELEMENT_SIZE);
        return (region.readShort(vectorOffset + 2) & 0xFFFF);
    }

    public void add(int offset, int keyDigest) {
        insert(getCount(), offset, keyDigest);
    }

    public void insert(int index, int offset, int keyDigest) {
        IRawWriteRegion region = page.getWriteRegion();
        int vectorOffset = ELEMENT_OFFSETS_OFFSET + (index * OFFSET_ELEMENT_SIZE);
        int elementCount = region.readShort(ELEMENT_COUNT_OFFSET);
        region.copy(vectorOffset, vectorOffset + OFFSET_ELEMENT_SIZE, (elementCount - index) * OFFSET_ELEMENT_SIZE);
        region.writeShort(vectorOffset, (short) offset);
        region.writeShort(vectorOffset + 2, (short) keyDigest);
        region.writeShort(ELEMENT_COUNT_OFFSET, (short) (elementCount + 1));
    }

    public void set(int index, int offset, int keyDigest) {
        IRawWriteRegion region = page.getWriteRegion();
        int vectorOffset = ELEMENT_OFFSETS_OFFSET + (index * OFFSET_ELEMENT_SIZE);
        region.writeShort(vectorOffset, (short) offset);
        region.writeShort(vectorOffset + 2, (short) keyDigest);
    }

    public void expand(int index, int count) {
        IRawWriteRegion region = page.getWriteRegion();
        int vectorOffset = ELEMENT_OFFSETS_OFFSET + (index * OFFSET_ELEMENT_SIZE);
        int elementCount = region.readShort(ELEMENT_COUNT_OFFSET);
        region.copy(vectorOffset, vectorOffset + (count * OFFSET_ELEMENT_SIZE), elementCount * OFFSET_ELEMENT_SIZE);
        region.fill(vectorOffset, count * OFFSET_ELEMENT_SIZE, (byte) 0);

        elementCount += count;
        region.writeShort(ELEMENT_COUNT_OFFSET, (short) elementCount);
    }

    public void remove(int index, int count) {
        Assert.isTrue(index + count <= getCount());
        IRawWriteRegion region = page.getWriteRegion();
        int vectorOffset = ELEMENT_OFFSETS_OFFSET + (index * OFFSET_ELEMENT_SIZE);
        int elementCount = region.readShort(ELEMENT_COUNT_OFFSET);
        region.copy(vectorOffset + (count * OFFSET_ELEMENT_SIZE), vectorOffset,
                (elementCount - index - count) * OFFSET_ELEMENT_SIZE);
        region.writeShort(ELEMENT_COUNT_OFFSET, (short) (elementCount - count));
    }

    @Override
    public String toString() {
        StringBuilder offsetsBuilder = new StringBuilder();
        StringBuilder digestsBuilder = new StringBuilder();

        offsetsBuilder.append('[');
        digestsBuilder.append('[');

        int elementCount = getCount();
        boolean first = true;
        for (int i = 0; i < elementCount; i++) {
            if (first)
                first = false;
            else {
                offsetsBuilder.append(", ");
                digestsBuilder.append(", ");
            }

            offsetsBuilder.append(getOffset(i));
            digestsBuilder.append(getKeyDigest(i));
        }

        offsetsBuilder.append(']');
        digestsBuilder.append(']');

        return MessageFormat.format("offsets: {0}\nkey digests: {1}", offsetsBuilder, digestsBuilder);
    }
}