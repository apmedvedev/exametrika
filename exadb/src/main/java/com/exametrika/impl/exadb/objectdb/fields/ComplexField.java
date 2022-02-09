/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;


/**
 * The {@link ComplexField} is a complex node field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ComplexField implements IComplexField, IFieldObject {
    public static final int FIELD_HEADER_SIZE = 16;// nextAreaBlockIndex(long) + lastAreaBlockIndex(long)
    private static final int FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET = 0;
    private static final int FIELD_LAST_AREA_BLOCK_INDEX_OFFSET = 8;
    public static final int AREA_HEADER_SIZE = 10;// areaMagic(byte) + usageCount(byte) + nextAreaBlockIndex(long)
    public static final int AREA_USAGE_COUNT_OFFSET = 1;
    public static final int AREA_NEXT_AREA_BLOCK_INDEX_OFFSET = 2;
    public static final byte AREA_MAGIC = 0x18;
    private static final byte FLAG_READONLY = 0x1;
    private static final byte FLAG_AUTO_REMOVE_UNUSED_AREAS = 0x2;
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Node node;
    private final IFieldSchema schema;
    private IFieldObject object;
    private byte flags;

    public static ComplexField create(Node node, int fieldIndex, Object primaryKey, Object initializer) {
        Assert.notNull(node);

        IFieldSchema schema = node.getSchema().getFields().get(fieldIndex);

        ComplexField field = new ComplexField(node, schema);

        field.writeAreaBlockIndex(FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET, 0);
        field.writeAreaBlockIndex(FIELD_LAST_AREA_BLOCK_INDEX_OFFSET, 0);

        field.object = schema.createField(field);
        field.onCreated(primaryKey, initializer);

        return field;
    }

    public static ComplexField open(Node node, int fieldIndex, boolean create, Object primaryKey, Object initializer) {
        Assert.notNull(node);

        IFieldSchema schema = node.getSchema().getFields().get(fieldIndex);
        ComplexField field = new ComplexField(node, schema);

        field.object = schema.createField(field);
        if (create)
            field.onCreated(primaryKey, initializer);
        else
            field.onOpened();

        return field;
    }

    public long getLastAreaBlockIndex() {
        return readAreaBlockIndex(FIELD_LAST_AREA_BLOCK_INDEX_OFFSET);
    }

    public long getNextAreaBlockIndex() {
        return readAreaBlockIndex(FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET);
    }

    public long allocateArea(IRawPage preferredPage) {
        Assert.checkState((flags & FLAG_READONLY) == 0);

        node.setModified();

        long areaBlockIndex = node.allocateArea(preferredPage);

        long lastAreaBlockIndex = getLastAreaBlockIndex();
        if (lastAreaBlockIndex != 0)
            writeAreaHeader(lastAreaBlockIndex, (byte) -1, areaBlockIndex, false);
        else
            writeAreaBlockIndex(FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET, areaBlockIndex);

        writeAreaHeader(areaBlockIndex, (byte) 0, 0, true);
        writeAreaBlockIndex(FIELD_LAST_AREA_BLOCK_INDEX_OFFSET, areaBlockIndex);

        return areaBlockIndex;
    }

    public void freeAll() {
        freeRest(0);
    }

    public void freeRest(long areaBlockIndex) {
        Assert.checkState(allowDeletion());

        long nextAreaBlockIndex;
        if (areaBlockIndex != 0) {
            nextAreaBlockIndex = readAreaHeader(areaBlockIndex);
            writeAreaHeader(areaBlockIndex, (byte) -1, 0, false);
        } else {
            nextAreaBlockIndex = readAreaBlockIndex(FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET);
            writeAreaBlockIndex(FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET, 0);
        }

        writeAreaBlockIndex(FIELD_LAST_AREA_BLOCK_INDEX_OFFSET, areaBlockIndex);

        while (nextAreaBlockIndex != 0) {
            areaBlockIndex = nextAreaBlockIndex;

            long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
            int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
            IRawPage page = node.getPage(node.getFileIndex(), pageIndex);

            nextAreaBlockIndex = readAreaHeader(page, pageOffset);
            node.freeArea(page, pageOffset);
        }
    }

    public void freeArea(long prevAreaBlockIndex, long areaBlockIndex) {
        Assert.checkState(allowDeletion());

        long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
        int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
        IRawPage page = node.getPage(node.getFileIndex(), pageIndex);

        long nextAreaBlockIndex = readAreaHeader(page, pageOffset);

        if (prevAreaBlockIndex != 0)
            writeAreaHeader(prevAreaBlockIndex, (byte) -1, nextAreaBlockIndex, false);
        else
            writeAreaBlockIndex(FIELD_NEXT_AREA_BLOCK_INDEX_OFFSET, nextAreaBlockIndex);

        if (nextAreaBlockIndex == 0)
            writeAreaBlockIndex(FIELD_LAST_AREA_BLOCK_INDEX_OFFSET, prevAreaBlockIndex);

        node.freeArea(page, pageOffset);
    }

    @Override
    public boolean isReadOnly() {
        return (flags & FLAG_READONLY) != 0 || node.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return node.allowFieldDeletion();
    }

    @Override
    public IFieldSchema getSchema() {
        return schema;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public <T> T get() {
        return null;
    }

    @Override
    public <T> T getObject() {
        return (T) object;
    }

    @Override
    public void refresh() {
        node.refresh();
    }

    @Override
    public void setModified() {
        Assert.checkState((flags & FLAG_READONLY) == 0);

        node.setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        flags &= ~FLAG_READONLY;

        int offset = node.getHeaderOffset() + schema.getOffset() + FIELD_HEADER_SIZE;
        int size = schema.getConfiguration().getSize() - FIELD_HEADER_SIZE;
        node.getHeaderPage().getWriteRegion().fill(offset, size, (byte) 0);

        if (object != this)
            object.onCreated(primaryKey, initializer);

        if (schema.getConfiguration().isPrimary())
            flags |= FLAG_READONLY;
        else
            flags &= ~FLAG_READONLY;
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
        if (object != this)
            object.onAfterCreated(primaryKey, initializer);
    }

    @Override
    public void onOpened() {
        if (object != this)
            object.onOpened();

        if (schema.getConfiguration().isPrimary())
            flags |= FLAG_READONLY;
        else
            flags &= ~FLAG_READONLY;
    }

    @Override
    public void onDeleted() {
        if (object != this)
            object.onDeleted();

        freeAll();
    }

    @Override
    public void onUnloaded() {
        if (object != this)
            object.onUnloaded();
    }

    @Override
    public boolean getAutoRemoveUnusedAreas() {
        return (flags & FLAG_AUTO_REMOVE_UNUSED_AREAS) != 0;
    }

    @Override
    public void setAutoRemoveUnusedAreas() {
        flags |= FLAG_AUTO_REMOVE_UNUSED_AREAS;
    }

    @Override
    public IFieldSerialization createSerialization() {
        Assert.checkState((flags & FLAG_READONLY) == 0);

        node.setModified();

        return new FieldSerialization(this);
    }

    @Override
    public IFieldDeserialization createDeserialization() {
        return new FieldDeserialization(this);
    }

    @Override
    public void flush() {
        if (object != this)
            object.flush();
    }

    private ComplexField(Node node, IFieldSchema schema) {
        Assert.notNull(node);
        Assert.notNull(schema);

        this.node = node;
        this.schema = schema;
    }

    private long readAreaHeader(long areaBlockIndex) {
        long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
        int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
        return readAreaHeader(node.getPage(node.getFileIndex(), pageIndex), pageOffset);
    }

    private long readAreaHeader(IRawPage page, int pageOffset) {
        IRawReadRegion region = page.getReadRegion();
        byte value = region.readByte(pageOffset);
        if (value != ComplexField.AREA_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(node.getFileIndex()));
        // skip usage count

        return region.readLong(pageOffset + AREA_NEXT_AREA_BLOCK_INDEX_OFFSET);
    }

    private void writeAreaHeader(long areaBlockIndex, byte usageCount, long nextAreaBlockIndex, boolean init) {
        long pageIndex = Constants.pageIndexByBlockIndex(areaBlockIndex);
        int pageOffset = Constants.pageOffsetByBlockIndex(areaBlockIndex);
        IRawWriteRegion region = node.getPage(node.getFileIndex(), pageIndex).getWriteRegion();
        if (init)
            region.fill(pageOffset, Constants.COMPLEX_FIELD_AREA_SIZE, (byte) 0);
        region.writeByte(pageOffset, AREA_MAGIC);
        if (usageCount != -1)
            region.writeByte(pageOffset + AREA_USAGE_COUNT_OFFSET, usageCount);
        region.writeLong(pageOffset + AREA_NEXT_AREA_BLOCK_INDEX_OFFSET, nextAreaBlockIndex);
    }

    private long readAreaBlockIndex(int offset) {
        IRawReadRegion region = node.getHeaderPage().getReadRegion();
        int fieldOffset = node.getHeaderOffset() + schema.getOffset();
        return region.readLong(fieldOffset + offset);
    }

    private void writeAreaBlockIndex(int offset, long areaBlockIndex) {
        IRawWriteRegion region = node.getHeaderPage().getWriteRegion();
        int fieldOffset = node.getHeaderOffset() + schema.getOffset();

        region.writeLong(fieldOffset + offset, areaBlockIndex);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
