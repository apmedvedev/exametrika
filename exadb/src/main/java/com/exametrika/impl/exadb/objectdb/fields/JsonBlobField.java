/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IJsonBlobField;
import com.exametrika.api.exadb.objectdb.fields.IJsonRecord;
import com.exametrika.api.exadb.objectdb.schema.IJsonBlobFieldSchema;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link JsonBlobField} is a json blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JsonBlobField extends StructuredBlobField<IJsonRecord> implements IJsonBlobField {
    public JsonBlobField(ISimpleField field) {
        super(field);
    }

    @Override
    public IJsonBlobFieldSchema getSchema() {
        return (IJsonBlobFieldSchema) super.getSchema();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        BlobFieldInitializer blobFieldInitializer = new BlobFieldInitializer();
        blobFieldInitializer.setStore(((Node) getNode()).getRootNode());
        super.onCreated(primaryKey, blobFieldInitializer);
    }

    @Override
    public void setStore(Object store) {
        Assert.supports(false);
    }

    @Override
    protected void checkClass(Object record) {
        Assert.isTrue(record instanceof JsonRecord);
    }

    @Override
    protected Object doRead(IDataDeserialization fieldDeserialization) {
        JsonObject value = JsonSerializers.deserialize(fieldDeserialization);
        return new JsonRecord(value);
    }

    @Override
    protected void doWrite(IDataSerialization fieldSerialization, Object r) {
        JsonRecord record = (JsonRecord) r;
        JsonSerializers.serialize(fieldSerialization, record.getValue());
    }
}
