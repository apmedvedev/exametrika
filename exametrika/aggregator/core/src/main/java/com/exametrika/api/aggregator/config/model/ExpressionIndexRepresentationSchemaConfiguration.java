/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.ExpressionIndexAccessorFactory;
import com.exametrika.impl.aggregator.values.ExpressionIndexComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link ExpressionIndexRepresentationSchemaConfiguration} is a expression index representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExpressionIndexRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    private final String expression;

    public ExpressionIndexRepresentationSchemaConfiguration(String name, String expression) {
        super(name);

        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        boolean stored = componentSchema.getMetrics().get(metricIndex) instanceof ExpressionIndexValueSchemaConfiguration;
        return new ExpressionIndexComputer(stored, expression, componentAccessorFactory);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        boolean stored = componentSchema.getMetrics().get(metricIndex) instanceof ExpressionIndexValueSchemaConfiguration;
        return new ExpressionIndexAccessorFactory(stored, metricIndex, expression);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionIndexRepresentationSchemaConfiguration))
            return false;

        ExpressionIndexRepresentationSchemaConfiguration configuration = (ExpressionIndexRepresentationSchemaConfiguration) o;
        return super.equals(configuration) && expression.equals(configuration.expression);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(expression);
    }
}
