/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.ConcurrentModificationException;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.VariableStructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IVisitor;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.schema.VariableStructuredBlobFieldSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;
import com.exametrika.spi.exadb.objectdb.schema.IBlobFieldSchema;


/**
 * The {@link VariableStructuredBlobField} is a variable structured blob field.
 *
 * @param <T> record type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class VariableStructuredBlobField<T> implements IVariableStructuredBlobField<T>, IFieldObject {
    public static final String FIELD_ID_FIELD_NAME = "exaFieldId";
    public static final String RECORD_ID_FIELD_NAME = "exaRecordId";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final byte RECORD_MAGIC = 0x17;
    private static final byte ELEMENT_MAGIC = 0x18;
    private static final int LAST_RECORD_ID_OFFSET = BlobField.HEADER_SIZE;// blobHeader + lastRecordId(long)
    private static final int WRITE_OVERHEAD = 6;
    protected final BlobField field;
    private final String fieldId;
    private int modCount;

    public VariableStructuredBlobField(ISimpleField field) {
        Assert.notNull(field);

        this.field = new BlobField(field);

        NodeSpace space = (NodeSpace) field.getNode().getSpace();
        fieldId = space.getFieldId(field);
    }

    public String getFieldId() {
        return fieldId;
    }

    @Override
    public final boolean isReadOnly() {
        return field.isReadOnly() || (field.getNode().getTransaction().getOptions() & IOperation.DELAYED_FLUSH) != 0;
    }

    @Override
    public final boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public IFieldSchema getSchema() {
        return field.getSchema();
    }

    @Override
    public final INode getNode() {
        return field.getNode();
    }

    @Override
    public final T getObject() {
        return (T) this;
    }

    @Override
    public final void setModified() {
        field.setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);

        field.onCreated(primaryKey, initializer);
        field.getField().getWriteRegion().writeLong(LAST_RECORD_ID_OFFSET, 0l);
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        field.onOpened();
    }

    @Override
    public final void onDeleted() {
        field.onDeleted();
        modCount++;
    }

    @Override
    public final void onUnloaded() {
        field.onUnloaded();
    }

    @Override
    public final void flush() {
        field.flush();
    }

    @Override
    public T get() {
        return null;
    }

    @Override
    public final Object getStore() {
        IBlob blob = field.get();
        if (blob != null)
            return blob.getStore().getNode().getObject();
        else
            return null;
    }

    @Override
    public void setStore(Object store) {
        Assert.checkState(!isReadOnly());

        IBlob blob = field.get();
        if (blob != null) {
            blob.delete();
            blob = null;

            IRawWriteRegion region = field.getField().getWriteRegion();
            region.writeLong(LAST_RECORD_ID_OFFSET, 0l);
        }

        if (store != null) {
            INodeObject nodeObject = (INodeObject) store;
            IBlobStoreField blobStoreField = nodeObject.getNode().getField(
                    ((IBlobFieldSchema) field.getSchema()).getStore().getIndex());

            Assert.isTrue(((IBlobFieldSchema) field.getSchema()).getStore() == blobStoreField.getSchema());

            blob = blobStoreField.createBlob();
        }

        field.set(blob);
        modCount++;
    }


    @Override
    public T getElement(long elementId) {
        IBlob blob = field.get();
        Assert.checkState(blob != null);

        IBlobDeserialization deserialization = blob.createDeserialization();
        readElementHeader(deserialization, elementId);
        return (T) doRead(deserialization);
    }

    @Override
    public IElementIterable<T> getElements(long recordId) {
        return new ElementIterable(recordId);
    }

    @Override
    public IRecordIterable<T> getRecords() {
        return new RecordIterable();
    }

    @Override
    public long addRecord() {
        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();
        long id = serialization.getPosition();

        RecordHeader header = new RecordHeader();
        writeRecordHeader(serialization, id, header);

        serialization.updateEndPosition();

        IRawWriteRegion region = field.getField().getWriteRegion();
        long prevRecordId = region.readLong(LAST_RECORD_ID_OFFSET);

        if (prevRecordId != 0) {
            RecordHeader prevHeader = readRecordHeader(serialization, prevRecordId);
            prevHeader.nextRecordId = id;
            writeRecordHeader(serialization, prevRecordId, prevHeader);
        }

        region.writeLong(LAST_RECORD_ID_OFFSET, id);

        setModified();

        return id;
    }

    @Override
    public long addElement(long recordId, T element) {
        Assert.notNull(element);
        checkClass(element);

        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();
        long id = serialization.getPosition();

        ElementHeader header = new ElementHeader();
        writeElementHeader(serialization, id, header);
        doWrite(serialization, element);

        serialization.updateEndPosition();

        RecordHeader recordHeader = readRecordHeader(serialization, recordId);
        long prevElementId = recordHeader.lastElementId;
        recordHeader.lastElementId = id;
        if (recordHeader.firstElementId == 0)
            recordHeader.firstElementId = id;
        writeRecordHeader(serialization, recordId, recordHeader);

        if (prevElementId != 0) {
            ElementHeader prevHeader = readElementHeader(serialization, prevElementId);
            prevHeader.nextElementId = id;
            writeElementHeader(serialization, prevElementId, prevHeader);
        }

        setModified();

        return id;
    }

    @Override
    public void setElement(long elementId, T element) {
        Assert.notNull(element);
        checkClass(element);

        VariableStructuredBlobFieldSchemaConfiguration configuration = (VariableStructuredBlobFieldSchemaConfiguration) getSchema().getConfiguration();
        Assert.isTrue(configuration.getFixedRecord());

        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();

        readElementHeader(serialization, elementId);
        doWrite(serialization, element);

        setModified();
    }

    @Override
    public void clearRecord(long recordId) {
        IBlob blob = field.get();
        Assert.supports(allowDeletion());
        Assert.checkState(blob != null);
        Assert.checkState(!field.isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();

        RecordHeader header = readRecordHeader(serialization, recordId);
        header.firstElementId = 0;
        header.lastElementId = 0;

        writeRecordHeader(serialization, recordId, header);

        setModified();
    }

    @Override
    public void clear() {
        IBlob blob = field.get();
        Assert.supports(allowDeletion());
        Assert.checkState(blob != null);
        Assert.checkState(!field.isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();
        serialization.setPosition(serialization.getBeginPosition());
        serialization.removeRest();

        IRawWriteRegion region = field.getField().getWriteRegion();
        region.writeLong(LAST_RECORD_ID_OFFSET, 0l);

        setModified();
        modCount++;
    }

    protected Object doRead(IDataDeserialization fieldDeserialization) {
        VariableStructuredBlobFieldSchemaConfiguration configuration = ((VariableStructuredBlobFieldSchemaConfiguration) getSchema().getConfiguration());

        ByteArray buffer;
        if (!configuration.isCompressed())
            buffer = fieldDeserialization.readByteArray();
        else {
            int decompressedLength = fieldDeserialization.readInt();
            ByteArray compressedBuffer = fieldDeserialization.readByteArray();
            buffer = LZ4.decompress(compressedBuffer, decompressedLength);
        }

        if (configuration.getFixedRecord()) {
            boolean bufferFull = fieldDeserialization.readBoolean();
            if (!bufferFull)
                fieldDeserialization.readByteArray();
        }

        ISerializationRegistry registry = ((VariableStructuredBlobFieldSchema) field.getSchema()).getSerializationRegistry();
        ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        Deserialization deserialization = new Deserialization(registry, stream);

        VariableStructuredBlobFieldSchema schema = (VariableStructuredBlobFieldSchema) getSchema();
        if (schema.getMainClass() != null)
            return deserialization.readTypedObject(schema.getMainClass());
        else
            return deserialization.readObject();
    }

    protected void doWrite(IDataSerialization fieldSerialization, Object record) {
        VariableStructuredBlobFieldSchemaConfiguration configuration = ((VariableStructuredBlobFieldSchemaConfiguration) getSchema().getConfiguration());

        ByteOutputStream stream = new ByteOutputStream();
        ISerializationRegistry registry = ((VariableStructuredBlobFieldSchema) field.getSchema()).getSerializationRegistry();
        Serialization serialization = new Serialization(registry, false, stream);

        VariableStructuredBlobFieldSchema schema = (VariableStructuredBlobFieldSchema) getSchema();
        if (schema.getMainClass() != null)
            serialization.writeTypedObject(record);
        else
            serialization.writeObject(record);

        ByteArray buffer = new ByteArray(stream.getBuffer(), 0, stream.getLength());

        if (!configuration.isCompressed())
            fieldSerialization.writeByteArray(buffer);
        else {
            ByteArray compressedBuffer = LZ4.compress(true, buffer);

            fieldSerialization.writeInt(buffer.getLength());
            fieldSerialization.writeByteArray(compressedBuffer);

            buffer = compressedBuffer;
        }

        if (configuration.getFixedRecord()) {
            int bufferLength = buffer.getLength() - WRITE_OVERHEAD;
            Assert.isTrue(bufferLength <= configuration.getFixedRecordSize());
            boolean bufferFull = bufferLength == configuration.getFixedRecordSize();

            fieldSerialization.writeBoolean(bufferFull);

            if (!bufferFull) {
                ByteArray restBuffer = new ByteArray(configuration.getFixedRecordSize() - bufferLength);
                fieldSerialization.writeByteArray(restBuffer);
            }
        }
    }

    private ElementHeader readElementHeader(IBlobDeserialization deserialization, long elementId) {
        field.getField().refresh();

        deserialization.setPosition(elementId);

        if (deserialization.readByte() != ELEMENT_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getBlob()));

        ElementHeader header = new ElementHeader();
        header.nextElementId = deserialization.readLong();
        return header;
    }

    private void writeElementHeader(IBlobSerialization serialization, long elementId, ElementHeader header) {
        field.getField().refresh();

        serialization.setPosition(elementId);

        serialization.writeByte(ELEMENT_MAGIC);
        serialization.writeLong(header.nextElementId);
    }

    private RecordHeader readRecordHeader(IBlobDeserialization deserialization, long recordId) {
        field.getField().refresh();

        deserialization.setPosition(recordId);

        if (deserialization.readByte() != RECORD_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getBlob()));

        RecordHeader header = new RecordHeader();
        header.nextRecordId = deserialization.readLong();
        header.firstElementId = deserialization.readLong();
        header.lastElementId = deserialization.readLong();
        return header;
    }

    private void writeRecordHeader(IBlobSerialization serialization, long recordId, RecordHeader header) {
        field.getField().refresh();

        serialization.setPosition(recordId);

        serialization.writeByte(RECORD_MAGIC);
        serialization.writeLong(header.nextRecordId);
        serialization.writeLong(header.firstElementId);
        serialization.writeLong(header.lastElementId);
    }

    protected void checkClass(Object element) {
        VariableStructuredBlobFieldSchema schema = (VariableStructuredBlobFieldSchema) getSchema();

        Assert.isTrue(schema.getAllowedClasses() == null || schema.getAllowedClasses().contains(element.getClass()));
    }

    private static class ElementHeader {
        long nextElementId;
    }

    private static class RecordHeader {
        long nextRecordId;
        long firstElementId;
        long lastElementId;
    }

    protected class ElementIterable implements IElementIterable<T> {
        private final long recordId;

        public ElementIterable(long recordId) {
            this.recordId = recordId;
        }

        @Override
        public IElementIterator<T> iterator() {
            return new ElementIterator(recordId);
        }

        @Override
        public void visitElements(ICondition condition, IVisitor visitor) {
            Assert.notNull(visitor);

            for (Object element : this) {
                if (condition == null || condition.evaluate(element)) {
                    if (!visitor.visit(element))
                        break;
                }
            }
        }
    }

    protected class ElementIterator implements IElementIterator<T> {
        private final IBlobDeserialization deserialization;
        private final int modCount;
        private long nextElementId;
        private long currentElementId;
        private T element;

        public ElementIterator(long recordId) {
            IBlob blob = field.get();
            Assert.checkState(blob != null);

            deserialization = blob.createDeserialization();
            RecordHeader header = readRecordHeader(deserialization, recordId);
            nextElementId = header.firstElementId;

            modCount = VariableStructuredBlobField.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return nextElementId != 0;
        }

        @Override
        public T next() {
            Assert.checkState(nextElementId != 0);

            if (modCount != VariableStructuredBlobField.this.modCount)
                throw new ConcurrentModificationException();

            ElementHeader header = readElementHeader(deserialization, nextElementId);
            currentElementId = nextElementId;
            nextElementId = header.nextElementId;

            element = (T) doRead(deserialization);
            return element;
        }

        @Override
        public IVariableStructuredBlobField<T> getField() {
            return VariableStructuredBlobField.this;
        }

        @Override
        public long getId() {
            return currentElementId;
        }

        @Override
        public T get() {
            return element;
        }

        @Override
        public void set(T element) {
            this.element = element;
            setElement(currentElementId, element);
        }

        @Override
        public void setNext(long id) {
            nextElementId = id;
        }
    }

    protected class RecordIterable implements IRecordIterable<T> {
        @Override
        public IRecordIterator<T> iterator() {
            return new RecordIterator();
        }

        @Override
        public void visitRecords(ICondition condition, IVisitor visitor) {
            Assert.notNull(visitor);

            for (Object record : this) {
                if (condition == null || condition.evaluate(record)) {
                    if (!visitor.visit(record))
                        break;
                }
            }
        }
    }

    protected class RecordIterator implements IRecordIterator<T> {
        private final IBlobDeserialization deserialization;
        private final int modCount;
        private long nextRecordId;
        private long currentRecordId;
        private ElementIterable record;

        public RecordIterator() {
            IBlob blob = field.get();
            Assert.checkState(blob != null);

            deserialization = blob.createDeserialization();
            nextRecordId = blob.getBeginPosition();

            modCount = VariableStructuredBlobField.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return nextRecordId != 0;
        }

        @Override
        public IElementIterable<T> next() {
            Assert.checkState(nextRecordId != 0);

            if (modCount != VariableStructuredBlobField.this.modCount)
                throw new ConcurrentModificationException();

            RecordHeader header = readRecordHeader(deserialization, nextRecordId);
            currentRecordId = nextRecordId;
            nextRecordId = header.nextRecordId;

            record = new ElementIterable(currentRecordId);
            return record;
        }

        @Override
        public IVariableStructuredBlobField<T> getField() {
            return VariableStructuredBlobField.this;
        }

        @Override
        public long getId() {
            return currentRecordId;
        }

        @Override
        public IElementIterable<T> get() {
            return record;
        }

        @Override
        public void setNext(long id) {
            nextRecordId = id;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of blob ''{0}''.")
        ILocalizedMessage invalidFormat(IBlob blob);
    }
}
