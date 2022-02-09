/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.FieldMetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackValueSchemaConfiguration;
import com.exametrika.impl.aggregator.values.StackComputer;
import com.exametrika.impl.aggregator.values.StackFieldAccessorFactory;
import com.exametrika.impl.aggregator.values.StackMetricAccessorFactory;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link StackRepresentationSchemaConfiguration} is a stack representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackRepresentationSchemaConfiguration extends FieldMetricRepresentationSchemaConfiguration {
    public StackRepresentationSchemaConfiguration(String name, List<? extends FieldRepresentationSchemaConfiguration> fields) {
        super(name, fields);
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        StackValueSchemaConfiguration schema = (StackValueSchemaConfiguration) componentSchema.getMetrics().get(metricIndex);

        StackFieldAccessorFactory inherentAccessorFactory = new StackFieldAccessorFactory(true, schema, this, metricIndex);
        inherentAccessorFactory.setComponentAccessorFactory(componentAccessorFactory);
        List<IFieldComputer> inherentComputers = new ArrayList<IFieldComputer>();
        int k = 0;
        for (FieldRepresentationSchemaConfiguration field : getFields()) {
            FieldValueSchemaConfiguration childSchema = null;
            if (field.isValueSupported()) {
                childSchema = ((FieldMetricValueSchemaConfiguration) schema).getFields().get(k);
                k++;
            }

            IFieldComputer computer = null;
            if (field.isEnabled())
                computer = field.createComputer(childSchema, inherentAccessorFactory);
            inherentComputers.add(computer);
        }

        StackFieldAccessorFactory totalAccessorFactory = new StackFieldAccessorFactory(false, schema, this, metricIndex);
        totalAccessorFactory.setComponentAccessorFactory(componentAccessorFactory);
        List<IFieldComputer> totalComputers = new ArrayList<IFieldComputer>();
        k = 0;
        for (FieldRepresentationSchemaConfiguration field : getFields()) {
            FieldValueSchemaConfiguration childSchema = null;
            if (field.isValueSupported()) {
                childSchema = ((FieldMetricValueSchemaConfiguration) schema).getFields().get(k);
                k++;
            }

            IFieldComputer computer = null;
            if (field.isEnabled())
                computer = field.createComputer(childSchema, totalAccessorFactory);
            totalComputers.add(computer);
        }

        return new StackComputer(this, inherentComputers, totalComputers);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        StackValueSchemaConfiguration schema = (StackValueSchemaConfiguration) componentSchema.getMetrics().get(metricIndex);
        StackFieldAccessorFactory inherentAccessorFactory = new StackFieldAccessorFactory(true, schema, this, metricIndex);
        StackFieldAccessorFactory totalAccessorFactory = new StackFieldAccessorFactory(false, schema, this, metricIndex);

        return new StackMetricAccessorFactory(inherentAccessorFactory, totalAccessorFactory, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackRepresentationSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
