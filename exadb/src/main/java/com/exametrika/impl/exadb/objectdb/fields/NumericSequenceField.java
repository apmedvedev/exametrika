/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.INumericSequenceField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.impl.exadb.objectdb.schema.NumericSequenceFieldSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link NumericSequenceField} is a numeric sequence node field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NumericSequenceField implements INumericSequenceField, IFieldObject {
    private final ISimpleField field;
    private boolean modified;
    private long value;
    private long lastResetTime;

    public NumericSequenceField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;
        value = field.getReadRegion().readLong(0);
        lastResetTime = field.getReadRegion().readLong(8);
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

    @Override
    public void setLong(long value) {
        set(value, true);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        NumericSequenceFieldSchema schema = (NumericSequenceFieldSchema) field.getSchema();
        set(schema.getConfiguration().getInitialValue(), true);
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
        field.getWriteRegion().writeLong(8, lastResetTime);
        modified = false;
    }

    @Override
    public void onDeleted() {
        modified = false;
    }

    @Override
    public long getNext() {
        NumericSequenceFieldSchema schema = (NumericSequenceFieldSchema) field.getSchema();

        boolean reset = false;
        if (schema.getPeriod() != null) {
            long currentTime = getCurrentTime();
            reset = schema.getPeriod().evaluate(lastResetTime, currentTime);
        }

        long res;
        if (reset)
            res = schema.getConfiguration().getInitialValue();
        else
            res = value;

        long newValue = res + schema.getConfiguration().getStep();

        set(newValue, reset);
        return res;
    }

    private void set(long value, boolean resetTime) {
        Assert.checkState(!isReadOnly());

        this.value = value;
        if (resetTime)
            this.lastResetTime = getCurrentTime();
        setModified();
    }

    private long getCurrentTime() {
        NodeSpaceSchema spaceSchema = ((NodeSpaceSchema) field.getSchema().getParent().getParent());
        IDatabaseContext context = spaceSchema.getContext();
        return context.getTimeService().getCurrentTime();
    }
}
