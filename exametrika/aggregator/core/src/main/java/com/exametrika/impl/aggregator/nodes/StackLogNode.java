/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackLogNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IStackLogNodeSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link StackLogNode} is an stack log node.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StackLogNode extends AggregationNode implements IStackLogNode {
    protected static final int MAIN_NODE_FIELD = 2;

    public StackLogNode(INode node) {
        super(node);
    }

    @Override
    public IStackLogNodeSchema getSchema() {
        return (IStackLogNodeSchema) super.getSchema();
    }

    @Override
    public ICallPath getMetric() {
        return (ICallPath) super.getMetric();
    }

    @Override
    public String getNodeType() {
        return isDerived() ? "logDerived" : "log";
    }

    @Override
    public IStackNode getMainNode() {
        ISingleReferenceField<IStackNode> main = getField(MAIN_NODE_FIELD);
        return main.get();
    }

    public void setMainNode(IStackNode node) {
        Assert.notNull(node);

        ISingleReferenceField<IStackNode> main = getField(MAIN_NODE_FIELD);
        Assert.checkState(main.get() == null);
        main.set(node);
    }

    @Override
    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
        super.init(nameManager, metadata, aggregatingPeriod);

        IPeriod period = getPeriod();
        RootNode root = period.getRootNode();
        root.addLog(this);
    }

    public void resolveReference() {
        if (getMainNode() != null)
            return;

        ICycleSchema cycleSchema = getSpace().getSchema();
        IStackLogNodeSchema schema = getSchema();
        IPeriod period = getPeriod();

        IJsonField metadataField = getField(schema.getAggregationField().getMetadataFieldIndex());
        JsonObject metadata = metadataField.get();
        if (metadata == null)
            return;

        String mainComponentType = (String) metadata.get("entry", null);
        if (mainComponentType == null && schema.getBackgroundRoot() != null)
            mainComponentType = schema.getBackgroundRoot().getConfiguration().getComponentType().getName();
        if (mainComponentType == null)
            return;
        IAggregationNodeSchema mainNodeSchema = cycleSchema.findAggregationNode(mainComponentType);
        if (mainNodeSchema == null)
            return;

        INodeIndex<Location, IStackNode> index = period.getIndex(mainNodeSchema.getPrimaryField());
        IStackNode mainNode = index.find(getLocation());
        if (mainNode instanceof IEntryPointNode)
            ((IEntryPointNode) mainNode).addLog(this);
        else if (mainNode != null)
            Assert.error();
    }
}