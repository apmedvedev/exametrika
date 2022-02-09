/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import java.util.List;
import java.util.UUID;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.values.IStackIdsValue;
import com.exametrika.api.aggregator.nodes.IIntermediateExitPointNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.schema.IStackNodeSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IUuidField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;


/**
 * The {@link SecondaryEntryPointNode} is an entry point node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SecondaryEntryPointNode extends EntryPointNode implements ISecondaryEntryPointNode {
    private static final int SYNC_FLAG = 0x4;
    private static final int SCOPE_INNER_FLAG = 0x8;
    private static final int RESOLVED_FLAG = 0x10;
    protected static final int PARENT_EXIT_POINT_FIELD = 11;
    protected static final int STACKID_FIELD = 12;

    public SecondaryEntryPointNode(INode node) {
        super(node);
    }

    @Override
    public boolean isSync() {
        return (getFlags() & SYNC_FLAG) != 0;
    }

    @Override
    public boolean isScopeInner() {
        return (getFlags() & SCOPE_INNER_FLAG) != 0;
    }

    @Override
    public IIntermediateExitPointNode getParentExitPoint() {
        ISingleReferenceField<IIntermediateExitPointNode> field = getField(PARENT_EXIT_POINT_FIELD);
        return field.get();
    }

    @Override
    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
        super.init(nameManager, metadata, aggregatingPeriod);

        IPeriod period = getPeriod();
        RootNode root = period.getRootNode();

        if (getCombineType() != CombineType.STACK) {
            root.addSecondaryEntryPoint(this);
            return;
        }

        String id = getScope().getLastSegment();
        int pos = id.indexOf("-");
        Assert.isTrue(pos != -1);
        id = id.substring(pos + 1);

        UUID stackId = UUID.fromString(id);
        IUuidField field = getField(STACKID_FIELD);

        INodeIndex<UUID, IntermediateExitPointNode> index = period.getIndex(field.getSchema());
        IntermediateExitPointNode exitPoint = index.find(stackId);
        if (exitPoint != null)
            exitPoint.setChildEntryPoint(this);

        field.set(stackId);
    }

    public void setParentExitPoint(IntermediateExitPointNode exitPoint, boolean sync) {
        CombineType combineType = getCombineType();
        if (combineType == CombineType.STACK) {
            ISingleReferenceField<IIntermediateExitPointNode> ref = getField(PARENT_EXIT_POINT_FIELD);
            ref.set(exitPoint);
        }

        if ((combineType == CombineType.STACK || combineType == CombineType.TRANSACTION) && exitPoint.getTransactionRoot() != null)
            setTransactionRoot(exitPoint.getTransactionRoot(), true);

        if (sync)
            setSync();
    }

    public void resolveReferences() {
        INumericField flags = getField(FLAGS_FIELD);
        if ((flags.getInt() & RESOLVED_FLAG) != 0)
            return;

        IPeriod period = getPeriod();
        IUuidField field = getField(STACKID_FIELD);
        INodeIndex<UUID, IntermediateExitPointNode> index = period.getIndex(field.getSchema());
        IStackNodeSchema schema = getSchema();
        IStackIdsValue stackIds = (IStackIdsValue) getAggregationField().getValue(false).getMetrics().get(schema.getStackIdsMetricIndex());

        boolean resolved;
        if (stackIds.getIds() != null) {
            resolved = true;
            for (UUID stackId : stackIds.getIds()) {
                IntermediateExitPointNode exitPoint = index.find(stackId);
                if (exitPoint != null)
                    exitPoint.setChildEntryPoint(this);
                else
                    resolved = false;
            }
        } else
            resolved = false;

        if (resolved)
            flags.setInt(flags.getInt() | RESOLVED_FLAG);
    }

    public void setSync() {
        INumericField flags = getField(FLAGS_FIELD);
        int value = flags.getInt() | SYNC_FLAG;
        flags.setInt(value);
    }

    public void setScopeInner() {
        INumericField flags = getField(FLAGS_FIELD);
        int value = flags.getInt() | SCOPE_INNER_FLAG;
        flags.setInt(value);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonScopeChildren = false;
        for (IStackNode scopeChild : getScopeChildren()) {
            if (!jsonScopeChildren) {
                json.key("scopeChildren");
                json.startArray();
                jsonScopeChildren = true;
            }

            json.value(getRefId(scopeChild));
        }

        if (jsonScopeChildren)
            json.endArray();

        if (getCombineType() != CombineType.STACK) {
            IStackIdsValue stackIds = (IStackIdsValue) getAggregationField().getValue(false).getMetrics().get(getSchema().getStackIdsMetricIndex());
            if (!Collections.isEmpty(stackIds.getIds())) {
                json.key("stackIds");
                json.startArray();
                for (UUID id : stackIds.getIds())
                    json.value(id);
                json.endArray();
            }
        }
    }

    @Override
    protected void buildFlagsList(int flags, List<String> list) {
        super.buildFlagsList(flags, list);
        if (isSync())
            list.add("sync");
        if (isScopeInner())
            list.add("scopeInner");
    }

    private CombineType getCombineType() {
        return getSpace().getSchema().getParent().getConfiguration().getCombineType();
    }
}