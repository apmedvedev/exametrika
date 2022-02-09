/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IExitPointNode;
import com.exametrika.api.aggregator.nodes.IStackLogNode;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;


/**
 * The {@link EntryPointNode} is a entry point node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class EntryPointNode extends StackNode implements IEntryPointNode {
    protected static final int EXIT_POINTS_FIELD = 7;
    protected static final int LOGS_FIELD = 8;
    protected static final int SCOPE_PARENT_FIELD = 9;
    protected static final int SCOPE_CHILDREN_FIELD = 10;

    public EntryPointNode(INode node) {
        super(node);
    }

    @Override
    public IEntryPointNode getScopeParent() {
        ISingleReferenceField<IEntryPointNode> parent = getField(SCOPE_PARENT_FIELD);
        return parent.get();
    }

    @Override
    public Iterable<IEntryPointNode> getScopeChildren() {
        IReferenceField<IEntryPointNode> children = getField(SCOPE_CHILDREN_FIELD);
        return children;
    }

    @Override
    public void addScopeChild(IEntryPointNode child) {
        ISingleReferenceField<IEntryPointNode> parentField = child.getField(SCOPE_PARENT_FIELD);
        if (parentField.get() == this)
            return;

        Assert.checkState(parentField.get() == null);
        parentField.set(this);

        IReferenceField<IEntryPointNode> children = getField(SCOPE_CHILDREN_FIELD);
        children.add(child);
    }

    @Override
    public Iterable<IExitPointNode> getExitPoints() {
        IReferenceField<IExitPointNode> exitPoints = getField(EXIT_POINTS_FIELD);
        return exitPoints;
    }

    @Override
    public void addExitPoint(IExitPointNode node) {
        Assert.notNull(node);

        IReferenceField<IExitPointNode> exitPoints = getField(EXIT_POINTS_FIELD);
        exitPoints.add(node);
    }

    @Override
    public Iterable<IStackLogNode> getLogs() {
        IReferenceField<IStackLogNode> logs = getField(LOGS_FIELD);
        return logs;
    }

    @Override
    public void addLog(IStackLogNode node) {
        if (node.getMainNode() == this)
            return;

        Assert.checkState(node.getMainNode() == null);
        StackLogNode logNode = (StackLogNode) node;
        logNode.setMainNode(this);

        IReferenceField<IStackLogNode> logs = getField(LOGS_FIELD);
        logs.add(node);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonExitPoints = false;
        for (IExitPointNode exitPoint : getExitPoints()) {
            if (!jsonExitPoints) {
                json.key("exitPoints");
                json.startArray();
                jsonExitPoints = true;
            }

            json.value(getRefId(exitPoint));
        }

        if (jsonExitPoints)
            json.endArray();

        boolean jsonLogs = false;
        for (IStackLogNode log : getLogs()) {
            if (!jsonLogs) {
                json.key("logs");
                json.startArray();
                jsonLogs = true;
            }

            json.startObject();
            ((StackLogNode) log).dump(json, context);
            json.endObject();
        }

        if (jsonLogs)
            json.endArray();
    }
}