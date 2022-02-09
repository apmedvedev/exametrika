/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.fields;

import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.IPeriodSpace;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.config.schema.PeriodAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.ILogAggregationField.IAggregationIterable;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.IComponentRepresentationSchema;
import com.exametrika.api.aggregator.schema.IPeriodAggregationFieldSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.aggregator.schema.PeriodAggregationFieldSchema;
import com.exametrika.impl.aggregator.values.ComputeContext;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;


/**
 * The {@link PeriodAggregationField} is an period aggregation field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class PeriodAggregationField implements IPeriodAggregationField, IFieldObject {
    private final IComplexField field;
    private IComponentValueBuilder builder;
    private long firstRecordIdOrPeriod;
    private long lastRecordIdOrTime;
    private boolean modified;
    private int lastCacheSize;

    public PeriodAggregationField(IComplexField field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public PeriodAggregationFieldSchema getSchema() {
        return (PeriodAggregationFieldSchema) field.getSchema();
    }

    @Override
    public ComputeContext getComputeContext() {
        ComputeContext context = new ComputeContext();
        context.setObject(this);

        IPeriodNode node = (IPeriodNode) getNode();
        CycleSchema cycleSchema = (CycleSchema) node.getSpace().getSchema();
        context.setNameManager(cycleSchema.getNameManager());

        IAggregationNode aggregationNode = node.getObject();
        context.setNodeType(aggregationNode.getNodeType());

        IAggregationNodeSchema nodeSchema = getSchema().getParent();
        if (!nodeSchema.getParent().getConfiguration().isNonAggregating()) {
            IPeriod period = node.getPeriod();
            Assert.checkState(period.getEndTime() != 0);

            context.setTime(period.getEndTime());
            context.setPeriod(period.getEndTime() - period.getStartTime());
        } else {
            context.setTime(lastRecordIdOrTime);
            context.setPeriod(firstRecordIdOrPeriod);
        }

        return context;
    }

    @Override
    public INode getNode() {
        return field.getNode();
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        modified = true;
        field.setModified();
    }

    @Override
    public IComponentValue getValue(boolean copy) {
        if (copy)
            return builder.toValue();
        else
            return builder;
    }

    @Override
    public IComponentValue get() {
        return getValue(true);
    }

    @Override
    public LogAggregationField getLog() {
        IPeriodAggregationFieldSchema schema = getSchema();
        PeriodAggregationFieldSchemaConfiguration configuration = schema.getConfiguration();

        if (configuration.getComponentType().hasLog()) {
            ISingleReferenceField logReferenceField = getNode().getField(schema.getLogReferenceFieldIndex());
            INode aggregationLogNode = ((INodeObject) logReferenceField.get()).getNode();
            return aggregationLogNode.getField(schema.getAggregationLog().getIndex());
        } else
            return null;
    }

    @Override
    public IAggregationIterable getPeriodRecords() {
        LogAggregationField log = getLog();
        if (log != null) {
            if (firstRecordIdOrPeriod != 0)
                return log.getRecords(firstRecordIdOrPeriod, lastRecordIdOrTime);
            else
                return log.getEmptyRecords();
        } else
            return null;
    }

    @Override
    public Object getRepresentation(int index, boolean includeTime, boolean includeMetadata) {
        IComputeContext context = getComputeContext();

        if (!getSchema().getRepresentations().isEmpty()) {
            IComponentRepresentationSchema representation = getSchema().getRepresentations().get(index);
            return representation.getComputer().compute(builder, context, includeTime, includeMetadata);
        } else
            return null;
    }

    @Override
    public void aggregate(IComponentValue value, IAggregationContext context) {
        Assert.notNull(value);
        Assert.checkState(!field.isReadOnly());

        boolean aggregated = false;
        IPeriodAggregationFieldSchema schema = getSchema();
        if (schema.isLogMetric()) {
            IObjectValue objectValue = (IObjectValue) value.getMetrics().get(0);
            if (objectValue.getObject() instanceof JsonArray) {
                JsonArray array = (JsonArray) objectValue.getObject();
                if (!array.isEmpty()) {
                    Object element = array.get(array.size() - 1);
                    schema.getAggregator().aggregate(this.builder, new ComponentValue(
                            Collections.singletonList(new ObjectValue(element)), value.getMetadata()), context);
                }

                aggregated = true;
            }
        }

        if (!aggregated)
            schema.getAggregator().aggregate(this.builder, value, context);

        if ((!context.isDerived() || context.isAggregateMetadata()) && value.getMetadata() != null && schema.getMetadataFieldIndex() != -1) {
            this.builder.setMetadata(value.getMetadata());
            IJsonField metadataField = getNode().getField(schema.getMetadataFieldIndex());
            metadataField.set(value.getMetadata());
        }

        PeriodAggregationFieldSchemaConfiguration configuration = schema.getConfiguration();
        if (configuration.getComponentType().isLog()) {
            ILogAggregationField log = getLog();
            if (log != null) {
                long startTime = ((IPeriodNode) getNode()).getPeriod().getStartTime();
                long time = context.getTime();
                if (time < startTime)
                    time = startTime;

                if (context.isDerived() && value.getMetadata() != null)
                    value = new ComponentValue(value.getMetrics(), null);

                long[] ids = log.add(value, time, context.getPeriod());
                if (ids[0] != 0) {
                    if (firstRecordIdOrPeriod == 0)
                        firstRecordIdOrPeriod = ids[0];

                    lastRecordIdOrTime = ids[1];
                }
            }
        }

        setModified();
    }

    @Override
    public List<Measurement> onPeriodClosed(IPeriod period) {
        IPeriodAggregationFieldSchema schema = getSchema();
        PeriodAggregationFieldSchemaConfiguration configuration = schema.getConfiguration();
        List<Measurement> measurements = null;
        boolean modified = false;
        if (schema.getBaseRepresentations() != null) {
            ComputeContext context = getComputeContext();

            for (int i = 0; i < schema.getBaseRepresentations().size(); i++)
                schema.getBaseRepresentations().get(i).getComputer().computeSecondary(builder, context);

            measurements = context.takeMeasurements();
            modified = true;
        }

        if (configuration.getComponentType().hasLog() && !configuration.getComponentType().isLog()) {
            ILogAggregationField log = getLog();
            if (log != null) {
                long[] ids = log.add(builder, period.getEndTime(), period.getEndTime() - period.getStartTime());
                firstRecordIdOrPeriod = ids[0];
                lastRecordIdOrTime = ids[1];
                modified = true;
            }
        }

        if (modified)
            setModified();

        return measurements;
    }

    @Override
    public void add(IComponentValue value, long time, long period) {
        Assert.notNull(value);
        Assert.checkState(!field.isReadOnly());

        if (time < this.lastRecordIdOrTime)
            time = this.lastRecordIdOrTime;
        else
            this.lastRecordIdOrTime = time;

        this.firstRecordIdOrPeriod = period;

        PeriodAggregationFieldSchema schema = getSchema();

        this.builder.set(value);

        if (value.getMetadata() != null && schema.getMetadataFieldIndex() != -1) {
            this.builder.setMetadata(value.getMetadata());
            IJsonField metadataField = getNode().getField(schema.getMetadataFieldIndex());
            metadataField.set(value.getMetadata());
        }

        if (schema.getBaseRepresentations() != null) {
            IComputeContext context = getComputeContext();
            context.setTime(time);
            context.setPeriod(period);

            for (int i = 0; i < schema.getBaseRepresentations().size(); i++)
                schema.getBaseRepresentations().get(i).getComputer().computeSecondary(builder, context);
        }

        IAggregationNodeSchema nodeSchema = schema.getParent();
        if (nodeSchema.getParent().getConfiguration().isNonAggregating() &&
                !nodeSchema.getComponentBindingStrategies().isEmpty()) {
            IAggregationNode node = getNode().getObject();
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

        setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);

        IPeriodAggregationFieldSchema schema = getSchema();
        PeriodAggregationFieldSchemaConfiguration configuration = schema.getConfiguration();
        builder = configuration.getComponentType().getMetrics().createBuilder();
        modified = true;
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
        IPeriodAggregationFieldSchema schema = getSchema();
        PeriodAggregationFieldSchemaConfiguration configuration = schema.getConfiguration();
        if (configuration.getComponentType().hasLog()) {
            IPeriodNode node = (IPeriodNode) getNode();
            IPeriodSpace space = node.getSpace();
            INodeObject aggregationLogNode = space.getCyclePeriod().findOrCreateNode(node.getLocation(), schema.getAggregationLog().getParent());

            ISingleReferenceField logReferenceField = getNode().getField(schema.getLogReferenceFieldIndex());
            logReferenceField.set(aggregationLogNode);
        }
    }

    @Override
    public void onOpened() {
        readValue();
    }

    @Override
    public void onDeleted() {
        Assert.supports(false);
    }

    @Override
    public void onUnloaded() {
        builder = null;
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        writeValue();

        modified = false;
    }

    private void readValue() {
        IPeriodAggregationFieldSchema schema = getSchema();

        JsonObject metadata = null;
        if (schema.getMetadataFieldIndex() != -1) {
            IJsonField metadataField = getNode().getField(schema.getMetadataFieldIndex());
            metadata = (JsonObject) metadataField.get();
        }

        IFieldDeserialization fieldDeserialization = field.createDeserialization();
        builder = (IComponentValueBuilder) schema.getSerializer().deserialize(fieldDeserialization, false, metadata);
        firstRecordIdOrPeriod = fieldDeserialization.readLong();
        lastRecordIdOrTime = fieldDeserialization.readLong();

        updateCacheSize();
    }

    private void writeValue() {
        IPeriodAggregationFieldSchema schema = getSchema();

        IFieldSerialization fieldSerialization = field.createSerialization();
        schema.getSerializer().serialize(fieldSerialization, builder, false);
        fieldSerialization.writeLong(firstRecordIdOrPeriod);
        fieldSerialization.writeLong(lastRecordIdOrTime);

        updateCacheSize();
    }

    private void updateCacheSize() {
        int cacheSize = ((ICacheable) builder).getCacheSize();

        if (cacheSize != lastCacheSize) {
            getNode().updateCacheSize(cacheSize - lastCacheSize);
            lastCacheSize = cacheSize;
        }
    }
}
