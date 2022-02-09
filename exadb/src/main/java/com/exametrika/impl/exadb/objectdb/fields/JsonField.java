/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Objects;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IJsonFieldSchema;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;


/**
 * The {@link JsonField} is a JSON field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JsonField implements IJsonField, IFieldObject {
    private final IComplexField field;
    private IJsonCollection value;
    private boolean modified;
    private int lastCacheSize;

    public JsonField(IComplexField field) {
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
    public IJsonCollection get() {
        if (value != null)
            return value;
        else
            return readValue();
    }

    @Override
    public void set(IJsonCollection value) {
        Assert.checkState(!field.isReadOnly());
        get();

        if (Objects.equals(this.value, value))
            return;

        if (value != null) {
            IJsonFieldSchema schema = (IJsonFieldSchema) field.getSchema();
            schema.validate(value);
        }

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

        writeValue(false);

        modified = false;
    }

    private IJsonCollection readValue() {
        JsonFieldSchemaConfiguration configuration = (JsonFieldSchemaConfiguration) getSchema().getConfiguration();
        IFieldDeserialization fieldDeserialization = field.createDeserialization();

        if (!configuration.isCompressed())
            value = JsonSerializers.deserialize(fieldDeserialization);
        else {
            int decompressedLength = fieldDeserialization.readInt();
            if (decompressedLength != 0) {
                ByteArray compressedBuffer = fieldDeserialization.readByteArray();

                ByteArray buffer = LZ4.decompress(compressedBuffer, decompressedLength);

                ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
                DataDeserialization dataDeserialization = new DataDeserialization(stream);
                value = JsonSerializers.deserialize(dataDeserialization);
            } else
                value = null;
        }

        updateCacheSize();
        return value;
    }

    private void writeValue(boolean create) {
        JsonFieldSchemaConfiguration configuration = (JsonFieldSchemaConfiguration) getSchema().getConfiguration();
        IFieldSerialization fieldSerialization = field.createSerialization();

        if (!configuration.isCompressed())
            JsonSerializers.serialize(fieldSerialization, value);
        else if (value != null) {
            ByteOutputStream stream = new ByteOutputStream();
            DataSerialization dataSerialization = new DataSerialization(stream);
            JsonSerializers.serialize(dataSerialization, value);

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
            cacheSize = ((ICacheable) value).getCacheSize();

        if (cacheSize != lastCacheSize) {
            getNode().updateCacheSize(cacheSize - lastCacheSize);
            lastCacheSize = cacheSize;
        }
    }
}
