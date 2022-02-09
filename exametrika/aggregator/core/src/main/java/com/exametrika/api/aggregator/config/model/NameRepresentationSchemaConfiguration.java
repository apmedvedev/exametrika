/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.FieldMetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.impl.aggregator.values.NameComputer;
import com.exametrika.impl.aggregator.values.NameFieldAccessorFactory;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link NameRepresentationSchemaConfiguration} is a name representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameRepresentationSchemaConfiguration extends FieldMetricRepresentationSchemaConfiguration {
    public NameRepresentationSchemaConfiguration(String name, List<? extends FieldRepresentationSchemaConfiguration> fields) {
        super(name, fields);
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        NameValueSchemaConfiguration schema = (NameValueSchemaConfiguration) componentSchema.getMetrics().get(metricIndex);
        NameFieldAccessorFactory accessorFactory = new NameFieldAccessorFactory(schema, this, metricIndex);
        accessorFactory.setComponentAccessorFactory(componentAccessorFactory);
        List<IFieldComputer> computers = new ArrayList<IFieldComputer>();
        int k = 0;
        for (FieldRepresentationSchemaConfiguration field : getFields()) {
            FieldValueSchemaConfiguration childSchema = null;
            if (field.isValueSupported()) {
                childSchema = ((FieldMetricValueSchemaConfiguration) schema).getFields().get(k);
                k++;
            }
            IFieldComputer computer = null;
            if (field.isEnabled())
                computer = field.createComputer(childSchema, accessorFactory);
            computers.add(computer);
        }

        return new NameComputer(this, computers);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        NameValueSchemaConfiguration schema = (NameValueSchemaConfiguration) componentSchema.getMetrics().get(metricIndex);
        return new NameFieldAccessorFactory(schema, this, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameRepresentationSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
