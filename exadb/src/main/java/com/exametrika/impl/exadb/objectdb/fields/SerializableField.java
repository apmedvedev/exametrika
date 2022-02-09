/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Objects;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.impl.exadb.objectdb.schema.SerializableFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;


/**
 * The {@link SerializableField} is a serializable field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SerializableField implements ISerializableField, IFieldObject {
    private final IComplexField field;
    private Object value;
    private boolean modified;
    private int lastCacheSize;

    public SerializableField(IComplexField field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public IFieldSchema getSchema() {
        return field.getSchema();
    }

    @Override
    public INode getNode() {
        return field.getNode();
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        modified = true;
        field.setModified();
    }

    @Override
    public Object get() {
        if (value != null)
            return value;
        else
            return readValue();
    }

    @Override
    public void set(Object value) {
        Assert.checkState(!field.isReadOnly());
        get();

        if (Objects.equals(this.value, value))
            return;

        SerializableFieldSchema schema = (SerializableFieldSchema) getSchema();
        Assert.isTrue(schema.getAllowedClasses() == null || schema.getAllowedClasses().contains(value.getClass()));

        this.value = value;
        setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onDeleted() {
        modified = false;
        value = null;
    }

    @Override
    public void onUnloaded() {
        value = null;
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        writeValue();

        modified = false;
    }

    private Object readValue() {
        SerializableFieldSchemaConfiguration configuration = (SerializableFieldSchemaConfiguration) getSchema().getConfiguration();

        IFieldDeserialization fieldDeserialization = field.createDeserialization();
        ByteArray buffer;
        if (!configuration.isCompressed())
            buffer = fieldDeserialization.readByteArray();
        else {
            int decompressedLength = fieldDeserialization.readInt();
            if (decompressedLength > 0) {
                ByteArray compressedBuffer = fieldDeserialization.readByteArray();
                buffer = LZ4.decompress(compressedBuffer, decompressedLength);
            } else
                buffer = null;
        }

        if (buffer != null) {
            ISerializationRegistry registry = ((SerializableFieldSchema) field.getSchema()).getSerializationRegistry();
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(registry, stream);
            value = deserialization.readObject();
        }

        updateCacheSize(buffer != null ? buffer.getLength() : 0);

        return value;
    }

    private void writeValue() {
        SerializableFieldSchemaConfiguration configuration = (SerializableFieldSchemaConfiguration) getSchema().getConfiguration();

        ByteArray buffer;
        if (value != null) {
            ByteOutputStream stream = new ByteOutputStream();
            ISerializationRegistry registry = ((SerializableFieldSchema) field.getSchema()).getSerializationRegistry();
            Serialization serialization = new Serialization(registry, true, stream);
            serialization.writeObject(value);

            buffer = new ByteArray(stream.getBuffer(), 0, stream.getLength());
        } else
            buffer = null;

        IFieldSerialization fieldSerialization = field.createSerialization();
        if (!configuration.isCompressed())
            fieldSerialization.writeByteArray(buffer);
        else if (buffer != null && buffer.getLength() > 0) {
            ByteArray compressedBuffer = LZ4.compress(true, buffer);

            fieldSerialization.writeInt(buffer.getLength());
            fieldSerialization.writeByteArray(compressedBuffer);
        } else
            fieldSerialization.writeInt(0);

        if (field.allowDeletion())
            fieldSerialization.removeRest();

        updateCacheSize(buffer != null ? buffer.getLength() : 0);
    }

    private void updateCacheSize(int cacheSize) {
        if (value != null) {
            if (value instanceof ICacheable)
                cacheSize = ((ICacheable) value).getCacheSize();
        }

        if (cacheSize != lastCacheSize) {
            getNode().updateCacheSize(cacheSize - lastCacheSize);
            lastCacheSize = cacheSize;
        }
    }
}
