/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.impl.RawAbstractBlockDeserialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValueDeserialization;


/**
 * The {@link IndexValueDeserialization} is an implementation of {@link IDataDeserialization} based on index value areas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexValueDeserialization extends RawAbstractBlockDeserialization implements IIndexValueDeserialization {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IndexValue value;
    private long areaBlockIndex;
    private long nextAreaBlockIndex;
    private IRawPage lastPage;
    private int dataOffset;

    public IndexValueDeserialization(IndexValue value) {
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
    protected void nextReadRegion() {
        Assert.checkState(areaBlockIndex != -1);

        if (nextAreaBlockIndex == 0) {
            areaBlockIndex = -1;
            throw new EndOfStreamException();
        }

        setNextPosition(nextAreaBlockIndex, 0);
    }

    private void setNextPosition(long areaBlockIndex, int areaOffset) {
        this.areaBlockIndex = areaBlockIndex;

        if (areaBlockIndex != 0) {
            int fileIndex = value.getSpace().getFileIndex();
            long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
            int headerOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
            IRawReadRegion region = getPage(fileIndex, pageIndex).getReadRegion();

            byte value = region.readByte(headerOffset);
            if (value != IndexValue.AREA_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));

            nextAreaBlockIndex = region.readLong(headerOffset + IndexValue.AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);

            dataOffset = headerOffset + IndexValue.AREA_HEADER_SIZE;
            blockOffset = dataOffset + areaOffset;
            blockSize = headerOffset + Constants.COMPLEX_FIELD_AREA_SIZE;
            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        } else {
            IRawReadRegion region = value.getRegion();
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
