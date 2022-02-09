/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.impl.RawAbstractBlockSerialization;
import com.exametrika.common.rawdb.impl.RawDbDebug;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;


/**
 * The {@link FieldSerialization} is an implementation of {@link IDataSerialization} based on field areas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FieldSerialization extends RawAbstractBlockSerialization implements IFieldSerialization {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ComplexField field;
    private long areaBlockIndex;
    private long nextAreaBlockIndex;
    private long prevAreaBlockIndex = -1;
    private IRawPage lastPage;
    private IRawWriteRegion areaHeaderRegion;
    private int dataOffset;

    public FieldSerialization(ComplexField field) {
        super(0, 0);

        Assert.notNull(field);
        Assert.isTrue(!field.isReadOnly());

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
        prevAreaBlockIndex = -1;
        setNextPosition(areaBlockIndex, areaOffset);
    }

    @Override
    public void removeRest() {
        Assert.checkState(areaBlockIndex != -1);

        field.freeRest(areaBlockIndex);
        nextAreaBlockIndex = 0;
    }

    @Override
    public void incrementCurrentAreaUsageCount() {
        if (areaBlockIndex == 0 || !field.allowDeletion())
            return;

        Assert.checkState(areaBlockIndex != -1);

        byte value = areaHeaderRegion.readByte(0);
        if (value != ComplexField.AREA_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(field.getNode().getFileIndex()));

        byte count = areaHeaderRegion.readByte(ComplexField.AREA_USAGE_COUNT_OFFSET);
        Assert.isTrue(count >= 0 && count < Byte.MAX_VALUE);
        areaHeaderRegion.writeByte(ComplexField.AREA_USAGE_COUNT_OFFSET, (byte) (count + 1));
    }

    @Override
    public void decrementCurrentAreaUsageCount() {
        if (areaBlockIndex == 0 || !field.allowDeletion())
            return;

        Assert.checkState(areaBlockIndex != -1);

        byte value = areaHeaderRegion.readByte(0);
        if (value != ComplexField.AREA_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(field.getNode().getFileIndex()));

        byte count = areaHeaderRegion.readByte(ComplexField.AREA_USAGE_COUNT_OFFSET);
        if (count > 1)
            areaHeaderRegion.writeByte(ComplexField.AREA_USAGE_COUNT_OFFSET, (byte) (count - 1));
        else {
            areaHeaderRegion.writeByte(ComplexField.AREA_USAGE_COUNT_OFFSET, (byte) 0);
            if (prevAreaBlockIndex != -1) {
                field.freeArea(prevAreaBlockIndex, areaBlockIndex);
                areaBlockIndex = -1;
                if (nextAreaBlockIndex != 0)
                    setNextPosition(nextAreaBlockIndex, 0);
            }
        }
    }

    @Override
    public IFieldSerialization clone() {
        return (IFieldSerialization) super.clone();
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
        RawDbDebug.debug(field.getNode().getFileIndex(), Constants.pageIndexByBlockIndex(areaBlockIndex), offset, length);
    }

    private void nextRegion(boolean read) {
        Assert.checkState(areaBlockIndex != -1);

        Node node = field.getNode();
        int fileIndex = node.getFileIndex();
        prevAreaBlockIndex = areaBlockIndex;

        if (field.getAutoRemoveUnusedAreas() && field.allowDeletion()) {
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
            if (read) {
                areaBlockIndex = -1;
                throw new EndOfStreamException();
            } else
                nextAreaBlockIndex = field.allocateArea(lastPage);
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
            IRawWriteRegion region = getPage(fileIndex, pageIndex).getWriteRegion();
            areaHeaderRegion = region.getRegion(headerOffset, ComplexField.AREA_HEADER_SIZE);

            byte value = areaHeaderRegion.readByte(0);
            if (value != ComplexField.AREA_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));

            nextAreaBlockIndex = areaHeaderRegion.readLong(ComplexField.AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);

            dataOffset = headerOffset + ComplexField.AREA_HEADER_SIZE;
            blockOffset = dataOffset + areaOffset;
            blockSize = headerOffset + Constants.COMPLEX_FIELD_AREA_SIZE;
            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        } else {
            dataOffset = node.getHeaderOffset() + field.getSchema().getOffset() + ComplexField.FIELD_HEADER_SIZE;
            areaHeaderRegion = null;
            nextAreaBlockIndex = field.getNextAreaBlockIndex();
            lastPage = headerPage;
            blockOffset = dataOffset + areaOffset;
            blockSize = dataOffset + field.getSchema().getConfiguration().getSize() - ComplexField.FIELD_HEADER_SIZE;

            IRawReadRegion region = headerPage.getWriteRegion();
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
