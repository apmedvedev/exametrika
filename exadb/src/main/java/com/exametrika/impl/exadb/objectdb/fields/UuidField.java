/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.UUID;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IUuidField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryField;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link UuidField} is a UUID inline node field, which contained in node's field table.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class UuidField implements IUuidField, IPrimaryField, IFieldObject {
    private final ISimpleField field;
    private boolean modified;
    private UUID value;

    public UuidField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;

        long least = field.getReadRegion().readLong(0);
        long most = field.getReadRegion().readLong(8);
        value = new UUID(most, least);
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
    public UUID get() {
        return value;
    }

    @Override
    public void set(UUID value) {
        Assert.checkState(!isReadOnly());
        get();

        if (Objects.equals(this.value, value))
            return;

        UUID oldValue = this.value;
        this.value = value;
        setModified();

        IFieldSchema schema = getSchema();
        FieldSchemaConfiguration configuration = schema.getConfiguration();
        INode node = getNode();
        if (configuration.isIndexed())
            ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        IFieldSchema schema = field.getSchema();
        boolean primary = schema.getConfiguration().isPrimary();

        if (primary) {
            Assert.notNull(primaryKey);

            value = (UUID) primaryKey;
            field.getWriteRegion().writeLong(0, value.getLeastSignificantBits());
            field.getWriteRegion().writeLong(8, value.getMostSignificantBits());

            if (schema.getConfiguration().isIndexed()) {
                Node node = (Node) getNode();
                node.getSpace().addIndexValue(schema, value, node, true, true);
            }

            modified = false;
        } else
            set(new UUID(0, 0));
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        IFieldSchema schema = getSchema();
        FieldSchemaConfiguration configuration = schema.getConfiguration();
        if (configuration.isCached()) {
            INode node = getNode();
            ((NodeSpace) node.getSpace()).addIndexValue(schema, get(), node, false, true);
        }
    }

    @Override
    public void onUnloaded() {
        IFieldSchema schema = getSchema();
        if (schema.getConfiguration().isCached()) {
            INode node = getNode();
            ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
        }
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        field.getWriteRegion().writeLong(0, value.getLeastSignificantBits());
        field.getWriteRegion().writeLong(8, value.getMostSignificantBits());
        modified = false;
    }

    @Override
    public void onDeleted() {
        IFieldSchema schema = getSchema();
        if (schema.getConfiguration().isIndexed()) {
            INode node = getNode();
            ((NodeSpace) node.getSpace()).removeIndexValue(schema, get(), node, true, true);
        }

        modified = false;
    }

    @Override
    public Object getKey() {
        return get();
    }
}
