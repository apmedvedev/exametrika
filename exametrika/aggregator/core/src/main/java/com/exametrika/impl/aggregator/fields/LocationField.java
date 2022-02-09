/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.fields;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.fields.ILocationField;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryField;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link LocationField} is a location node field, which contained in node's field table.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class LocationField implements ILocationField, IPrimaryField, IFieldObject {
    private final ISimpleField field;
    private Location value;

    public LocationField(ISimpleField field) {
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
    public Location get() {
        if (value != null)
            return value;
        else
            return readValue();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.notNull(primaryKey);
        IFieldSchema schema = getSchema();
        Assert.checkState(schema.getConfiguration().isPrimary());
        value = (Location) primaryKey;

        INode node = getNode();
        ((NodeSpace) node.getSpace()).addIndexValue(schema, value, node, true, true);
        writeValue();
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        IFieldSchema schema = getSchema();
        INode node = getNode();
        ((NodeSpace) node.getSpace()).addIndexValue(schema, get(), node, false, true);
    }

    @Override
    public void onDeleted() {
        Assert.supports(false);
    }

    @Override
    public void onUnloaded() {
        IFieldSchema schema = getSchema();
        INode node = getNode();
        ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
        value = null;
    }

    @Override
    public void flush() {
    }

    @Override
    public Object getKey() {
        return get();
    }

    private Location readValue() {
        IRawReadRegion region = field.getReadRegion();
        long scopeId = region.readLong(0);
        long locationId = region.readLong(8);

        value = new Location(scopeId, locationId);
        return value;
    }

    private void writeValue() {
        IRawWriteRegion region = field.getWriteRegion();
        region.writeLong(0, value.getScopeId());
        region.writeLong(8, value.getMetricId());
    }
}
