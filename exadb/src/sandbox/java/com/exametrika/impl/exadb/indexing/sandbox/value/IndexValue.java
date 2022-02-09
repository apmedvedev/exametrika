/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.impl.RawHeapWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValue;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValueDeserialization;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValueSerialization;


/**
 * The {@link IndexValue} is a index value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexValue implements IIndexValue {
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final int HEADER_SIZE = 16;
    private static final int NEXT_AREA_BLOCK_INDEX_OFFSET = 0;// nextAreaBlockIndex(long) + lastAreaBlockIndex(long)
    private static final int LAST_AREA_BLOCK_INDEX_OFFSET = 8;
    public static final byte AREA_MAGIC = 0x1A;
    public static final int AREA_HEADER_SIZE = 9;
    public static final int AREA_NEXT_AREA_BLOCK_INDEX_OFFSET = 1;// areaMagic(byte) + nextAreaBlockIndex(long)
    private final IndexValueSpace space;
    private final IRawReadRegion region;
    private final boolean readOnly;

    public static IndexValue create(IndexValueSpace space, int initialSize) {
        Assert.isTrue(initialSize >= HEADER_SIZE * 2);
        IndexValue value = new IndexValue(space, new RawHeapWriteRegion(new byte[initialSize], 0, initialSize), false);

        value.writeAreaBlockIndex(NEXT_AREA_BLOCK_INDEX_OFFSET, 0);
        value.writeAreaBlockIndex(LAST_AREA_BLOCK_INDEX_OFFSET, 0);

        return value;
    }

    public static IndexValue open(IndexValueSpace space, IRawReadRegion region, boolean readOnly) {
        return new IndexValue(space, region, readOnly);
    }

    public IRawReadRegion getRegion() {
        return region;
    }

    public long getLastAreaBlockIndex() {
        return readAreaBlockIndex(LAST_AREA_BLOCK_INDEX_OFFSET);
    }

    public IndexValueSpace getSpace() {
        return space;
    }

    public long getNextAreaBlockIndex() {
        return readAreaBlockIndex(NEXT_AREA_BLOCK_INDEX_OFFSET);
    }

    public long allocateArea(IRawPage preferredPage) {
        long areaBlockIndex = space.allocateArea(preferredPage);

        long lastAreaBlockIndex = getLastAreaBlockIndex();
        if (lastAreaBlockIndex != 0)
            writeAreaHeader(lastAreaBlockIndex, areaBlockIndex, false);
        else
            writeAreaBlockIndex(NEXT_AREA_BLOCK_INDEX_OFFSET, areaBlockIndex);

        writeAreaHeader(areaBlockIndex, 0, true);
        writeAreaBlockIndex(LAST_AREA_BLOCK_INDEX_OFFSET, areaBlockIndex);

        return areaBlockIndex;
    }

    public void freeRest(long areaBlockIndex, boolean delete) {
        IRawTransaction transaction = space.getTransaction();
        long nextAreaBlockIndex;
        if (areaBlockIndex != 0) {
            nextAreaBlockIndex = readAreaHeader(areaBlockIndex);
            writeAreaHeader(areaBlockIndex, 0, false);
        } else {
            nextAreaBlockIndex = readAreaBlockIndex(NEXT_AREA_BLOCK_INDEX_OFFSET);
            if (!delete)
                writeAreaBlockIndex(NEXT_AREA_BLOCK_INDEX_OFFSET, 0);
        }

        if (!delete)
            writeAreaBlockIndex(LAST_AREA_BLOCK_INDEX_OFFSET, areaBlockIndex);

        while (nextAreaBlockIndex != 0) {
            areaBlockIndex = nextAreaBlockIndex;

            long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
            int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
            IRawPage page = transaction.getPage(space.getFileIndex(), pageIndex);

            nextAreaBlockIndex = readAreaHeader(page, pageOffset);
            space.freeArea(page, pageOffset);
        }
    }

    public void onDeleted() {
        freeRest(0, true);
    }

    @Override
    public IIndexValueSerialization createSerialization() {
        Assert.checkState(!readOnly);
        return new IndexValueSerialization(this);
    }

    @Override
    public IIndexValueDeserialization createDeserialization() {
        return new IndexValueDeserialization(this);
    }

    private IndexValue(IndexValueSpace space, IRawReadRegion region, boolean readOnly) {
        Assert.notNull(space);
        Assert.notNull(region);

        this.space = space;
        this.region = region;
        this.readOnly = readOnly;
    }

    private long readAreaHeader(long areaBlockIndex) {
        IRawTransaction transaction = space.getTransaction();
        long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
        int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
        return readAreaHeader(transaction.getPage(space.getFileIndex(), pageIndex), pageOffset);
    }

    private long readAreaHeader(IRawPage page, int pageOffset) {
        IRawReadRegion region = page.getReadRegion();
        byte value = region.readByte(pageOffset);
        if (value != IndexValue.AREA_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(space.getFileIndex()));

        return region.readLong(pageOffset + AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);
    }

    private void writeAreaHeader(long areaBlockIndex, long nextAreaBlockIndex, boolean init) {
        IRawTransaction transaction = space.getTransaction();
        long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
        int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
        IRawWriteRegion region = transaction.getPage(space.getFileIndex(), pageIndex).getWriteRegion();
        if (init)
            region.fill(pageOffset, Constants.COMPLEX_FIELD_AREA_SIZE, (byte) 0);
        region.writeByte(pageOffset, AREA_MAGIC);
        region.writeLong(pageOffset + AREA_NEXT_AREA_BLOCK_INDEX_OFFSET, nextAreaBlockIndex);
    }

    private long readAreaBlockIndex(int offset) {
        return region.readLong(offset);
    }

    private void writeAreaBlockIndex(int offset, long areaBlockIndex) {
        ((IRawWriteRegion) region).writeLong(offset, areaBlockIndex);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
