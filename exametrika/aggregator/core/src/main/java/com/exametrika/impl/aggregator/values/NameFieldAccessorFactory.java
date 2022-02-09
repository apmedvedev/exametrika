/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link NameFieldAccessorFactory} is a field accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameFieldAccessorFactory implements IMetricAccessorFactory {
    private final NameValueSchemaConfiguration schema;
    private final NameRepresentationSchemaConfiguration configuration;
    private final int metricIndex;
    private IComponentAccessorFactory componentAccessorFactory;

    public NameFieldAccessorFactory(NameValueSchemaConfiguration schema, NameRepresentationSchemaConfiguration configuration, int metricIndex) {
        Assert.notNull(schema);
        Assert.notNull(configuration);

        this.schema = schema;
        this.configuration = configuration;
        this.metricIndex = metricIndex;
    }

    @Override
    public int getMetricIndex() {
        return metricIndex;
    }

    @Override
    public IComponentAccessorFactory getComponentAccessorFactory() {
        return componentAccessorFactory;
    }

    @Override
    public void setComponentAccessorFactory(IComponentAccessorFactory componentAccessorFactory) {
        Assert.notNull(componentAccessorFactory);
        Assert.checkState(this.componentAccessorFactory == null);

        this.componentAccessorFactory = componentAccessorFactory;
    }

    @Override
    public IMetricAccessor createAccessor(String navigationType, String navigationArgs, String fieldName) {
        if (componentAccessorFactory.hasMetric(fieldName))
            return componentAccessorFactory.createAccessor(navigationType, navigationArgs, fieldName);
        else if (navigationType != null)
            return componentAccessorFactory.createAccessor(navigationType, navigationArgs, schema.getName() + "." + fieldName);

        int k = 0;
        for (int i = 0; i < configuration.getFields().size(); i++) {
            FieldRepresentationSchemaConfiguration field = configuration.getFields().get(i);
            FieldValueSchemaConfiguration fieldSchema = null;
            if (field.isValueSupported())
                fieldSchema = schema.getFields().get(k);

            IFieldAccessor fieldAccessor;
            if (fieldName.equals(field.getName()))
                fieldAccessor = field.createAccessor("", fieldSchema, this);
            else if (fieldName.startsWith(field.getName() + ".")) {
                String subField = fieldName.substring(field.getName().length() + 1);
                fieldAccessor = field.createAccessor(subField, fieldSchema, this);
            } else {
                if (field.isValueSupported())
                    k++;
                continue;
            }

            if (field.isValueSupported())
                return new NameStoredAccessor(k, fieldAccessor);
            else
                return new NameAccessor(fieldAccessor);
        }

        return null;
    }
}
