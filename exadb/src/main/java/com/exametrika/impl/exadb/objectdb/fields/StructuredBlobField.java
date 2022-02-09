/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
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
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IVisitor;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;
import com.exametrika.spi.exadb.objectdb.schema.IBlobFieldSchema;


/**
 * The {@link StructuredBlobField} is a structured blob field.
 *
 * @param <T> record type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StructuredBlobField<T> implements IStructuredBlobField<T>, IFieldObject, IRecordIndexProvider, IFullTextDocumentSpace {
    public static final String FIELD_ID_FIELD_NAME = "exaFieldId";
    public static final String RECORD_ID_FIELD_NAME = "exaRecordId";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final byte MAGIC = 0x17;
    private static final int LAST_RECORD_ID_OFFSET = BlobField.HEADER_SIZE;// blobHeader + lastRecordId(long)
    private static final int WRITE_OVERHEAD = 6;
    protected final BlobField field;
    private final StructuredBlobIndex[] indexes;
    private final IRecordIndexer recordIndexer;
    private final String fieldId;
    private StructuredBlobFullTextIndex fullTextIndex;
    private T current;
    private int modCount;

    public StructuredBlobField(ISimpleField field) {
        Assert.notNull(field);

        this.field = new BlobField(field);

        StructuredBlobFieldSchemaConfiguration configuration = (StructuredBlobFieldSchemaConfiguration) field.getSchema().getConfiguration();
        StructuredBlobFieldSchema schema = (StructuredBlobFieldSchema) getSchema();
        NodeSpace space = (NodeSpace) field.getNode().getSpace();

        if (configuration.getRecordIndexer() != null)
            recordIndexer = configuration.getRecordIndexer().createIndexer(this, this);
        else
            recordIndexer = null;

        indexes = new StructuredBlobIndex[configuration.getIndexes().size()];
        for (int i = 0; i < configuration.getIndexes().size(); i++)
            indexes[i] = createNodeIndex(space.getBlobIndex(schema, i), configuration.getIndexes().get(i));

        if (configuration.isFullTextIndex())
            fullTextIndex = new StructuredBlobFullTextIndex(getContext(), space.getFullTextIndex().getIndex(), 0, space, this);
        else
            fullTextIndex = null;

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

        long lastRecordId = field.getField().getReadRegion().readLong(LAST_RECORD_ID_OFFSET);
        if (lastRecordId != 0)
            current = get(lastRecordId);
    }

    @Override
    public final void onDeleted() {
        clearIndexes();

        field.onDeleted();
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
    public final T get() {
        return current;
    }

    @Override
    public final IRecordIndexer getRecordIndexer() {
        return recordIndexer;
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
            clearIndexes();
            blob.delete();
            blob = null;
            current = null;

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
    }

    @Override
    public long getCurrentId() {
        IRawReadRegion region = field.getField().getReadRegion();
        return region.readLong(LAST_RECORD_ID_OFFSET);
    }

    @Override
    public final T getCurrent() {
        return current;
    }

    @Override
    public final T get(long id) {
        IBlob blob = field.get();
        Assert.checkState(blob != null);

        IBlobDeserialization deserialization = blob.createDeserialization();
        deserialization.setPosition(id);
        return (T) read(deserialization, true);
    }

    @Override
    public IStructuredIterable getRecords() {
        IBlob blob = field.get();
        Assert.checkState(blob != null);

        return createIterable(blob.getBeginPosition(), blob.getEndPosition(), false, true);
    }

    @Override
    public IStructuredIterable getRecords(long startId, long endId) {
        boolean includeEnd = true;
        IBlob blob = field.get();
        if (endId == 0 || endId == blob.getEndPosition()) {
            endId = blob.getEndPosition();
            includeEnd = false;
        }

        check(startId);
        check(endId);

        return createIterable(startId, endId, includeEnd, true);
    }

    @Override
    public IStructuredIterable getReverseRecords() {
        IBlob blob = field.get();
        Assert.checkState(blob != null);

        long currentId = getCurrentId();
        if (currentId != 0)
            return createIterable(getCurrentId(), blob.getBeginPosition(), true, false);
        else
            return createIterable(0, 0, false, true);
    }

    @Override
    public IStructuredIterable getReverseRecords(long startId, long endId) {
        IBlob blob = field.get();
        long currentId = getCurrentId();
        if (currentId == 0)
            return createIterable(0, 0, false, true);

        if (endId == 0)
            endId = blob.getBeginPosition();

        check(startId);
        check(endId);

        return createIterable(startId, endId, true, false);
    }

    @Override
    public final StructuredBlobIndex getIndex(int index) {
        field.getField().refresh();

        return indexes[index];
    }

    @Override
    public final INodeFullTextIndex getFullTextIndex() {
        field.getField().refresh();

        return fullTextIndex;
    }

    @Override
    public long add(T record) {
        Assert.notNull(record);
        checkClass(record);

        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();
        long id = serialization.getPosition();

        write(serialization, record, getCurrentId());

        if (recordIndexer != null)
            recordIndexer.addRecord(record, id);

        current = record;

        IRawWriteRegion region = field.getField().getWriteRegion();
        region.writeLong(LAST_RECORD_ID_OFFSET, id);

        setModified();

        return id;
    }

    @Override
    public void set(long id, T record) {
        Assert.notNull(record);
        checkClass(record);

        StructuredBlobFieldSchemaConfiguration configuration = (StructuredBlobFieldSchemaConfiguration) getSchema().getConfiguration();
        Assert.isTrue(configuration.getFixedRecord());

        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!isReadOnly());

        if (recordIndexer != null)
            recordIndexer.removeRecord(get(id));

        IBlobSerialization serialization = blob.createSerialization();
        serialization.setPosition(id);

        write(serialization, record, -1);

        if (recordIndexer != null)
            recordIndexer.addRecord(record, id);

        if (id == getCurrentId())
            current = record;

        setModified();
    }

    @Override
    public final void clear() {
        IBlob blob = field.get();
        Assert.supports(allowDeletion());
        Assert.checkState(blob != null);
        Assert.checkState(!field.isReadOnly());

        clearIndexes();

        IBlobSerialization serialization = blob.createSerialization();
        serialization.setPosition(serialization.getBeginPosition());
        serialization.removeRest();

        current = null;

        IRawWriteRegion region = field.getField().getWriteRegion();
        region.writeLong(LAST_RECORD_ID_OFFSET, 0l);

        setModified();
    }

    @Override
    public final IDocumentSchema createDocumentSchema(DocumentSchemaConfiguration schemaConfiguration) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        fields.add(new NumericFieldSchemaConfiguration(RECORD_ID_FIELD_NAME, DataType.LONG, true, true));
        fields.add(new StringFieldSchemaConfiguration(FIELD_ID_FIELD_NAME, Enums.of(Option.INDEXED, Option.INDEX_DOCUMENTS, Option.OMIT_NORMS),
                new StandardAnalyzerSchemaConfiguration()));
        fields.addAll(schemaConfiguration.getFields());
        return new DocumentSchemaConfiguration("default", fields, schemaConfiguration.getAnalyzer(), 2).createSchema();
    }

    @Override
    public final void add(int index, Object key, long id) {
        indexes[index].add(key, id);
    }

    @Override
    public final void remove(int index, Object key) {
        indexes[index].remove(key);
    }

    @Override
    public final void add(IDocumentSchema schema, long id, Object... values) {
        Assert.notNull(schema);
        Assert.notNull(values);
        Assert.checkState(fullTextIndex != null);

        List<Object> fields = new ArrayList<Object>(values.length + 2);
        fields.add(id);
        fields.add(fieldId);
        for (int i = 0; i < values.length; i++)
            fields.add(values[i]);

        fullTextIndex.getIndex().add(schema.createDocument(this, fields));
    }

    @Override
    public final void write(IDataSerialization serialization, IDocument document) {
        long id = ((INumericField) document.getFields().get(0)).get().longValue();
        serialization.writeLong(id);
    }

    @Override
    public final void readAndReindex(IDataDeserialization deserialization) {
        Assert.checkState(recordIndexer != null);

        long id = deserialization.readLong();

        Object record = get(id);
        recordIndexer.reindex(record, id);
    }

    protected IStructuredIterable createIterable(long startId, long endId, boolean includeEnd, boolean direct) {
        if (startId != 0)
            return new StructuredIterable(startId, endId, includeEnd, direct);
        else
            return new StructuredIterable();
    }

    protected void checkClass(Object record) {
        StructuredBlobFieldSchema schema = (StructuredBlobFieldSchema) getSchema();

        Assert.isTrue(schema.getAllowedClasses() == null || schema.getAllowedClasses().contains(record.getClass()));
    }

    protected Object doRead(IDataDeserialization fieldDeserialization) {
        StructuredBlobFieldSchemaConfiguration configuration = ((StructuredBlobFieldSchemaConfiguration) getSchema().getConfiguration());

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

        ISerializationRegistry registry = ((StructuredBlobFieldSchema) field.getSchema()).getSerializationRegistry();
        ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        Deserialization deserialization = new Deserialization(registry, stream);

        StructuredBlobFieldSchema schema = (StructuredBlobFieldSchema) getSchema();
        if (schema.getMainClass() != null)
            return deserialization.readTypedObject(schema.getMainClass());
        else
            return deserialization.readObject();
    }

    protected void doWrite(IDataSerialization fieldSerialization, Object record) {
        StructuredBlobFieldSchemaConfiguration configuration = ((StructuredBlobFieldSchemaConfiguration) getSchema().getConfiguration());

        ByteOutputStream stream = new ByteOutputStream();
        ISerializationRegistry registry = ((StructuredBlobFieldSchema) field.getSchema()).getSerializationRegistry();
        Serialization serialization = new Serialization(registry, false, stream);

        StructuredBlobFieldSchema schema = (StructuredBlobFieldSchema) getSchema();
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

    protected final void check(long id) {
        IBlob blob = field.get();
        Assert.checkState(blob != null);
        IBlobDeserialization deserialization = blob.createDeserialization();
        if (id == deserialization.getEndPosition())
            return;
        deserialization.setPosition(id);
        if (deserialization.readByte() != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getBlob()));
    }

    private Object read(IBlobDeserialization fieldDeserialization, boolean direct) {
        field.getField().refresh();

        if (fieldDeserialization.readByte() != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fieldDeserialization.getBlob()));

        long prevRecordId = fieldDeserialization.readLong();

        Object result = doRead(fieldDeserialization);

        if (!direct && prevRecordId != 0)
            fieldDeserialization.setPosition(prevRecordId);

        return result;
    }

    private void write(IBlobSerialization fieldSerialization, Object record, long prevRecordId) {
        field.getField().refresh();

        if (prevRecordId != -1) {
            fieldSerialization.writeByte(MAGIC);
            fieldSerialization.writeLong(prevRecordId);
        } else {
            fieldSerialization.readByte();
            fieldSerialization.readLong();
        }

        doWrite(fieldSerialization, record);

        if (prevRecordId != -1)
            fieldSerialization.updateEndPosition();
    }

    private void clearIndexes() {
        if (recordIndexer != null && indexes.length != 0) {
            for (Object record : getRecords())
                recordIndexer.removeRecord(record);
        }

        if (fullTextIndex != null)
            fullTextIndex.getIndex().remove(FIELD_ID_FIELD_NAME, fieldId);
        modCount++;
    }

    private IDatabaseContext getContext() {
        NodeSpaceSchema spaceSchema = ((NodeSpaceSchema) field.getSchema().getParent().getParent());
        return spaceSchema.getContext();
    }

    private StructuredBlobIndex createNodeIndex(IUniqueIndex index, StructuredBlobIndexSchemaConfiguration configuration) {
        if (configuration.isSorted()) {
            if (index instanceof INonUniqueSortedIndex)
                return new StructuredBlobNonUniqueSortedIndex(configuration, getContext(), (INonUniqueSortedIndex) index, this);
            else if (index instanceof ISortedIndex)
                return new StructuredBlobSortedIndex(configuration, getContext(), (ISortedIndex) index, this);
            else
                return Assert.error();
        } else
            return new StructuredBlobIndex(configuration, getContext(), index, this);
    }

    @SuppressWarnings("hiding")
    protected class StructuredIterable<T> implements IStructuredIterable<T> {
        protected final long startId;
        protected final long endId;
        protected final boolean includeEnd;
        protected final boolean direct;

        public StructuredIterable() {
            startId = 0;
            endId = 0;
            includeEnd = false;
            direct = true;
        }

        public StructuredIterable(long startId, long endId, boolean includeEnd, boolean direct) {
            this.startId = startId;
            this.endId = endId;
            this.includeEnd = includeEnd;
            this.direct = direct;
        }

        @Override
        public IStructuredIterator iterator() {
            if (startId != 0)
                return new StructuredIterator(startId, endId, includeEnd, direct);
            else
                return new StructuredIterator();
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

    protected class StructuredIterator implements IStructuredIterator<T> {
        private final long startId;
        private final long endId;
        private final IBlobDeserialization deserialization;
        private final boolean direct;
        private long id = -1;
        private T record;
        private T prevRecord;
        private final int modCount;
        private final boolean includeEnd;
        private boolean last;

        public StructuredIterator() {
            startId = 0;
            endId = 0;
            deserialization = null;
            direct = true;
            modCount = 0;
            includeEnd = false;
            last = true;
        }

        public StructuredIterator(long startId, long endId, boolean includeEnd, boolean direct) {
            this.startId = startId;
            this.endId = endId;
            this.includeEnd = includeEnd;
            this.direct = direct;

            if (startId != endId || includeEnd) {
                IBlob blob = field.get();
                deserialization = blob.createDeserialization();
                deserialization.setPosition(startId);
                modCount = StructuredBlobField.this.modCount;
            } else {
                deserialization = null;
                modCount = 0;
                last = true;
            }
        }

        @Override
        public boolean hasNext() {
            return !last;
        }

        @Override
        public T next() {
            Assert.checkState(deserialization != null && !last);

            if (modCount != StructuredBlobField.this.modCount)
                throw new ConcurrentModificationException();

            id = deserialization.getPosition();

            if (!includeEnd)
                Assert.checkState(id != endId);

            prevRecord = record;
            record = (T) read(deserialization, direct);

            if (includeEnd) {
                if (id == endId)
                    last = true;
            } else if (deserialization.getPosition() == endId)
                last = true;

            return record;
        }

        @Override
        public IStructuredBlobField<T> getField() {
            return StructuredBlobField.this;
        }

        @Override
        public long getStartId() {
            return startId;
        }

        @Override
        public long getEndId() {
            return endId;
        }

        @Override
        public long getId() {
            Assert.checkState(id != -1);
            return id;
        }

        @Override
        public void setNext(long id) {
            Assert.checkState(deserialization != null);
            check(id);
            deserialization.setPosition(id);

            if (!includeEnd && deserialization.getPosition() == endId)
                last = true;
            else
                last = false;
        }

        @Override
        public T get() {
            Assert.checkState(record != null);
            return record;
        }

        @Override
        public T getPrevious() {
            return prevRecord;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of blob ''{0}''.")
        ILocalizedMessage invalidFormat(IBlob blob);
    }
}
