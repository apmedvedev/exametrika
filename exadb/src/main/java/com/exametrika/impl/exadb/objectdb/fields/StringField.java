/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Objects;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.fields.IStringSequenceField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.schema.StringFieldSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IFullTextField;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryField;


/**
 * The {@link StringField} is a string field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StringField implements IStringField, IPrimaryField, IFullTextField, IFieldObject {
    private final IComplexField field;
    private String value;
    private boolean modified;
    private int lastCacheSize;

    public StringField(IComplexField field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public boolean isModified() {
        return modified;
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
    public String get() {
        if (value != null)
            return value;
        else
            return readValue();
    }

    @Override
    public void set(String value) {
        IFieldSchema schema = getSchema();
        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) schema.getConfiguration();

        Assert.checkState(!field.isReadOnly());
        Assert.isTrue(value == null || (value.length() >= configuration.getMinSize() && value.length() <= configuration.getMaxSize()));
        get();

        if (Objects.equals(this.value, value))
            return;

        String oldValue = this.value;
        this.value = value;
        setModified();

        INode node = getNode();
        if (configuration.isIndexed())
            ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        StringFieldSchema schema = (StringFieldSchema) getSchema();
        StringFieldSchemaConfiguration configuration = schema.getConfiguration();

        if (configuration.isPrimary() || schema.getSequenceField() != null) {
            Node node = (Node) getNode();

            if (primaryKey == null && schema.getSequenceField() != null) {
                IStringSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                primaryKey = sequenceField.getNextString();
            }

            Assert.notNull(primaryKey);

            String value = (String) primaryKey;
            Assert.isTrue(value.length() >= configuration.getMinSize() && value.length() <= configuration.getMaxSize());

            this.value = value;
            writeValue(true);

            if (configuration.isIndexed())
                node.getSpace().addIndexValue(schema, value, node, true, true);

            modified = false;
        }
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        IFieldSchema schema = getSchema();
        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) schema.getConfiguration();
        if (configuration.isCached()) {
            INode node = getNode();
            ((NodeSpace) node.getSpace()).addIndexValue(schema, get(), node, false, true);
        }
    }

    @Override
    public void onDeleted() {
        IFieldSchema schema = getSchema();
        if (schema.getConfiguration().isIndexed()) {
            INode node = getNode();
            ((NodeSpace) node.getSpace()).removeIndexValue(schema, get(), node, true, true);
        }

        modified = false;
        value = null;
    }

    @Override
    public void onUnloaded() {
        IFieldSchema schema = getSchema();
        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) schema.getConfiguration();
        if (configuration.isCached()) {
            INode node = getNode();
            ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
        }

        value = null;
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        writeValue(false);

        modified = false;
    }

    @Override
    public Object getKey() {
        return get();
    }

    @Override
    public Object getFullTextValue() {
        return get();
    }

    private String readValue() {
        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) getSchema().getConfiguration();
        IFieldDeserialization fieldDeserialization = field.createDeserialization();
        if (!configuration.isCompressed())
            value = fieldDeserialization.readString();
        else {
            int decompressedLength = fieldDeserialization.readInt();
            if (decompressedLength != 0) {
                ByteArray compressedBuffer = fieldDeserialization.readByteArray();

                ByteArray buffer = LZ4.decompress(compressedBuffer, decompressedLength);

                ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
                DataDeserialization dataDeserialization = new DataDeserialization(stream);
                value = dataDeserialization.readString();
            } else
                value = null;
        }

        updateCacheSize();

        return value;
    }

    private void writeValue(boolean create) {
        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) getSchema().getConfiguration();
        IFieldSerialization fieldSerialization = field.createSerialization();

        if (!configuration.isCompressed())
            fieldSerialization.writeString(value);
        else if (value != null) {
            ByteOutputStream stream = new ByteOutputStream();
            DataSerialization dataSerialization = new DataSerialization(stream);
            dataSerialization.writeString(value);

            ByteArray compressedBuffer = LZ4.compress(true, new ByteArray(stream.getBuffer(), 0, stream.getLength()));

            fieldSerialization.writeInt(stream.getLength());
            fieldSerialization.writeByteArray(compressedBuffer);
        } else
            fieldSerialization.writeInt(0);

        if (!create && field.allowDeletion())
            fieldSerialization.removeRest();

        updateCacheSize();
    }

    private void updateCacheSize() {
        int cacheSize = 0;
        if (value != null)
            cacheSize = CacheSizes.getStringCacheSize(value);

        if (cacheSize != lastCacheSize) {
            getNode().updateCacheSize(cacheSize - lastCacheSize);
            lastCacheSize = cacheSize;
        }
    }
}
