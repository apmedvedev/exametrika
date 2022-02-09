/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.text.DateFormat;
import java.util.Date;

import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.config.model.AnomalyRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyValueSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.forecast.AnomalyDetector;
import com.exametrika.impl.aggregator.forecast.AnomalyResult;
import com.exametrika.impl.aggregator.forecast.IAnomalyDetector;
import com.exametrika.impl.aggregator.forecast.IAnomalyDetectorSpace;
import com.exametrika.spi.aggregator.BehaviorType;
import com.exametrika.spi.aggregator.IBehaviorTypeLabelStrategy;
import com.exametrika.spi.aggregator.IBehaviorTypeProvider;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link AnomalyComputer} is an implementation of {@link IFieldComputer} for anomaly fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AnomalyComputer implements IFieldComputer {
    private static final float ANOMALY_WARNING_THRESHOLD = 0.7f;
    protected final AnomalyValueSchemaConfiguration schema;
    protected final AnomalyRepresentationSchemaConfiguration configuration;
    private final IMetricAccessor baseFieldAccessor;
    private final IComponentAccessor idFieldAccessor;
    private AnomalyDetector.Parameters parameters;
    private IBehaviorTypeProvider behaviorTypeProvider;
    private IBehaviorTypeLabelStrategy behaviorTypeLabelStrategy;

    public AnomalyComputer(AnomalyValueSchemaConfiguration schema, AnomalyRepresentationSchemaConfiguration configuration,
                           IMetricAccessor baseFieldAccessor, IComponentAccessor idFieldAccessor) {
        Assert.notNull(schema);
        Assert.notNull(configuration);
        Assert.notNull(baseFieldAccessor);
        Assert.notNull(idFieldAccessor);

        this.schema = schema;
        this.configuration = configuration;
        this.baseFieldAccessor = baseFieldAccessor;
        this.idFieldAccessor = idFieldAccessor;
    }

    public String getAnomalyLevel(IAnomalyValue value) {
        if (value.isAnomaly())
            return "error";
        else if (value.getAnomalyScore() >= ANOMALY_WARNING_THRESHOLD)
            return "warning";
        else
            return "normal";
    }

    public String getBehaviorType(IAnomalyValue value, IComputeContext context) {
        IBehaviorTypeProvider behaviorTypeProvider = getBehaviorTypeProvider(context);
        if (behaviorTypeProvider != null) {
            BehaviorType behaviorType = behaviorTypeProvider.findBehaviorType(value.getBehaviorType());
            if (behaviorType != null)
                return behaviorType.getName();
        }

        return null;
    }

    public JsonObject getBehaviorTypeMetadata(IAnomalyValue value, IComputeContext context) {
        IBehaviorTypeProvider behaviorTypeProvider = getBehaviorTypeProvider(context);
        if (behaviorTypeProvider != null) {
            BehaviorType behaviorType = behaviorTypeProvider.findBehaviorType(value.getBehaviorType());
            if (behaviorType != null)
                return behaviorType.getMetadata();
        }

        return null;
    }

    public JsonArray getBehaviorTypeLabels(IAnomalyValue value, IComputeContext context) {
        IBehaviorTypeProvider behaviorTypeProvider = getBehaviorTypeProvider(context);
        if (behaviorTypeProvider != null) {
            BehaviorType behaviorType = behaviorTypeProvider.findBehaviorType(value.getBehaviorType());
            if (behaviorType != null)
                return JsonUtils.toJson(behaviorType.getLabels());
        }

        return null;
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IAnomalyValue value = (IAnomalyValue) v;

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("score", value.getAnomalyScore());
        fields.put("level", getAnomalyLevel(value));
        fields.put("primary", value.isPrimaryAnomaly());

        IBehaviorTypeProvider behaviorTypeProvider = getBehaviorTypeProvider(context);
        if (configuration.isComputeBehaviorTypes() && behaviorTypeProvider != null) {
            BehaviorType behaviorType = behaviorTypeProvider.findBehaviorType(value.getBehaviorType());
            if (behaviorType != null) {
                JsonObjectBuilder builder = new JsonObjectBuilder();
                builder.put("name", behaviorType.getName());
                builder.put("labels", JsonUtils.toJson(behaviorType.getLabels()));
                if (behaviorType.getMetadata() != null)
                    builder.put("metadata", behaviorType.getMetadata());
                fields.put("behaviorType", builder.toJson());
            }
        }
        doCompute(fields, value, context);
        return fields.toJson();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
        AnomalyBuilder builder = (AnomalyBuilder) value;

        Number baseValue = (Number) baseFieldAccessor.get(componentValue, metricValue, context);
        if (baseValue == null)
            baseValue = 0;
        int id = getAnomalyDetectorId(context);
        IAnomalyDetector anomalyDetector = getAnomalyDetector(id, context);

        AnomalyResult result = anomalyDetector.computeAnomaly(context.getTime(), baseValue.floatValue());

        builder.setId(anomalyDetector.getId());
        builder.setAnomalyScore(result.getAnomalyScore());
        builder.setBehaviorType(result.getBehaviorType());
        builder.setAnomaly(result.isAnomaly());
        builder.setPrimaryAnomaly(result.isPrimaryAnomaly());

        if (schema.isAnomalyAutoLabeling() && result.isAnomaly() && result.isPrimaryAnomaly())
            autoLabel(result, context);
    }

    protected void doCompute(JsonObjectBuilder fields, IAnomalyValue value, IComputeContext context) {
    }

    protected IAnomalyDetector getAnomalyDetector(int id, IComputeContext context) {
        AnomalyDetector.Parameters parameters = getParameters(context);

        PeriodSpace periodSpace = (PeriodSpace) ((IField) context.getObject()).getNode().getSpace();
        IAnomalyDetectorSpace anomalySpace = parameters.fast ? periodSpace.getFastAnomalyDetectorSpace() : periodSpace.getAnomalyDetectorSpace();

        if (id == -1)
            return anomalySpace.createAnomalyDetector(parameters);
        else
            return anomalySpace.openAnomalyDetector(id, parameters);
    }

    protected final int getAnomalyDetectorId(IComputeContext context) {
        IAggregationField field = (IAggregationField) context.getObject();
        if (field instanceof IPeriodAggregationField) {
            IPeriodAggregationField periodField = (IPeriodAggregationField) field;
            if (periodField.getLog() != null) {
                IAggregationRecord record = periodField.getLog().getCurrent();
                if (record != null)
                    return (Integer) idFieldAccessor.get(record.getValue(), context);
                IPeriodNode prevNode = ((IPeriodNode) periodField.getNode()).getPreviousPeriodNode();
                if (prevNode != null) {
                    periodField = prevNode.getField(periodField.getSchema().getIndex());
                    record = periodField.getLog().getCurrent();
                    if (record != null)
                        return (Integer) idFieldAccessor.get(record.getValue(), context);
                }
            } else {
                IPeriodNode prevNode = ((IPeriodNode) periodField.getNode()).getPreviousPeriodNode();
                if (prevNode != null) {
                    periodField = prevNode.getField(periodField.getSchema().getIndex());
                    IComponentValue value = periodField.getValue(false);
                    return (Integer) idFieldAccessor.get(value, context);
                }
            }
        } else {
            ILogAggregationField logField = (ILogAggregationField) field;
            IAggregationRecord record = logField.getCurrent();
            if (record != null)
                return (Integer) idFieldAccessor.get(record.getValue(), context);

            IPeriodNode prevNode = ((IPeriodNode) logField.getNode()).getPreviousPeriodNode();
            if (prevNode != null) {
                logField = prevNode.getField(logField.getSchema().getIndex());
                record = logField.getCurrent();
                if (record != null)
                    return (Integer) idFieldAccessor.get(record.getValue(), context);
            }
        }

        return -1;
    }

    protected final IBehaviorTypeProvider getBehaviorTypeProvider(IComputeContext context) {
        if (behaviorTypeProvider == null) {
            IField field;
            if (context.getObject() instanceof IField)
                field = (IField) context.getObject();
            else
                field = ((ILogAggregationField.IAggregationIterator) context.getObject()).getField();

            behaviorTypeProvider = field.getNode().getTransaction().findDomainService(IBehaviorTypeProvider.NAME);
        }

        return behaviorTypeProvider;
    }

    private void autoLabel(AnomalyResult result, IComputeContext context) {
        IBehaviorTypeProvider behaviorTypeProvider = getBehaviorTypeProvider(context);
        if (behaviorTypeProvider != null && !behaviorTypeProvider.containsBehaviorType(result.getBehaviorType())) {
            IBehaviorTypeLabelStrategy behaviorTypeLabelStrategy = getBehaviorTypeLabelStrategy();
            BehaviorType behaviorType = behaviorTypeLabelStrategy.getBehaviorType(context);
            behaviorTypeProvider.addBehaviorType(result.getBehaviorType(), behaviorType);
        }
    }

    private IBehaviorTypeLabelStrategy getBehaviorTypeLabelStrategy() {
        if (behaviorTypeLabelStrategy == null) {
            if (schema.getBahaviorTypeLabelStrategy() != null)
                behaviorTypeLabelStrategy = schema.getBahaviorTypeLabelStrategy().createStrategy();
            else
                behaviorTypeLabelStrategy = new DefaultBehaviorTypeLabelStrategy();
        }

        return behaviorTypeLabelStrategy;
    }

    private AnomalyDetector.Parameters getParameters(IComputeContext context) {
        if (parameters != null) {
            parameters.aggregationPeriod = context.getPeriod();
            return parameters;
        }

        parameters = new AnomalyDetector.Parameters(context.getPeriod());

        parameters.fast = schema.isFast();
        parameters.disableTypes = schema.isFast();
        parameters.sensitivityAutoAdjustment = schema.isSensitivityAutoAdjustment();
        parameters.initialSensitivity = schema.getInitialSensitivity();
        parameters.sensitivityIncrement = schema.getSensitivityIncrement();
        parameters.maxSensitivity = schema.getMaxSensitivity();
        parameters.initialLearningPeriod = schema.getInitialLearningPeriod();
        parameters.initialAdjustmentLearningPeriod = schema.getInitialAdjustmentLearningPeriod();
        parameters.anomaliesEstimationPeriod = schema.getAnomaliesEstimationPeriod();
        parameters.maxAnomaliesPerEstimationPeriodPercentage = schema.getMaxAnomaliesPerEstimationPeriodPercentage();
        parameters.maxAnomaliesPerType = (byte) schema.getMaxAnomaliesPerType();

        return parameters;
    }

    private static class DefaultBehaviorTypeLabelStrategy implements IBehaviorTypeLabelStrategy {
        private final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        @Override
        public BehaviorType getBehaviorType(IComputeContext context) {
            return new BehaviorType("anomaly-" + format.format(new Date(context.getTime())), null, null);
        }
    }
}
