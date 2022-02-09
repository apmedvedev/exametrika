/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.nodes.IPrimaryEntryPointNode;
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
 * The {@link PrimaryEntryPointNode} is a primary entry point node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class PrimaryEntryPointNode extends EntryPointNode implements IPrimaryEntryPointNode {
    protected static final int TRANSACTION_FAILURES_FIELD = 11;
    protected static final int ANOMALIES_FIELD = 12;

    public PrimaryEntryPointNode(INode node) {
        super(node);
    }

    @Override
    public IStackLogNode getTransactionFailures() {
        ISingleReferenceField<IStackLogNode> transactionFailures = getField(TRANSACTION_FAILURES_FIELD);
        return transactionFailures.get();
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
            root.addTransactionRoot(this);
        }

        if (aggregatingPeriod && schema.isAllowTransactionFailureDependenciesAggregation()) {
            IPeriod period = getPeriod();
            IStackLogNode node = period.createNode(getLocation(), schema.getTransactionFailureDependenciesNode());

            ISingleReferenceField<IStackLogNode> transactionFailures = getField(TRANSACTION_FAILURES_FIELD);
            transactionFailures.set(node);
        }
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        IStackLogNode failures = getTransactionFailures();
        if (failures != null) {
            json.key("failures");
            json.startObject();
            ((StackLogNode) failures).dump(json, context);
            json.endObject();
        }

        IStackLogNode anomalies = getAnomalies();
        if (anomalies != null) {
            json.key("anomalies");
            json.startObject();
            ((StackLogNode) anomalies).dump(json, context);
            json.endObject();
        }

        boolean jsonScopeChildren = false;
        for (IStackNode scopeChild : getScopeChildren()) {
            if (!jsonScopeChildren) {
                json.key("scopeChildren");
                json.startArray();
                jsonScopeChildren = true;
            }

            json.startObject();
            ((StackNode) scopeChild).dump(json, context);
            json.endObject();
        }

        if (jsonScopeChildren)
            json.endArray();
    }
}