/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.List;

import com.exametrika.api.aggregator.config.model.ForecastRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ForecastValueSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.forecast.Forecaster;
import com.exametrika.impl.aggregator.forecast.IForecaster;
import com.exametrika.impl.aggregator.forecast.IForecasterSpace;
import com.exametrika.impl.aggregator.forecast.PredictionResult;
import com.exametrika.spi.aggregator.BehaviorType;
import com.exametrika.spi.aggregator.IBehaviorTypeProvider;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link ForecastComputer} is an implementation of {@link IFieldComputer} for forecast fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ForecastComputer extends AnomalyComputer {
    private Forecaster.Parameters parameters;

    public ForecastComputer(ForecastValueSchemaConfiguration schema, ForecastRepresentationSchemaConfiguration configuration,
                            IMetricAccessor baseFieldAccessor, IComponentAccessor idFieldAccessor) {
        super(schema, configuration, baseFieldAccessor, idFieldAccessor);
    }

    public JsonArray getPredictions(IComputeContext context) {
        return getPredictions(context, -1);
    }

    @Override
    protected void doCompute(JsonObjectBuilder fields, IAnomalyValue value, IComputeContext context) {
        ForecastRepresentationSchemaConfiguration forecastConfiguration = (ForecastRepresentationSchemaConfiguration) configuration;
        if (forecastConfiguration.isComputePredictions()) {
            JsonArray predictions = getPredictions(context, value.getId());
            if (predictions != null)
                fields.put("predictions", predictions);
        }
    }

    @Override
    protected IForecaster getAnomalyDetector(int id, IComputeContext context) {
        Forecaster.Parameters parameters = getParameters(context);

        PeriodSpace periodSpace = (PeriodSpace) ((IField) context.getObject()).getNode().getSpace();
        IForecasterSpace forecasterSpace = periodSpace.getForecasterSpace();

        if (id == -1)
            return forecasterSpace.createForecaster(parameters);
        else
            return forecasterSpace.openForecaster(id, parameters);
    }

    private JsonArray getPredictions(IComputeContext context, int id) {
        if (!(context.getObject() instanceof IAggregationField))
            return null;

        ForecastRepresentationSchemaConfiguration forecastConfiguration = (ForecastRepresentationSchemaConfiguration) configuration;

        if (id == -1)
            id = getAnomalyDetectorId(context);
        if (id == -1)
            return null;

        IForecaster forecaster = getAnomalyDetector(id, context);
        List<PredictionResult> predictions = forecaster.computePredictions(forecastConfiguration.getPredictionsStepCount());
        JsonArrayBuilder array = new JsonArrayBuilder();

        IBehaviorTypeProvider behaviorTypeProvider = getBehaviorTypeProvider(context);
        for (int i = 0; i < predictions.size(); i++) {
            JsonObjectBuilder object = new JsonObjectBuilder();
            PredictionResult prediction = predictions.get(i);
            object.put("value", prediction.getValue());

            if (configuration.isComputeBehaviorTypes() && behaviorTypeProvider != null) {
                BehaviorType behaviorType = behaviorTypeProvider.findBehaviorType(prediction.getBehaviorType());
                if (behaviorType != null) {
                    JsonObjectBuilder builder = new JsonObjectBuilder();
                    builder.put("name", behaviorType.getName());
                    builder.put("labels", JsonUtils.toJson(behaviorType.getLabels()));
                    if (behaviorType.getMetadata() != null)
                        builder.put("metadata", behaviorType.getMetadata());
                    object.put("behaviorType", builder.toJson());
                }
            }

            array.add(object);
        }
        return array.toJson();
    }

    private Forecaster.Parameters getParameters(IComputeContext context) {
        if (parameters != null) {
            parameters.aggregationPeriod = context.getPeriod();
            return parameters;
        }

        parameters = new Forecaster.Parameters(context.getPeriod());

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
}
