/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.impl.RawAbstractBlockSerialization;
import com.exametrika.common.rawdb.impl.RawDbDebug;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValueSerialization;


/**
 * The {@link IndexValueSerialization} is an implementation of {@link IDataSerialization} based on index value areas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexValueSerialization extends RawAbstractBlockSerialization implements IIndexValueSerialization {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IndexValue value;
    private long areaBlockIndex;
    private long nextAreaBlockIndex;
    private IRawPage lastPage;
    private int dataOffset;

    public IndexValueSerialization(IndexValue value) {
        super(0, 0);

        Assert.notNull(value);

        this.value = value;
        setPosition(0, 0);
    }

    @Override
    public long getAreaId() {
        return areaBlockIndex;
    }

    @Override
    public int getAreaOffset() {
        return blockOffset - dataOffset;
    }

    @Override
    public long getLastAreaId() {
        return value.getLastAreaBlockIndex();
    }

    @Override
    public boolean hasNext(int readSize) {
        return areaBlockIndex != -1 && (nextAreaBlockIndex != 0 || blockOffset + readSize <= blockSize);
    }

    @Override
    public void setPosition(long areaBlockIndex, int areaOffset) {
        setNextPosition(areaBlockIndex, areaOffset);
    }

    @Override
    public void removeRest() {
        Assert.checkState(areaBlockIndex != -1);

        value.freeRest(areaBlockIndex, false);
        nextAreaBlockIndex = 0;
    }

    @Override
    public IIndexValueSerialization clone() {
        return (IIndexValueSerialization) super.clone();
    }

    @Override
    protected void nextReadRegion() {
        nextRegion(true);
    }

    @Override
    protected void nextWriteRegion() {
        nextRegion(false);
    }

    @Override
    protected void debug(int offset, int length) {
        RawDbDebug.debug(value.getSpace().getFileIndex(), Constants.pageIndexByBlockIndex(areaBlockIndex), offset, length);
    }

    private void nextRegion(boolean read) {
        Assert.checkState(areaBlockIndex != -1);

        if (nextAreaBlockIndex == 0) {
            if (read) {
                areaBlockIndex = -1;
                throw new EndOfStreamException();
            } else
                nextAreaBlockIndex = value.allocateArea(lastPage);
        }

        setNextPosition(nextAreaBlockIndex, 0);
    }

    private void setNextPosition(long areaBlockIndex, int areaOffset) {
        this.areaBlockIndex = areaBlockIndex;

        if (areaBlockIndex != 0) {
            int fileIndex = value.getSpace().getFileIndex();
            long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
            int headerOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
            IRawWriteRegion region = getPage(fileIndex, pageIndex).getWriteRegion();
            IRawWriteRegion areaHeaderRegion = region.getRegion(headerOffset, IndexValue.AREA_HEADER_SIZE);

            byte value = areaHeaderRegion.readByte(0);
            if (value != IndexValue.AREA_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));

            nextAreaBlockIndex = areaHeaderRegion.readLong(IndexValue.AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);

            dataOffset = headerOffset + IndexValue.AREA_HEADER_SIZE;
            blockOffset = dataOffset + areaOffset;
            blockSize = headerOffset + Constants.COMPLEX_FIELD_AREA_SIZE;
            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        } else {
            IRawWriteRegion region = (IRawWriteRegion) value.getRegion();
            dataOffset = region.getOffset() + IndexValue.HEADER_SIZE;
            nextAreaBlockIndex = value.getNextAreaBlockIndex();
            blockOffset = dataOffset + areaOffset;
            blockSize = region.getOffset() + region.getLength();

            if (region.getParent() != null)
                region = region.getParent();

            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        }
    }

    private IRawPage getPage(int fileIndex, long pageIndex) {
        if (lastPage != null && lastPage.getIndex() == pageIndex && lastPage.getFile().getIndex() == fileIndex)
            return lastPage;

        lastPage = value.getSpace().getTransaction().getPage(fileIndex, pageIndex);
        return lastPage;
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
