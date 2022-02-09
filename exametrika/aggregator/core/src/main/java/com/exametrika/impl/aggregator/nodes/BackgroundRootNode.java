/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.nodes.IBackgroundRootNode;
import com.exametrika.api.aggregator.nodes.IStackLogNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.schema.IStackNodeSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link BackgroundRootNode} is a background root stack node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BackgroundRootNode extends EntryPointNode implements IBackgroundRootNode {
    protected static final int ANOMALIES_FIELD = 11;

    public BackgroundRootNode(INode node) {
        super(node);
    }

    @Override
    public IStackLogNode getAnomalies() {
        ISingleReferenceField<IStackLogNode> anomalies = getField(ANOMALIES_FIELD);
        return anomalies.get();
    }

    public IStackLogNode ensureAnomalies() {
        ISingleReferenceField<IStackLogNode> anomaliesField = getField(ANOMALIES_FIELD);
        StackLogNode anomalies = (StackLogNode) anomaliesField.get();
        if (anomaliesField.get() == null) {
            IStackNodeSchema schema = getSchema();

            if (schema.isAllowAnomaliesCorrelation()) {
                IPeriod period = getPeriod();
                anomalies = period.createNode(getLocation(), schema.getAnomaliesNode());

                anomaliesField.set(anomalies);
                anomalies.setMainNode(this);
            }
        }

        return anomalies;
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
        super.onCreated(primaryKey, args);

        ISingleReferenceField<IStackNode> transactionRoot = getField(TRANSACTION_ROOT_FIELD);
        transactionRoot.set(this);
    }

    @Override
    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
        super.init(nameManager, metadata, aggregatingPeriod);

        ICallPath callPath = getMetric();
        Assert.checkState(callPath.isEmpty());
        IStackNodeSchema schema = getSchema();

        if ((aggregatingPeriod && (schema.isAllowHierarchyAggregation() || schema.isAllowStackNameAggregation())) ||
                !aggregatingPeriod) {
            IPeriod period = getPeriod();
            RootNode root = period.getRootNode();
            root.addBackgroundRoot(this);
        }
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        IStackLogNode anomalies = getAnomalies();
        if (anomalies != null) {
            json.key("anomalies");
            json.startObject();
            ((StackLogNode) anomalies).dump(json, context);
            json.endObject();
        }

        boolean hasValues = false;
        for (IStackNode scopeChild : getScopeChildren()) {
            if (!hasValues) {
                json.key("scopeChildren");
                json.startArray();
                hasValues = true;
            }

            json.startObject();
            ((StackNode) scopeChild).dump(json, context);
            json.endObject();
        }

        if (hasValues)
            json.endArray();
    }
}