/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.AnomalyAccessor.Type;
import com.exametrika.impl.aggregator.values.AnomalyIdAccessor;
import com.exametrika.impl.aggregator.values.ComponentAccessor;
import com.exametrika.impl.aggregator.values.ForecastAccessor;
import com.exametrika.impl.aggregator.values.ForecastComputer;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link ForecastRepresentationSchemaConfiguration} is a forecast aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ForecastRepresentationSchemaConfiguration extends AnomalyRepresentationSchemaConfiguration {
    private boolean computePredictions;
    private int predictionsStepCount;

    public ForecastRepresentationSchemaConfiguration(String name, boolean computeBehaviorTypes, boolean computePredictions,
                                                     int predictionsStepCount, boolean enabled) {
        super(name, computeBehaviorTypes, enabled);

        this.computePredictions = computePredictions;
        this.predictionsStepCount = predictionsStepCount;
    }

    public boolean isComputePredictions() {
        return computePredictions;
    }

    public int getPredictionsStepCount() {
        return predictionsStepCount;
    }

    @Override
    public IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        if (fieldName.equals("id"))
            return new AnomalyIdAccessor();
        else
            return new ForecastAccessor(getType(fieldName), (ForecastComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        ForecastValueSchemaConfiguration anomalySchema = (ForecastValueSchemaConfiguration) schema;
        IComponentAccessor idAccessor = new ComponentAccessor(accessorFactory.createAccessor(null, null, getName() +
                ".id"), accessorFactory.getMetricIndex());

        return new ForecastComputer((ForecastValueSchemaConfiguration) schema, this,
                accessorFactory.createAccessor(null, null, anomalySchema.getBaseField()), idAccessor);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ForecastRepresentationSchemaConfiguration))
            return false;

        ForecastRepresentationSchemaConfiguration configuration = (ForecastRepresentationSchemaConfiguration) o;
        return super.equals(o) && computePredictions == configuration.computePredictions && predictionsStepCount == configuration.predictionsStepCount;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(computePredictions, predictionsStepCount);
    }

    @Override
    Type getType(String fieldName) {
        if (fieldName.equals("predictions"))
            return Type.PREDICTIONS;
        else
            return super.getType(fieldName);
    }
}
