/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

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
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;


/**
 * The {@link FieldDeserialization} is an implementation of {@link IDataDeserialization} based on field areas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FieldDeserialization extends RawAbstractBlockDeserialization implements IFieldDeserialization {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ComplexField field;
    private long areaBlockIndex;
    private long nextAreaBlockIndex;
    private IRawPage lastPage;
    private int dataOffset;

    public FieldDeserialization(ComplexField field) {
        super(0, 0);

        Assert.notNull(field);

        this.field = field;

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
        return field.getLastAreaBlockIndex();
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

        Node node = field.getNode();
        int fileIndex = node.getFileIndex();
        long prevAreaBlockIndex = areaBlockIndex;

        if (field.getAutoRemoveUnusedAreas() && field.allowDeletion() && !field.isReadOnly()) {
            while (nextAreaBlockIndex != 0) {
                areaBlockIndex = nextAreaBlockIndex;
                long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
                int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
                IRawReadRegion region = getPage(fileIndex, pageIndex).getReadRegion();

                byte value = region.readByte(pageOffset);
                if (value != ComplexField.AREA_MAGIC)
                    throw new RawDatabaseException(messages.invalidFormat(fileIndex));

                byte usageCount = region.readByte(pageOffset + ComplexField.AREA_USAGE_COUNT_OFFSET);
                if (usageCount == 0) {
                    nextAreaBlockIndex = region.readLong(pageOffset + ComplexField.AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);
                    field.freeArea(prevAreaBlockIndex, areaBlockIndex);
                } else
                    break;
            }
        }

        if (nextAreaBlockIndex == 0) {
            areaBlockIndex = -1;
            throw new EndOfStreamException();
        }

        setNextPosition(nextAreaBlockIndex, 0);
    }

    private void setNextPosition(long areaBlockIndex, int areaOffset) {
        Node node = field.getNode();
        int fileIndex = node.getFileIndex();
        IRawPage headerPage = node.getHeaderPage();

        this.areaBlockIndex = areaBlockIndex;

        if (areaBlockIndex != 0) {
            long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
            int headerOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
            IRawReadRegion region = getPage(fileIndex, pageIndex).getReadRegion();

            byte value = region.readByte(headerOffset);
            if (value != ComplexField.AREA_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));

            nextAreaBlockIndex = region.readLong(headerOffset + ComplexField.AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);

            dataOffset = headerOffset + ComplexField.AREA_HEADER_SIZE;
            blockOffset = dataOffset + areaOffset;
            blockSize = headerOffset + Constants.COMPLEX_FIELD_AREA_SIZE;
            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        } else {
            dataOffset = node.getHeaderOffset() + field.getSchema().getOffset() + ComplexField.FIELD_HEADER_SIZE;
            nextAreaBlockIndex = field.getNextAreaBlockIndex();
            blockOffset = dataOffset + areaOffset;
            blockSize = dataOffset + field.getSchema().getConfiguration().getSize() - ComplexField.FIELD_HEADER_SIZE;

            IRawReadRegion region = headerPage.getReadRegion();
            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        }
    }

    private IRawPage getPage(int fileIndex, long pageIndex) {
        if (lastPage != null && lastPage.getIndex() == pageIndex && lastPage.getFile().getIndex() == fileIndex)
            return lastPage;

        lastPage = field.getNode().getPage(fileIndex, pageIndex);
        return lastPage;
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
