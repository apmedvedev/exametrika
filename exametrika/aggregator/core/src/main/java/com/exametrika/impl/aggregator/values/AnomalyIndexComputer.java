/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.FieldMetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyIndexValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.BackgroundRootSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PrimaryEntryPointSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IBackgroundRootNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IPrimaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackLogNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.AggregationContext;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.nodes.BackgroundRootNode;
import com.exametrika.impl.aggregator.nodes.PrimaryEntryPointNode;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link AnomalyIndexComputer} is an computer of anomaly index metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyIndexComputer extends ObjectComputer {
    private final AnomalyIndexValueSchemaConfiguration schema;
    private final List<AnomalyAccessorInfo> accessors = new ArrayList<AnomalyIndexComputer.AnomalyAccessorInfo>();

    public AnomalyIndexComputer(ComponentValueSchemaConfiguration componentSchema, int metricIndex) {
        Assert.notNull(componentSchema);

        for (int i = 0; i < componentSchema.getMetrics().size(); i++) {
            MetricValueSchemaConfiguration metric = componentSchema.getMetrics().get(i);
            if (!(metric instanceof FieldMetricValueSchemaConfiguration))
                continue;

            TIntList fieldIndexes = null;
            List<String> fieldNames = null;
            FieldMetricValueSchemaConfiguration fieldMetric = (FieldMetricValueSchemaConfiguration) metric;
            for (int k = 0; k < fieldMetric.getFields().size(); k++) {
                FieldValueSchemaConfiguration field = fieldMetric.getFields().get(k);
                if (!(field instanceof AnomalyValueSchemaConfiguration))
                    continue;

                if (fieldIndexes == null) {
                    fieldIndexes = new TIntArrayList();
                    fieldNames = new ArrayList<String>();
                }

                fieldIndexes.add(k);
                fieldNames.add(field.getName());
            }

            if (fieldIndexes != null) {
                AnomalyAccessorInfo accessor = new AnomalyAccessorInfo();
                accessor.metricName = metric.getName();
                accessor.metricIndex = i;
                accessor.fieldIndexes = fieldIndexes;
                accessor.fieldNames = fieldNames;

                accessors.add(accessor);
            }
        }

        schema = (AnomalyIndexValueSchemaConfiguration) componentSchema.getMetrics().get(metricIndex);
    }

    public Object getValue(IMetricValue value, boolean causes) {
        IObjectValue metricValue = (IObjectValue) value;
        if (metricValue != null) {
            JsonObject object = (JsonObject) metricValue.getObject();
            if (causes)
                return object;
            else
                return object.get("state");
        } else
            return null;
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        return getValue(value, false);
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        if (value == null)
            return;

        ObjectBuilder builder = (ObjectBuilder) value;

        int anomalyCount = 0;
        List<String> metrics = null;
        for (int i = 0; i < accessors.size(); i++) {
            AnomalyAccessorInfo accessor = accessors.get(i);
            IMetricValue metric = componentValue.getMetrics().get(accessor.metricIndex);

            List<String> fields = null;
            boolean anomaly = false;
            for (int k = 0; k < accessor.fieldIndexes.size(); k++) {
                if (metric instanceof INameValue) {
                    INameValue nameMetric = (INameValue) metric;
                    int index = accessor.fieldIndexes.get(k);
                    IAnomalyValue anomalyValue = (IAnomalyValue) nameMetric.getFields().get(index);
                    if (anomalyValue.isAnomaly()) {
                        if (fields == null)
                            fields = new ArrayList<String>();
                        fields.add(accessor.metricName + "." + accessor.fieldNames.get(k));

                        anomaly = true;
                        break;
                    }
                } else if (metric instanceof IStackValue) {
                    IStackValue stackMetric = (IStackValue) metric;
                    int index = accessor.fieldIndexes.get(k);
                    IAnomalyValue anomalyValue = (IAnomalyValue) stackMetric.getInherentFields().get(index);
                    if (anomalyValue.isAnomaly()) {
                        if (fields == null)
                            fields = new ArrayList<String>();
                        fields.add(accessor.metricName + "." + accessor.fieldNames.get(k));

                        anomaly = true;
                        break;
                    }

                    anomalyValue = (IAnomalyValue) stackMetric.getTotalFields().get(index);
                    if (anomalyValue.isAnomaly()) {
                        if (fields == null)
                            fields = new ArrayList<String>();
                        fields.add(accessor.metricName + "." + accessor.fieldNames.get(k));

                        anomaly = true;
                        break;
                    }
                }
            }

            if (anomaly) {
                anomalyCount++;

                if (metrics == null)
                    metrics = new ArrayList<String>();
                metrics.addAll(fields);
            }
        }

        boolean anomaly = anomalyCount >= schema.getMinAnomalyMetricCount();
        String state = anomaly ? "error" : "normal";

        builder.setObject(Json.object().put("state", state).putIf("causes", JsonUtils.toJson(metrics), metrics != null && anomaly).toObject());

        if (anomaly && context.getObject() instanceof IPeriodAggregationField) {
            IAggregationNode node = (IAggregationNode) ((IPeriodAggregationField) context.getObject()).getNode().getObject();
            IStackLogNode anomaliesNode = null;
            if (node instanceof IStackNode) {
                IStackNode stackNode = (IStackNode) node;
                IEntryPointNode root = stackNode.getTransactionRoot();
                if (root instanceof IBackgroundRootNode) {
                    BackgroundRootSchemaConfiguration componentType = (BackgroundRootSchemaConfiguration) root.getSchema().getConfiguration().getComponentType();
                    if (componentType.isAllowAnomaliesCorrelation())
                        anomaliesNode = ((BackgroundRootNode) root).ensureAnomalies();
                } else if (root instanceof IPrimaryEntryPointNode) {
                    PrimaryEntryPointSchemaConfiguration componentType = (PrimaryEntryPointSchemaConfiguration) root.getSchema().getConfiguration().getComponentType();
                    if (componentType.isAllowAnomaliesCorrelation())
                        anomaliesNode = ((PrimaryEntryPointNode) root).ensureAnomalies();
                }
            }

            if (anomaliesNode != null) {
                AggregationContext aggregationContext = new AggregationContext();
                aggregationContext.setDerived(node.isDerived());
                aggregationContext.setPeriod(context.getPeriod());
                aggregationContext.setTime(context.getTime());

                IPeriodAggregationField anomaliesField = anomaliesNode.getField(anomaliesNode.getSchema().getAggregationField());

                JsonObject jsonAnomaly = Json.object()
                        .put("scopeId", node.getLocation().getScopeId())
                        .put("metricId", node.getLocation().getMetricId())
                        .put("componentType", node.getSchema().getConfiguration().getComponentType().getName())
                        .putIf("causes", JsonUtils.toJson(metrics), metrics != null)
                        .toObject();

                IComponentValue anomalyValue = new ComponentValue(java.util.Collections.singletonList(new ObjectValue(jsonAnomaly)), null);
                anomaliesField.aggregate(anomalyValue, aggregationContext);
            }
        }
    }

    private static class AnomalyAccessorInfo {
        private String metricName;
        private int metricIndex;
        private List<String> fieldNames;
        private TIntList fieldIndexes;
    }
}
