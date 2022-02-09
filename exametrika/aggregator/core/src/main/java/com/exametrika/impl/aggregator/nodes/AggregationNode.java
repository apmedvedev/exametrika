/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.ILogAggregationField.IAggregationIterator;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link AggregationNode} is an aggregation node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AggregationNode extends PeriodNodeObject implements IAggregationNode {
    private static final int DERIVED_FLAG = 0x1;
    protected static final int FLAGS_FIELD = 1;

    public AggregationNode(INode node) {
        super(node);
    }

    @Override
    public IAggregationNodeSchema getSchema() {
        return (IAggregationNodeSchema) getNode().getSchema();
    }

    @Override
    public String getComponentType() {
        return getSchema().getConfiguration().getComponentType().getName();
    }

    @Override
    public String getNodeType() {
        return isDerived() ? "derived" : "default";
    }

    @Override
    public int getFlags() {
        INumericField field = getField(FLAGS_FIELD);
        return field.getInt();
    }

    @Override
    public boolean isDerived() {
        return (getFlags() & DERIVED_FLAG) != 0;
    }

    public void setDerived() {
        INumericField flags = getField(FLAGS_FIELD);
        int value = flags.getInt() | DERIVED_FLAG;
        flags.setInt(value);
    }

    @Override
    public IAggregationField getAggregationField() {
        IAggregationNodeSchema schema = getSchema();
        return getField(schema.getAggregationField());
    }

    @Override
    public JsonObject getMetadata() {
        int metadataIndex = getSchema().getAggregationField().getMetadataFieldIndex();
        if (metadataIndex != -1) {
            IJsonField metadataField = getField(metadataIndex);
            return metadataField.get();
        } else
            return null;
    }

    public void init(IPeriodNameManager nameManager, JsonObject metadata, boolean aggregatingPeriod) {
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("componentType");
        json.value(getSchema().getConfiguration().getComponentType().getName());

        boolean includeTime = (context.getFlags() & IDumpContext.DUMP_TIMES) != 0;
        List<String> flagsList = new ArrayList<String>();
        buildFlagsList(getFlags(), flagsList);
        if (!flagsList.isEmpty()) {
            json.key("flags");
            json.value(flagsList.toString());
        }

        IAggregationField aggregationField = getField(getSchema().getAggregationField());
        if (aggregationField instanceof IPeriodAggregationField) {
            IPeriodAggregationField periodField = (IPeriodAggregationField) aggregationField;
            json.key("measurements");
            JsonSerializers.write(json, periodField.getRepresentation(0, includeTime, false));
        } else if (aggregationField instanceof ILogAggregationField) {
            ILogAggregationField logField = (ILogAggregationField) aggregationField;
            if (context.getQuery() != null && context.getQuery().get("dumpLogs", false).equals(true)) {
                json.key("measurements");
                json.startArray();
                for (IAggregationIterator it = logField.getRecords().iterator(); it.hasNext(); ) {
                    it.next();
                    JsonSerializers.write(json, it.getRepresentation(0, includeTime, false));
                }
                json.endArray();
            } else {
                json.key("lastMeasurements");
                JsonSerializers.write(json, logField.getRepresentation(0, includeTime, false));
            }
        } else {
            Assert.error();
            return;
        }

        IAggregationFieldSchema aggregationFieldSchema = aggregationField.getSchema();
        if (aggregationFieldSchema.getMetadataFieldIndex() != -1) {
            IJsonField metadataField = getField(aggregationFieldSchema.getMetadataFieldIndex());
            if (metadataField.get() != null) {
                json.key("metadata");
                JsonSerializers.write(json, metadataField.get());
            }
        }
    }

    @Override
    public String toString() {
        IAggregationNodeSchema schema = getSchema();
        AggregationComponentTypeSchemaConfiguration componentType = schema.getConfiguration().getComponentType();

        IField valueField = getField(schema.getAggregationField());
        String fieldValue = "";
        if (valueField instanceof IPeriodAggregationField) {
            List<String> metricTypes = new ArrayList<String>();
            for (MetricTypeSchemaConfiguration metricType : componentType.getMetricTypes())
                metricTypes.add(metricType.getName());

            IComponentValue value = ((IPeriodAggregationField) valueField).get();
            fieldValue = "\n" + value.toJson(metricTypes, value.getMetadata());
        }

        List<String> flagsList = new ArrayList<String>();
        buildFlagsList(getFlags(), flagsList);
        String value = "\ncomponentType:" + componentType.getName() + ", flags:" + flagsList.toString() + fieldValue;
        return super.toString() + value;
    }

    protected void buildFlagsList(int flags, List<String> list) {
        if ((flags & DERIVED_FLAG) != 0)
            list.add("derived");
    }
}