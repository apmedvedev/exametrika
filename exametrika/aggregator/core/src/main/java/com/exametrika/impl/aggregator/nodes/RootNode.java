/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IBackgroundRootNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.nodes.IPrimaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IRootNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.common.json.IJsonHandler;


/**
 * The {@link RootNode} is a root node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class RootNode extends PeriodNodeObject implements IRootNode {
    protected static final int NAME_NODES_FIELD = 1;
    protected static final int BACKGROUND_ROOTS_FIELD = 2;
    protected static final int TRANSACTION_ROOTS_FIELD = 3;
    protected static final int SECONDARY_ENTRY_POINTS_FIELD = 4;
    protected static final int LOGS_FIELD = 5;
    protected static final int DERIVED_ROOTS_FIELD = 6;

    public RootNode(INode node) {
        super(node);
    }

    @Override
    public Iterable<INameNode> getNameNodes() {
        IReferenceField<INameNode> nameNodes = getField(NAME_NODES_FIELD);
        return nameNodes;
    }

    @Override
    public void addNameNode(INameNode node) {
        IReferenceField<INameNode> nameNodes = getField(NAME_NODES_FIELD);
        nameNodes.add(node);
    }

    @Override
    public Iterable<IBackgroundRootNode> getBackgroundRoots() {
        IReferenceField<IBackgroundRootNode> backgroundRoots = getField(BACKGROUND_ROOTS_FIELD);
        return backgroundRoots;
    }

    @Override
    public void addBackgroundRoot(IBackgroundRootNode root) {
        IReferenceField<IBackgroundRootNode> backgroundRoots = getField(BACKGROUND_ROOTS_FIELD);
        backgroundRoots.add(root);
    }

    @Override
    public Iterable<IPrimaryEntryPointNode> getTransactionRoots() {
        IReferenceField<IPrimaryEntryPointNode> transactionRoots = getField(TRANSACTION_ROOTS_FIELD);
        return transactionRoots;
    }

    @Override
    public void addTransactionRoot(IPrimaryEntryPointNode root) {
        IReferenceField<IPrimaryEntryPointNode> transactionRoots = getField(TRANSACTION_ROOTS_FIELD);
        transactionRoots.add(root);
    }

    @Override
    public Iterable<ISecondaryEntryPointNode> getSecondaryEntryPoints() {
        IReferenceField<ISecondaryEntryPointNode> secondaryEntryPoints = getField(SECONDARY_ENTRY_POINTS_FIELD);
        return secondaryEntryPoints;
    }

    @Override
    public void addSecondaryEntryPoint(ISecondaryEntryPointNode value) {
        IReferenceField<ISecondaryEntryPointNode> secondaryEntryPoints = getField(SECONDARY_ENTRY_POINTS_FIELD);
        secondaryEntryPoints.add(value);
    }

    @Override
    public Iterable<IAggregationNode> getLogs() {
        IReferenceField<IAggregationNode> logs = getField(LOGS_FIELD);
        return logs;
    }

    @Override
    public void addLog(IAggregationNode node) {
        IReferenceField<IAggregationNode> logs = getField(LOGS_FIELD);
        logs.add(node);
    }

    @Override
    public Iterable<IAggregationNode> getDerivedRoots() {
        IReferenceField<IAggregationNode> derivedRoots = getField(DERIVED_ROOTS_FIELD);
        return derivedRoots;
    }

    @Override
    public void addDerivedRoot(IAggregationNode root) {
        IReferenceField<IAggregationNode> derivedRoots = getField(DERIVED_ROOTS_FIELD);
        derivedRoots.add(root);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonRootNodes = false;
        for (IAggregationNode derivedRootNode : getDerivedRoots()) {
            if (!jsonRootNodes) {
                json.key("nodes");
                json.startArray();
                jsonRootNodes = true;
            }

            json.startObject();
            ((AggregationNode) derivedRootNode).dump(json, context);
            json.endObject();
        }

        for (IBackgroundRootNode backgroundRootNode : getBackgroundRoots()) {
            if (backgroundRootNode.getScopeParent() != null)
                continue;

            if (!jsonRootNodes) {
                json.key("nodes");
                json.startArray();
                jsonRootNodes = true;
            }

            json.startObject();
            ((AggregationNode) backgroundRootNode).dump(json, context);
            json.endObject();
        }

        for (IPrimaryEntryPointNode transactionRootNode : getTransactionRoots()) {
            if (transactionRootNode.getScopeParent() != null)
                continue;

            if (!jsonRootNodes) {
                json.key("nodes");
                json.startArray();
                jsonRootNodes = true;
            }

            json.startObject();
            ((AggregationNode) transactionRootNode).dump(json, context);
            json.endObject();
        }

        if (jsonRootNodes)
            json.endArray();
    }
}