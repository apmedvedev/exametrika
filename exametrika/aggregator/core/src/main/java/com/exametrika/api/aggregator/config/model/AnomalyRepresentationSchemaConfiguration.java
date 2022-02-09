/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.AnomalyAccessor;
import com.exametrika.impl.aggregator.values.AnomalyAccessor.Type;
import com.exametrika.impl.aggregator.values.AnomalyComputer;
import com.exametrika.impl.aggregator.values.AnomalyIdAccessor;
import com.exametrika.impl.aggregator.values.ComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link AnomalyRepresentationSchemaConfiguration} is a anomaly aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AnomalyRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    private final boolean computeBehaviorTypes;

    public AnomalyRepresentationSchemaConfiguration(String name, boolean computeBehaviorTypes, boolean enabled) {
        super(name, enabled);
        this.computeBehaviorTypes = computeBehaviorTypes;
    }

    public boolean isComputeBehaviorTypes() {
        return computeBehaviorTypes;
    }

    @Override
    public boolean isValueSupported() {
        return true;
    }

    @Override
    public boolean isSecondaryComputationSupported() {
        return true;
    }

    @Override
    public IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        if (fieldName.equals("id"))
            return new AnomalyIdAccessor();
        else
            return new AnomalyAccessor(getType(fieldName), (AnomalyComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        AnomalyValueSchemaConfiguration anomalySchema = (AnomalyValueSchemaConfiguration) schema;
        IComponentAccessor idAccessor = new ComponentAccessor(accessorFactory.createAccessor(null, null, getName() + ".id"),
                accessorFactory.getMetricIndex());

        return new AnomalyComputer((AnomalyValueSchemaConfiguration) schema, this,
                accessorFactory.createAccessor(null, null, anomalySchema.getBaseField()), idAccessor);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyRepresentationSchemaConfiguration))
            return false;

        AnomalyRepresentationSchemaConfiguration configuration = (AnomalyRepresentationSchemaConfiguration) o;
        return super.equals(o) && computeBehaviorTypes == configuration.computeBehaviorTypes;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(computeBehaviorTypes);
    }

    Type getType(String fieldName) {
        if (fieldName.equals("score"))
            return Type.ANOMALY_SCORE;
        else if (fieldName.equals("level"))
            return Type.ANOMALY_LEVEL;
        else if (fieldName.equals("anomaly"))
            return Type.ANOMALY;
        else if (fieldName.equals("primary"))
            return Type.PRIMARY_ANOMALY;
        else if (fieldName.equals("behaviorType"))
            return Type.BEHAVIOR_TYPE;
        else if (fieldName.equals("labels"))
            return Type.BEHAVIOR_TYPE_LABELS;
        else if (fieldName.equals("metadata"))
            return Type.BEHAVIOR_TYPE_METADATA;
        else {
            Assert.isTrue(false);
            return null;
        }
    }
}
