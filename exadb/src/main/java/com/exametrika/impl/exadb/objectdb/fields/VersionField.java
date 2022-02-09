/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IVersionField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link VersionField} is a version inline node field, which contained in node's field table.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class VersionField implements IVersionField, IFieldObject {
    private final ISimpleField field;
    private boolean modified;
    private long value;

    public VersionField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;

        value = field.getReadRegion().readLong(0);
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
    public Long get() {
        return getLong();
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
    public long getLong() {
        return value;
    }

    public void set(long value) {
        Assert.checkState(!isReadOnly());
        get();

        if (this.value == value)
            return;

        this.value = value;
        setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        set(1);
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
        if (!modified)
            return;

        field.getWriteRegion().writeLong(0, value);
        modified = false;
    }

    @Override
    public void onDeleted() {
        modified = false;
    }
}
