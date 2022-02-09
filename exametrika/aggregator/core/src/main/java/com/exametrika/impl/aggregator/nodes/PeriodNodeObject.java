/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.IPeriodSpace;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.schema.IPeriodNodeSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.objectdb.NodeObject;


/**
 * The {@link PeriodNodeObject} is a period node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class PeriodNodeObject extends NodeObject implements IPeriodNode {
    public PeriodNodeObject(INode node) {
        super(node);
    }

    @Override
    public IPeriodNode getNode() {
        return (IPeriodNode) super.getNode();
    }

    @Override
    public IPeriodSpace getSpace() {
        return getNode().getSpace();
    }

    @Override
    public IPeriod getPeriod() {
        return getNode().getPeriod();
    }

    @Override
    public Location getLocation() {
        return getNode().getLocation();
    }

    @Override
    public IScopeName getScope() {
        return getNode().getScope();
    }

    @Override
    public IMetricLocation getMetric() {
        return getNode().getMetric();
    }

    @Override
    public <T> T getPreviousPeriodNode() {
        return getNode().getPreviousPeriodNode();
    }

    @Override
    public <T> Iterable<Pair<IPeriod, T>> getPeriodNodes() {
        return getNode().getPeriodNodes();
    }

    @Override
    public boolean allowDeletion() {
        return false;
    }

    @Override
    public boolean allowFieldDeletion() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return getNode().isReadOnly();
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public boolean isModified() {
        return getNode().isModified();
    }

    @Override
    public void setModified() {
        Assert.supports(false);
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
        Assert.supports(false);
    }

    @Override
    public void updateCacheSize(int delta) {
        Assert.supports(false);
    }

    @Override
    public IPeriodNodeSchema getSchema() {
        return getNode().getSchema();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("id");
        json.value(getId() + "@" + getPeriod().toString());
        json.key("scope");
        json.value(getScope());
        json.key("metric");
        json.value(getMetric());
    }

    protected String getRefId(IPeriodNode node) {
        return "scope:" + node.getScope().toString() + ", location:" + node.getMetric().toString() +
                " (" + node.getId() + "@" + node.getPeriod().toString() + ")";
    }
}