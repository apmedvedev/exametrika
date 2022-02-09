/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.nodes.Dependency;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IExitPointNode;
import com.exametrika.api.aggregator.nodes.IStackNameNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IStackNodeSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.ScopeName;


/**
 * The {@link StackNode} is an stack node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StackNode extends AggregationNode implements IStackNode {
    protected static final int TOTAL_REFERENCE_FLAG = 1;
    protected static final int ROOT_FIELD = 2;
    protected static final int PARENT_FIELD = 3;
    protected static final int CHILDREN_FIELD = 4;
    protected static final int DEPENDENTS_FIELD = 5;
    protected static final int TRANSACTION_ROOT_FIELD = 6;
    private static final IScopeName scopeName = ScopeName.get("none");

    public StackNode(INode node) {
        super(node);
    }

    @Override
    public IStackNodeSchema getSchema() {
        return (IStackNodeSchema) super.getSchema();
    }

    @Override
    public ICallPath getMetric() {
        return (ICallPath) super.getMetric();
    }

    @Override
    public String getNodeType() {
        return isDerived() ? "stackDerived" : "stack";
    }

    @Override
    public IStackNode getParent() {
        ISingleReferenceField<IStackNode> field = getField(PARENT_FIELD);
        return field.get();
    }

    @Override
    public Iterable<IStackNode> getChildren() {
        IReferenceField<IStackNode> field = getField(CHILDREN_FIELD);
        return field;
    }

    @Override
    public void addChild(IStackNode child) {
        Assert.notNull(child);

        ISingleReferenceField<IStackNode> parentField = child.getField(PARENT_FIELD);
        if (parentField.get() == this)
            return;

        Assert.checkState(parentField.get() == null);
        parentField.set(this);

        IReferenceField<IStackNode> childrenField = getField(CHILDREN_FIELD);
        childrenField.add(child);

        ISingleReferenceField<IStackNode> rootField = child.getField(ROOT_FIELD);
        Assert.checkState(rootField.get() == null);
        if (getRoot() != null)
            rootField.set(getRoot());

        if (getTransactionRoot() != null)
            ((StackNode) child).setTransactionRoot(getTransactionRoot(), false);

        if (child instanceof IExitPointNode && child.getParent() instanceof IExitPointNode && getRoot() != null)
            getRoot().addExitPoint((IExitPointNode) child);
    }

    @Override
    public IEntryPointNode getRoot() {
        ISingleReferenceField<IEntryPointNode> parent = getField(ROOT_FIELD);
        return parent.get();
    }

    @Override
    public IEntryPointNode getTransactionRoot() {
        ISingleReferenceField<IEntryPointNode> field = getField(TRANSACTION_ROOT_FIELD);
        return field.get();
    }

    public void setTransactionRoot(IEntryPointNode transactionRoot, boolean recursive) {
        Assert.notNull(transactionRoot);

        ISingleReferenceField<IEntryPointNode> field = getField(TRANSACTION_ROOT_FIELD);
        if (field.get() == null) {
            field.set(transactionRoot);

            if (recursive) {
                for (IStackNode child : getChildren())
                    ((StackNode) child).setTransactionRoot(transactionRoot, true);
            }
        } else
            Assert.checkState(field.get() == transactionRoot);
    }

    @Override
    public Iterable<Dependency<IStackNameNode>> getDependents() {
        IReferenceField<IStackNameNode> dependents = getField(DEPENDENTS_FIELD);
        return new DependencyIterable(dependents);
    }

    @Override
    public void addDependent(IStackNameNode dependent, boolean total) {
        IReferenceField<IStackNameNode> dependents = getField(DEPENDENTS_FIELD);
        dependents.add(dependent, total ? TOTAL_REFERENCE_FLAG : 0);

        IReferenceField<IStackNode> dependencies = dependent.getField(StackNameNode.DEPENDENCIES_FIELD);
        dependencies.add(this, total ? TOTAL_REFERENCE_FLAG : 0);
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
        super.onCreated(primaryKey, args);

        ICallPath callPath = getMetric();
        if (callPath.isEmpty()) {
            ISingleReferenceField<IStackNode> root = getField(ROOT_FIELD);
            root.set(this);
        }
    }

    @Override
    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
        super.init(nameManager, metadata, aggregatingPeriod);

        ICallPath callPath = getMetric();
        if (callPath.isEmpty())
            return;

        ICycleSchema cycleSchema = getSpace().getSchema();
        IPeriod period = getPeriod();

        Location location = getLocation();
        NameMeasurementId parentId = getParentId(cycleSchema, callPath, metadata);
        long parentCallPathId = getCallPathId(nameManager, (ICallPath) parentId.getLocation());
        Assert.isTrue(parentCallPathId >= 0);

        IAggregationNodeSchema parentNodeSchema = cycleSchema.findAggregationNode(parentId.getComponentType());

        INodeIndex<Location, StackNode> index = period.getIndex(parentNodeSchema.getPrimaryField());
        StackNode parent = index.find(new Location(location.getScopeId(), parentCallPathId));
        if (parent != null)
            parent.addChild(this);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonDependents = false;
        for (Dependency<IStackNameNode> dependent : getDependents()) {
            if (!jsonDependents) {
                json.key("dependents");
                json.startArray();
                jsonDependents = true;
            }

            String dependentName = dependent.getNode().getSchema().getQualifiedName() + "@" +
                    dependent.getNode().getId() + (dependent.isTotal() ? "[inherent,total]" : "[inherent]");
            json.value(dependentName);
        }

        if (jsonDependents)
            json.endArray();

        boolean jsonChildren = false;
        for (IStackNode child : getChildren()) {
            if (!jsonChildren) {
                json.key("children");
                json.startArray();
                jsonChildren = true;
            }

            json.startObject();
            ((StackNode) child).dump(json, context);
            json.endObject();
        }

        if (jsonChildren)
            json.endArray();
    }

    private NameMeasurementId getParentId(ICycleSchema cycleSchema, ICallPath callPath, JsonObject metadata) {
        Assert.notNull(metadata);
        String parentComponentType = (String) metadata.get("parent");
        IAggregationNodeSchema parentNodeSchema = cycleSchema.findAggregationNode(parentComponentType);

        if (parentNodeSchema != null)
            return new NameMeasurementId(scopeName, callPath.getParent(), parentComponentType);
        else
            return new NameMeasurementId(scopeName, CallPath.root(), (String) metadata.get("entry", "app.stack.root"));
    }

    private long getCallPathId(IPeriodNameManager nameManager, ICallPath callPath) {
        if (callPath.isEmpty())
            return 0;

        IPeriodName name = nameManager.findByName(callPath);
        if (name != null)
            return name.getId();
        else
            return -1;
    }
}