/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.BodyFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.objectdb.schema.BodyFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;
import com.exametrika.spi.exadb.objectdb.fields.INodeBody;


/**
 * The {@link BodyField} is a body field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BodyField implements IField, IFieldObject {
    private final IComplexField field;
    private boolean modified;

    public BodyField(IComplexField field) {
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
    public <T> T get() {
        return getNode().getObject();
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
    }

    @Override
    public void onUnloaded() {
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        writeValue();

        modified = false;
    }

    public INodeBody readValue() {
        BodyFieldSchemaConfiguration configuration = (BodyFieldSchemaConfiguration) getSchema().getConfiguration();

        IFieldDeserialization fieldDeserialization = field.createDeserialization();
        ByteArray buffer;
        if (!configuration.isCompressed())
            buffer = fieldDeserialization.readByteArray();
        else {
            int decompressedLength = fieldDeserialization.readInt();
            ByteArray compressedBuffer = fieldDeserialization.readByteArray();
            buffer = LZ4.decompress(compressedBuffer, decompressedLength);
        }

        ISerializationRegistry registry = ((BodyFieldSchema) field.getSchema()).getSerializationRegistry();
        ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        Deserialization deserialization = new Deserialization(registry, stream);
        return deserialization.readObject();
    }

    private void writeValue() {
        BodyFieldSchemaConfiguration configuration = (BodyFieldSchemaConfiguration) getSchema().getConfiguration();

        INodeBody body = getNode().getObject();
        ByteOutputStream stream = new ByteOutputStream();
        ISerializationRegistry registry = ((BodyFieldSchema) field.getSchema()).getSerializationRegistry();
        Serialization serialization = new Serialization(registry, true, stream);
        serialization.writeObject(body);

        ByteArray buffer = new ByteArray(stream.getBuffer(), 0, stream.getLength());

        IFieldSerialization fieldSerialization = field.createSerialization();
        if (!configuration.isCompressed())
            fieldSerialization.writeByteArray(buffer);
        else {
            ByteArray compressedBuffer = LZ4.compress(true, buffer);

            fieldSerialization.writeInt(buffer.getLength());
            fieldSerialization.writeByteArray(compressedBuffer);
        }

        if (field.allowDeletion())
            fieldSerialization.removeRest();
    }
}
