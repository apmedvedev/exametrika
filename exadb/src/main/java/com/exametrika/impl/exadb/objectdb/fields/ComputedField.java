/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Map;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IComputedField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.schema.ComputedFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link ComputedField} is a computed inline node field, which contained in node's field table.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ComputedField implements IComputedField, IFieldObject {
    private final ISimpleField field;

    public ComputedField(ISimpleField field) {
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
        field.setModified();
    }

    @Override
    public <T> T get() {
        ComputedFieldSchema schema = (ComputedFieldSchema) getSchema();
        return schema.execute(getNode());
    }

    @Override
    public <T> T execute(Map<String, ? extends Object> variables) {
        ComputedFieldSchema schema = (ComputedFieldSchema) getSchema();
        return schema.execute(getNode(), variables);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onUnloaded() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void onDeleted() {
    }
}
