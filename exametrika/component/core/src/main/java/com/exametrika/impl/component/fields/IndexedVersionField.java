/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link IndexedVersionField} is a indexed version node field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexedVersionField implements IField, IFieldObject {
    private final ISimpleField field;

    public IndexedVersionField(ISimpleField field) {
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
        return null;
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
    public void onCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onDeleted() {
    }

    @Override
    public void onUnloaded() {
    }

    @Override
    public void flush() {
    }

    public void set(long nodeId, long time) {
        IFieldSchema schema = getSchema();
        INode node = getNode();
        ((NodeSpace) node.getSpace()).addIndexValue(schema, new VersionTime(nodeId, time), node, true, false);
    }
}
