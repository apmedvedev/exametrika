/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.objectdb.INodeLoader;
import com.exametrika.impl.exadb.objectdb.Node;


/**
 * The {@link NodeObject} is an abstract node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class NodeObject implements INodeObject {
    private final long id;
    private INodeLoader nodeLoader;
    private Node node;

    public NodeObject(INode n) {
        Node node = (Node) n;
        Assert.notNull(node);

        this.node = node;
        this.id = node.getId();
        if (node.isCached())
            this.nodeLoader = node.getNodeLoader();
        else
            this.nodeLoader = null;
    }

    public boolean isLoaded() {
        return node != null;
    }

    @Override
    public boolean isStale() {
        return node == null && nodeLoader == null;
    }

    public long getId() {
        return id;
    }

    @Override
    public INode getNode() {
        if (node != null && !node.isStale())
            return node;
        else
            return refreshNode();
    }

    public void init(INode node) {
        Assert.notNull(node);
        Assert.checkState(this.node == null);

        this.node = (Node) node;
    }

    public void setUnloaded() {
        node = null;
    }

    public void setStale() {
        nodeLoader = null;
        node = null;
    }

    @Override
    public boolean allowModify() {
        return true;
    }

    @Override
    public boolean allowDeletion() {
        return true;
    }

    @Override
    public final void validate() {
    }

    @Override
    public void onBeforeCreated(Object primaryKey, Object[] args, Object[] fieldInitializers) {
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onBeforeMigrated(Object primaryKey) {
    }

    @Override
    public void onMigrated() {
    }

    @Override
    public void onDeleted() {
    }

    @Override
    public void onUnloaded() {
    }

    @Override
    public void onBeforeFlush() {
    }

    @Override
    public void onAfterFlush() {
    }

    public void refresh() {
        getNode();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        INode node = getNode();

        if ((context.getFlags() & IDumpContext.DUMP_ORPHANED) != 0)
            ((DumpContext) context).traverseNode(node.getId());
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeObject))
            return false;

        NodeObject n = (NodeObject) o;
        return id == n.id;
    }

    @Override
    public final int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return getNode().toString();
    }

    private INode refreshNode() {
        Assert.checkState(nodeLoader != null);
        node = nodeLoader.loadNode(id, this);
        Assert.checkState(node.getObject() == this);
        return node;
    }
}