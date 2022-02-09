/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.BatchOperation;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.impl.aggregator.AggregationService.EntryPointHierarchy;
import com.exametrika.impl.aggregator.RuleContext.RuleExecutorInfo;
import com.exametrika.impl.aggregator.nodes.SecondaryEntryPointNode;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.aggregator.ScopeHierarchy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ClosePeriodBatchOperationSerializer} is serializer for {@link ClosePeriodBatchOperation}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ClosePeriodBatchOperationSerializer extends AbstractSerializer {
    private static final UUID ID = UUID.fromString("d6e03218-f5b5-44e0-93a0-000b80fe247d");
    private IPeriodNameManager nameManager;
    private IObjectSpaceSchema componentSpaceSchema;
    private IPeriodSpaceSchema aggregatorSpaceSchema;

    public ClosePeriodBatchOperationSerializer() {
        super(ID, ClosePeriodBatchOperation.class);
    }

    @Override
    public void serialize(final ISerialization serialization, Object object) {
        ClosePeriodBatchOperation operation = (ClosePeriodBatchOperation) object;
        serialization.writeLong(operation.getCurrentTime());
        serialization.writeString(operation.getPeriodType());
        serialization.writeInt(operation.getCycleSchemaState());
        serialization.writeInt(operation.getAggregationState());
        serialization.writeLong(operation.getAggregationNodeId());
        serialization.writeBoolean(operation.getInterceptResult());
        serialization.writeInt(operation.getTotalEndNodes());
        serialization.writeInt(operation.getAggregationEndNodes());
        serialization.writeInt(operation.getTotalNodes());
        serialization.writeInt(operation.getAggregationNodes());
        serialization.writeInt(operation.getEndNodes());
        serialization.writeInt(operation.getDerivedNodes());
        serialization.writeInt(operation.getIteration());

        if (operation.getRuleContext() != null && operation.getRuleContext().getExecutors() != null) {
            serialization.writeBoolean(true);
            serialization.writeInt(operation.getRuleContext().getExecutors().size());
            operation.getRuleContext().getExecutors().forEachEntry(new TLongObjectProcedure<RuleExecutorInfo>() {
                @Override
                public boolean execute(long id, RuleExecutorInfo info) {
                    serialization.writeLong(id);
                    serialization.writeInt(info.getFacts().size());

                    for (Map.Entry<String, Object> entry : info.getFacts().entrySet()) {
                        serialization.writeString(entry.getKey());
                        serialization.writeObject(entry.getValue());
                    }
                    return true;
                }
            });
        } else
            serialization.writeBoolean(false);

        if (operation.getHierarchyMap() != null) {
            serialization.writeBoolean(true);

            ensureSpace(serialization);

            serialization.writeInt(operation.getHierarchyMap().size());
            operation.getHierarchyMap().forEachEntry(new TLongObjectProcedure<EntryPointHierarchy>() {
                @Override
                public boolean execute(long id, EntryPointHierarchy hierarchy) {
                    serialization.writeLong(id);

                    serialization.writeInt(hierarchy.scopeHierarchy.getScopes().size());
                    for (IScopeName scope : hierarchy.scopeHierarchy.getScopes()) {
                        long scopeId = 0;
                        if (!scope.isEmpty())
                            scopeId = nameManager.addName(scope).getId();

                        serialization.writeLong(scopeId);
                    }

                    serialization.writeInt(hierarchy.nodeHierarchy.size());
                    for (SecondaryEntryPointNode node : hierarchy.nodeHierarchy)
                        serialization.writeLong(node.getId());

                    return true;
                }
            });
        } else
            serialization.writeBoolean(false);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        ensureSpace(deserialization);

        ClosePeriodBatchOperation operation = new ClosePeriodBatchOperation();
        operation.setCurrentTime(deserialization.readLong());
        operation.setPeriodType(deserialization.readString());
        operation.setCycleSchemaState(deserialization.readInt());
        operation.setAggregationState(deserialization.readInt());
        operation.setAggregationNodeId(deserialization.readLong());
        operation.setInterceptResult(deserialization.readBoolean());
        operation.setTotalEndNodes(deserialization.readInt());
        operation.setAggregationEndNodes(deserialization.readInt());
        operation.setTotalNodes(deserialization.readInt());
        operation.setAggregationNodes(deserialization.readInt());
        operation.setEndNodes(deserialization.readInt());
        operation.setDerivedNodes(deserialization.readInt());
        operation.setIteration(deserialization.readInt());

        RuleContext ruleContext = null;
        if (deserialization.readBoolean()) {
            IObjectSpace space = componentSpaceSchema.getSpace();
            INodeIndex<Long, IRuleExecutor> index = space.findIndex("componentIndex");

            ruleContext = new RuleContext();
            int count = deserialization.readInt();
            for (int i = 0; i < count; i++) {
                long executorId = deserialization.readLong();
                IRuleExecutor executor = index.find(executorId);

                int factsCount = deserialization.readInt();
                for (int k = 0; k < factsCount; k++) {
                    String name = deserialization.readString();
                    Object value = deserialization.readObject();

                    ruleContext.setFact(executor, name, value);
                }
            }
        }

        operation.setRuleContext(ruleContext);

        TLongObjectMap<EntryPointHierarchy> hierarchyMap = null;
        if (deserialization.readBoolean()) {
            IPeriod period = aggregatorSpaceSchema.findCycle(operation.getPeriodType()).getCurrentCycle().getSpace().getCurrentPeriod();

            hierarchyMap = new TLongObjectHashMap<EntryPointHierarchy>();
            int count = deserialization.readInt();
            for (int i = 0; i < count; i++) {
                long nodeId = deserialization.readLong();
                EntryPointHierarchy hierarchy = new EntryPointHierarchy();

                int scopeCount = deserialization.readInt();
                List<IScopeName> scopes = new ArrayList<IScopeName>(scopeCount);
                for (int k = 0; k < scopeCount; k++) {
                    long scopeId = deserialization.readLong();
                    if (scopeId == 0)
                        scopes.add(Names.rootScope());
                    else
                        scopes.add((IScopeName) nameManager.findById(scopeId).getName());
                }

                hierarchy.scopeHierarchy = new ScopeHierarchy(scopes);

                int nodeCount = deserialization.readInt();
                for (int k = 0; k < nodeCount; k++) {
                    long secondaryNodeId = deserialization.readLong();
                    SecondaryEntryPointNode node = period.findNodeById(secondaryNodeId);
                    hierarchy.nodeHierarchy.add(node);
                }

                hierarchyMap.put(nodeId, hierarchy);
            }
        }

        operation.setHierarchyMap(hierarchyMap);

        return operation;
    }

    private void ensureSpace(Object contextHolder) {
        if (nameManager != null)
            return;

        IDatabaseContext context;
        if (contextHolder instanceof ISerialization)
            context = ((ISerialization) contextHolder).getExtension(BatchOperation.EXTENTION_ID);
        else
            context = ((IDeserialization) contextHolder).getExtension(BatchOperation.EXTENTION_ID);

        nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
        componentSpaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
        aggregatorSpaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:aggregation.aggregation");
    }
}
