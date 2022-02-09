/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import java.util.List;
import java.util.UUID;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IIntermediateExitPointNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IUuidField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IntermediateExitPointNode} is an intermediate exit point node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class IntermediateExitPointNode extends ExitPointNode implements IIntermediateExitPointNode {
    private static final int SYNC_FLAG = 0x4;
    private static final int SCOPE_INNER_FLAG = 0x8;
    private static final int LEAF_FLAG = 0x10;
    protected static final int CHILD_ENTRY_POINT_FIELD = 7;
    protected static final int STACKID_FIELD = 8;

    public IntermediateExitPointNode(INode node) {
        super(node);
    }

    @Override
    public boolean isLeaf() {
        return (getFlags() & LEAF_FLAG) != 0;
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
    public ISecondaryEntryPointNode getChildEntryPoint() {
        ISingleReferenceField<ISecondaryEntryPointNode> field = getField(CHILD_ENTRY_POINT_FIELD);
        return field.get();
    }

    @Override
    public void setTransactionRoot(IEntryPointNode transactionRoot, boolean recursive) {
        super.setTransactionRoot(transactionRoot, recursive);

        if (recursive && getChildEntryPoint() != null)
            ((StackNode) getChildEntryPoint()).setTransactionRoot(transactionRoot, true);
    }

    @Override
    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
        super.init(nameManager, metadata, aggregatingPeriod);

        Assert.notNull(metadata);

        String type = metadata.get("type");
        if (!type.contains("async"))
            setSync();

        if (metadata.contains("stackId")) {
            INumericField flags = getField(FLAGS_FIELD);
            int value = flags.getInt() | LEAF_FLAG;
            flags.setInt(value);

            UUID stackId = UUID.fromString((String) metadata.get("stackId"));
            IUuidField field = getField(STACKID_FIELD);

            INodeIndex<UUID, SecondaryEntryPointNode> index = getPeriod().getIndex(field.getSchema());
            SecondaryEntryPointNode entryPoint = index.find(stackId);
            if (entryPoint != null)
                setChildEntryPoint(entryPoint);

            field.set(stackId);
        }
    }

    public void setSync() {
        INumericField flags = getField(FLAGS_FIELD);
        int value = flags.getInt() | SYNC_FLAG;
        flags.setInt(value);
    }

    public void setChildEntryPoint(SecondaryEntryPointNode entryPoint) {
        if (entryPoint.getParentExitPoint() == this)
            return;

        Assert.checkState(entryPoint.getParentExitPoint() == null);
        entryPoint.setParentExitPoint(this, isSync());

        ISingleReferenceField<ISecondaryEntryPointNode> ref = getField(CHILD_ENTRY_POINT_FIELD);
        ref.set(entryPoint);
    }

    public void setScopeInner() {
        INumericField flags = getField(FLAGS_FIELD);
        int value = flags.getInt() | SCOPE_INNER_FLAG;
        flags.setInt(value);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        IEntryPointNode childEntryPoint = getChildEntryPoint();
        if (childEntryPoint != null) {
            if (!context.isNodeTraversed(childEntryPoint.getId())) {
                json.key("childEntryPoint");
                json.startObject();
                ((EntryPointNode) childEntryPoint).dump(json, context);
                json.endObject();
            } else {
                json.key("childEntryPoint");
                json.value(getRefId(childEntryPoint));
            }
        }
    }

    @Override
    protected void buildFlagsList(int flags, List<String> list) {
        super.buildFlagsList(flags, list);
        if (isLeaf())
            list.add("leaf");
        if (isSync())
            list.add("sync");
        if (isScopeInner())
            list.add("scopeInner");
    }
}