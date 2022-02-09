/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.fields;

import java.util.Collections;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.IPeriodSpace;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.IComponentRepresentationSchema;
import com.exametrika.api.aggregator.schema.ILogAggregationFieldSchema;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.ComponentBuilder;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.nodes.StackLogNode;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.aggregator.schema.LogAggregationFieldSchema;
import com.exametrika.impl.aggregator.values.ComputeContext;
import com.exametrika.impl.exadb.objectdb.fields.BlobFieldInitializer;
import com.exametrika.impl.exadb.objectdb.fields.StructuredBlobField;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link LogAggregationField} is an aggregation field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LogAggregationField extends StructuredBlobField<IAggregationRecord> implements ILogAggregationField {
    private IJsonField metadataField;
    private long time;

    public LogAggregationField(ISimpleField field) {
        super(field);
    }

    @Override
    public ILogAggregationFieldSchema getSchema() {
        return (ILogAggregationFieldSchema) super.getSchema();
    }

    @Override
    public IComponentValue getValue(boolean copy) {
        IAggregationRecord record = getCurrent();
        if (record != null)
            return record.getValue();
        else
            return null;
    }

    @Override
    public IComputeContext getComputeContext() {
        IAggregationRecord record = getCurrent();

        ComputeContext context = getContext(this);

        IAggregationNode aggregationNode = getNode().getObject();
        context.setNodeType(aggregationNode.getNodeType());

        if (record != null) {
            context.setTime(record.getTime());
            context.setPeriod(record.getPeriod());
        }

        return context;
    }

    @Override
    public Object getRepresentation(int index, boolean includeTime, boolean includeMetadata) {
        IAggregationRecord record = getCurrent();
        if (record == null)
            return null;

        IComputeContext context = getComputeContext();

        IComponentRepresentationSchema representation = getSchema().getRepresentations().get(index);
        return representation.getComputer().compute(record.getValue(), context, includeTime, includeMetadata);
    }

    @Override
    public IAggregationIterable getRecords() {
        return (IAggregationIterable) super.getRecords();
    }

    @Override
    public IAggregationIterable getRecords(long startId, long endId) {
        return (IAggregationIterable) super.getRecords(startId, endId);
    }

    @Override
    public IAggregationIterable getReverseRecords() {
        return (IAggregationIterable) super.getReverseRecords();
    }

    @Override
    public IAggregationIterable getReverseRecords(long startId, long endId) {
        return (IAggregationIterable) super.getReverseRecords(startId, endId);
    }

    public IAggregationIterable getEmptyRecords() {
        return new AggregationIterable();
    }

    @Override
    public JsonObject getMetadata() {
        return metadataField.get();
    }

    @Override
    public long[] add(IComponentValue value, long time, long period) {
        if (time < this.time)
            time = this.time;

        ILogAggregationFieldSchema schema = getSchema();
        if (schema.isLogMetric()) {
            IObjectValue objectValue = (IObjectValue) value.getMetrics().get(0);
            if (objectValue.getObject() instanceof JsonArray) {
                JsonObject metadata = value.getMetadata();
                long firstId = 0;
                long lastId = 0;
                for (Object element : (JsonArray) objectValue.getObject()) {
                    long id = add(new AggregationRecord(new ComponentValue(Collections.singletonList(new ObjectValue(element)), metadata), time, period));
                    metadata = null;
                    if (firstId == 0)
                        firstId = id;
                    lastId = id;
                }

                return new long[]{firstId, lastId};
            }
        }

        if (schema.getBaseRepresentations() != null) {
            ComponentBuilder builder = (ComponentBuilder) schema.getConfiguration().getComponentType().getMetrics().createBuilder();
            builder.set(value);
            IComputeContext context = getComputeContext();

            context.setTime(time);
            context.setPeriod(period);

            for (int i = 0; i < schema.getBaseRepresentations().size(); i++)
                schema.getBaseRepresentations().get(i).getComputer().computeSecondary(builder, context);

            value = builder.toValue();
        }

        long id = add(new AggregationRecord(value, time, period));
        return new long[]{id, id};
    }

    @Override
    public long add(IAggregationRecord record) {
        if (record.getTime() > this.time)
            this.time = record.getTime();
        else
            record = new AggregationRecord(record.getValue(), this.time, record.getPeriod());

        if (metadataField != null && record.getValue().getMetadata() != null)
            metadataField.set(record.getValue().getMetadata());

        IAggregationNode node = getNode().getObject();
        if (node instanceof StackLogNode)
            ((StackLogNode) node).resolveReference();

        long id = super.add(record);

        LogAggregationFieldSchema schema = (LogAggregationFieldSchema) getSchema();
        IAggregationNodeSchema nodeSchema = schema.getParent();
        if (nodeSchema.getParent().getConfiguration().isNonAggregating() &&
                !nodeSchema.getComponentBindingStrategies().isEmpty()) {
            for (IComponentBindingStrategy strategy : nodeSchema.getComponentBindingStrategies()) {
                IScopeName scopeName = strategy.getComponentScope(node);
                if (scopeName == null)
                    continue;

                IPeriodName name = schema.getPeriodNameManager().findByName(scopeName);
                if (name == null)
                    continue;

                IRuleExecutor ruleExecutor = schema.getRuleService().findRuleExecutor(name.getId());
                if (ruleExecutor != null)
                    ruleExecutor.executeSimpleRules(node, null);
            }
        }

        return id;
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        BlobFieldInitializer blobFieldInitializer = new BlobFieldInitializer();
        blobFieldInitializer.setStore(((IPeriodSpace) getNode().getSpace()).getCyclePeriod().getRootNode());
        super.onCreated(primaryKey, blobFieldInitializer);
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
        initMetadataField();
    }

    @Override
    public void onOpened() {
        initMetadataField();

        super.onOpened();

        IAggregationRecord record = getCurrent();
        if (record != null)
            time = record.getTime();
    }

    @Override
    public void setStore(Object store) {
        Assert.supports(false);
    }

    @Override
    protected IStructuredIterable createIterable(long startId, long endId, boolean includeEnd, boolean direct) {
        if (startId != 0)
            return new AggregationIterable(startId, endId, includeEnd, direct);
        else
            return new AggregationIterable();
    }

    @Override
    protected void checkClass(Object record) {
        Assert.isTrue(record instanceof AggregationRecord);
    }

    @Override
    protected Object doRead(IDataDeserialization fieldDeserialization) {
        LogAggregationFieldSchema schema = (LogAggregationFieldSchema) getSchema();

        JsonObject metadata = null;
        if (metadataField != null)
            metadata = metadataField.get();

        IComponentValue value = schema.getSerializer().deserialize(fieldDeserialization, false, metadata);
        long time = fieldDeserialization.readLong();
        long period = fieldDeserialization.readLong();

        return new AggregationRecord(value, time, period);
    }

    @Override
    protected void doWrite(IDataSerialization fieldSerialization, Object r) {
        LogAggregationFieldSchema schema = (LogAggregationFieldSchema) getSchema();
        AggregationRecord record = (AggregationRecord) r;

        schema.getSerializer().serialize(fieldSerialization, record.getValue(), false);
        fieldSerialization.writeLong(record.getTime());
        fieldSerialization.writeLong(record.getPeriod());
    }

    private ComputeContext getContext(Object object) {
        ComputeContext context = new ComputeContext();

        context.setObject(object);

        IPeriodNode node = (IPeriodNode) getNode();
        CycleSchema cycleSchema = (CycleSchema) node.getSpace().getSchema();
        context.setNameManager(cycleSchema.getNameManager());
        return context;
    }

    private void initMetadataField() {
        int metadataFieldIndex = getSchema().getMetadataFieldIndex();
        if (metadataFieldIndex != -1)
            metadataField = getNode().getField(metadataFieldIndex);
    }

    private class AggregationIterable extends StructuredIterable<IAggregationRecord> implements IAggregationIterable {
        public AggregationIterable() {
        }

        public AggregationIterable(long startId, long endId, boolean includeEnd, boolean direct) {
            super(startId, endId, includeEnd, direct);
        }

        @Override
        public IAggregationIterator iterator() {
            if (startId != 0)
                return new AggregationIterator(startId, endId, includeEnd, direct);
            else
                return new AggregationIterator();
        }
    }

    private class AggregationIterator extends StructuredIterator implements IAggregationIterator {
        private final ComputeContext context;

        public AggregationIterator() {
            context = null;
        }

        public AggregationIterator(long startId, long endId, boolean includeEnd, boolean direct) {
            super(startId, endId, includeEnd, direct);

            context = getContext(this);
            context.setNodeType("log");
        }

        @Override
        public ILogAggregationField getField() {
            return LogAggregationField.this;
        }

        @Override
        public IComputeContext getComputeContext() {
            IAggregationRecord record = get();

            context.setTime(record.getTime());
            context.setPeriod(record.getPeriod());

            return context;
        }

        @Override
        public Object getRepresentation(int index, boolean includeTime, boolean includeMetadata) {
            IAggregationRecord record = get();

            context.setTime(record.getTime());
            context.setPeriod(record.getPeriod());

            IComponentRepresentationSchema representation = getSchema().getRepresentations().get(index);
            return representation.getComputer().compute(record.getValue(), context, includeTime, includeMetadata);
        }
    }
}
