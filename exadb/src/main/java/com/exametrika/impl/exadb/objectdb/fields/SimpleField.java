/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.schema.SimpleFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link SimpleField} is a simple inline node field, which contained in node's field table.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SimpleField implements ISimpleField, IFieldObject {
    protected final Node node;
    protected final SimpleFieldSchema schema;
    protected IFieldObject object;
    private boolean readOnly;

    public static SimpleField create(Node node, int fieldIndex, Object primaryKey, Object initalizer) {
        Assert.notNull(node);

        SimpleFieldSchema schema = (SimpleFieldSchema) node.getSchema().getFields().get(fieldIndex);

        SimpleField field = new SimpleField(node, schema);

        field.object = schema.createField(field);
        field.onCreated(primaryKey, initalizer);

        return field;
    }

    public static SimpleField open(Node node, int fieldIndex, boolean create, Object primaryKey, Object initializer) {
        Assert.notNull(node);

        SimpleFieldSchema schema = (SimpleFieldSchema) node.getSchema().getFields().get(fieldIndex);

        SimpleField field = new SimpleField(node, schema);

        field.object = schema.createField(field);
        if (create)
            field.onCreated(primaryKey, initializer);
        else
            field.onOpened();

        return field;
    }

    @Override
    public final boolean isReadOnly() {
        return readOnly || node.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return node.allowFieldDeletion();
    }

    @Override
    public final IFieldSchema getSchema() {
        return schema;
    }

    @Override
    public final Node getNode() {
        return node;
    }

    @Override
    public final <T> T getObject() {
        return (T) object;
    }

    @Override
    public <T> T get() {
        return null;
    }

    @Override
    public IRawReadRegion getReadRegion() {
        return node.getHeaderPage().getReadRegion().getRegion(node.getHeaderOffset() + schema.getOffset(), schema.getConfiguration().getSize());
    }

    @Override
    public IRawWriteRegion getWriteRegion() {
        return node.getHeaderPage().getWriteRegion().getRegion(node.getHeaderOffset() + schema.getOffset(), schema.getConfiguration().getSize());
    }

    @Override
    public void refresh() {
        node.refresh();
    }

    @Override
    public final void setModified() {
        Assert.supports(!readOnly);

        node.setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        readOnly = false;

        if (object != this)
            object.onCreated(primaryKey, initializer);

        readOnly = schema.getConfiguration().isPrimary();
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
        if (object != this)
            object.onAfterCreated(primaryKey, initializer);
    }

    @Override
    public void onOpened() {
        if (object != this)
            object.onOpened();

        readOnly = schema.getConfiguration().isPrimary();
    }

    @Override
    public void onDeleted() {
        if (object != this)
            object.onDeleted();
    }

    @Override
    public void onUnloaded() {
        if (object != this)
            object.onUnloaded();
    }

    @Override
    public void flush() {
        if (object != this)
            object.flush();
    }

    protected SimpleField(Node node, SimpleFieldSchema schema) {
        Assert.notNull(node);
        Assert.notNull(schema);

        this.node = node;
        this.schema = schema;
    }
}
