/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import java.util.List;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.INameNodeSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link NameNode} is an name node.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NameNode extends AggregationNode implements INameNode {
    private static final int DERIVED_AGGREGATION_BLOCKED_FLAG = 0x2;
    private static final int RESOLVED_FLAG = 0x4;
    protected static final int SCOPE_PARENT_FIELD = 2;
    protected static final int SCOPE_CHILDREN_FIELD = 3;
    protected static final int METRIC_PARENT_FIELD = 4;
    protected static final int METRIC_CHILDREN_FIELD = 5;

    public NameNode(INode node) {
        super(node);
    }

    @Override
    public INameNodeSchema getSchema() {
        return (INameNodeSchema) super.getSchema();
    }

    @Override
    public IMetricName getMetric() {
        return (IMetricName) super.getMetric();
    }

    @Override
    public boolean isDerivedAggregationBlocked() {
        return (getFlags() & DERIVED_AGGREGATION_BLOCKED_FLAG) != 0;
    }

    public void setDerivedAggregationBlocked() {
        INumericField flags = getField(FLAGS_FIELD);
        int value = flags.getInt() | DERIVED_AGGREGATION_BLOCKED_FLAG;
        flags.setInt(value);
    }

    @Override
    public String getNodeType() {
        return isDerived() ? "nameDerived" : "name";
    }

    @Override
    public INameNode getScopeParent() {
        ISingleReferenceField<INameNode> parent = getField(SCOPE_PARENT_FIELD);
        return parent.get();
    }

    @Override
    public Iterable<INameNode> getScopeChildren() {
        IReferenceField<INameNode> children = getField(SCOPE_CHILDREN_FIELD);
        return children;
    }

    @Override
    public void addScopeChild(INameNode child) {
        ISingleReferenceField<INameNode> parentField = child.getField(SCOPE_PARENT_FIELD);
        if (parentField.get() == this)
            return;

        Assert.checkState(parentField.get() == null);
        parentField.set(this);

        IReferenceField<INameNode> children = getField(SCOPE_CHILDREN_FIELD);
        children.add(child);
    }

    @Override
    public INameNode getMetricParent() {
        ISingleReferenceField<INameNode> parent = getField(METRIC_PARENT_FIELD);
        return parent.get();
    }

    @Override
    public Iterable<INameNode> getMetricChildren() {
        IReferenceField<INameNode> children = getField(METRIC_CHILDREN_FIELD);
        return children;
    }

    @Override
    public void addMetricChild(INameNode child) {
        ISingleReferenceField<INameNode> parentField = child.getField(METRIC_PARENT_FIELD);
        if (parentField.get() == this)
            return;

        Assert.checkState(parentField.get() == null);
        parentField.set(this);

        IReferenceField<INameNode> children = getField(METRIC_CHILDREN_FIELD);
        children.add(child);
    }

    @Override
    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
        super.init(nameManager, metadata, aggregatingPeriod);

        IPeriod period = getPeriod();
        RootNode root = period.getRootNode();

        INameNodeSchema schema = getSchema();
        if (schema.isAllowHierarchyAggregation())
            root.addNameNode(this);
        if (aggregatingPeriod && schema.getAggregationField().isLogMetric())
            root.addLog(this);
    }

    public void initEndDerived(Location scopeParentLocation, Location metricParentLocation) {
        IAggregationNodeSchema schema = getSchema();
        IPeriod period = getPeriod();
        INodeIndex<Location, NameNode> index = period.getIndex(schema.getPrimaryField());

        if (scopeParentLocation != null) {
            NameNode parentScopeNode = index.find(scopeParentLocation);
            if (parentScopeNode != null)
                parentScopeNode.addScopeChild(this);
        }

        if (metricParentLocation != null) {
            NameNode parentMetricNode = index.find(metricParentLocation);
            if (parentMetricNode != null)
                parentMetricNode.addMetricChild(this);
        }
    }

    public boolean areReferencesResolved() {
        INumericField flags = getField(FLAGS_FIELD);
        return ((flags.getInt() & RESOLVED_FLAG) != 0);
    }

    public void setReferencesResolved() {
        INumericField flags = getField(FLAGS_FIELD);
        flags.setInt(flags.getInt() | RESOLVED_FLAG);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonMetricChildren = false;
        for (INameNode metricChild : getMetricChildren()) {
            if (!jsonMetricChildren) {
                json.key("metricChildren");
                json.startArray();
                jsonMetricChildren = true;
            }

            json.startObject();
            ((NameNode) metricChild).dump(json, context);
            json.endObject();
        }

        if (jsonMetricChildren)
            json.endArray();

        if (getMetricParent() == null) {
            boolean jsonScopeChildren = false;
            for (INameNode scopeChild : getScopeChildren()) {
                if (!jsonScopeChildren) {
                    json.key("scopeChildren");
                    json.startArray();
                    jsonScopeChildren = true;
                }

                json.startObject();
                ((NameNode) scopeChild).dump(json, context);
                json.endObject();
            }

            if (jsonScopeChildren)
                json.endArray();
        } else {
            boolean jsonScopeChildren = false;
            for (INameNode scopeChild : getScopeChildren()) {
                if (!jsonScopeChildren) {
                    json.key("scopeChildren");
                    json.startArray();
                    jsonScopeChildren = true;
                }

                json.value(getRefId(scopeChild));
            }

            if (jsonScopeChildren)
                json.endArray();
        }
    }

    @Override
    protected void buildFlagsList(int flags, List<String> list) {
        super.buildFlagsList(flags, list);
        if ((flags & DERIVED_AGGREGATION_BLOCKED_FLAG) != 0)
            list.add("derivedAggregationBlocked");
    }
}