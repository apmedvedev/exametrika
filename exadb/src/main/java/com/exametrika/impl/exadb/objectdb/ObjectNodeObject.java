/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.NodeObject;


/**
 * The {@link ObjectNodeObject} is a period node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ObjectNodeObject extends NodeObject implements IObjectNode {
    public ObjectNodeObject(INode node) {
        super(node);
    }

    @Override
    public IObjectNode getNode() {
        return (IObjectNode) super.getNode();
    }

    @Override
    public IObjectSpace getSpace() {
        return getNode().getSpace();
    }

    @Override
    public boolean allowDeletion() {
        return true;
    }

    @Override
    public boolean allowFieldDeletion() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return getNode().isReadOnly();
    }

    @Override
    public boolean isDeleted() {
        return getNode().isDeleted();
    }

    @Override
    public boolean isModified() {
        return getNode().isModified();
    }

    @Override
    public void setModified() {
        getNode().setModified();
    }

    @Override
    public IRawTransaction getRawTransaction() {
        Assert.supports(false);
        return null;
    }

    @Override
    public ITransaction getTransaction() {
        return getNode().getTransaction();
    }

    @Override
    public int getCacheSize() {
        return getNode().getCacheSize();
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public int getFieldCount() {
        return getNode().getFieldCount();
    }

    @Override
    public <T> T getField(int index) {
        return getNode().getField(index);
    }

    @Override
    public <T> T getField(IFieldSchema schema) {
        return getNode().getField(schema);
    }

    @Override
    public void delete() {
        getNode().delete();
    }

    @Override
    public void updateCacheSize(int delta) {
        Assert.supports(false);
    }

    @Override
    public IObjectNodeSchema getSchema() {
        return getNode().getSchema();
    }

    @Override
    public Object getKey() {
        return getNode().getKey();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        if (json == null)
            return;

        if ((context.getFlags() & IDumpContext.DUMP_ID) != 0) {
            json.key("id");
            json.value(getId() + "@" + getSpace().toString());
        } else {
            json.key("space");
            json.value(getSpace().toString());
        }

        Object key = getKey();
        if (key != null) {
            json.key("key");
            json.value(key);
        }
    }

    protected String getRefId(IObjectNode node) {
        return node.getId() + "@" + node.getSpace().toString();
    }
}