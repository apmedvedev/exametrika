/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.PercentageAccessor;
import com.exametrika.impl.aggregator.values.PercentageComputer;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link PercentageRepresentationSchemaConfiguration} is a percentage aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PercentageRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    private final String navigationType;
    private final String navigationArgs;
    private final String nodeType;
    private final String currentField;
    private final String baseField;

    public PercentageRepresentationSchemaConfiguration(String navigationType, String baseField, boolean enabled) {
        this(null, navigationType, null, null, baseField, baseField, enabled);
    }

    public PercentageRepresentationSchemaConfiguration(String name, String navigationType, String navigationArgs,
                                                       String nodeType, String currentField, String baseField, boolean enabled) {
        super(name != null ? name : createName(navigationType, navigationArgs, nodeType, currentField, baseField), enabled);

        Assert.notNull(navigationType);
        Assert.notNull(currentField);
        Assert.notNull(baseField);

        this.navigationType = navigationType;
        this.navigationArgs = navigationArgs;
        this.nodeType = nodeType;
        this.currentField = currentField;
        this.baseField = baseField;
    }

    public String getNavigationType() {
        return navigationType;
    }

    public String getNavigationArgs() {
        return navigationArgs;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getCurrentField() {
        return currentField;
    }

    public String getBaseField() {
        return baseField;
    }

    @Override
    public boolean isValueSupported() {
        return false;
    }

    @Override
    public boolean isSecondaryComputationSupported() {
        return false;
    }

    @Override
    public IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        Assert.isTrue(fieldName.isEmpty());
        return new PercentageAccessor((PercentageComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new PercentageComputer(accessorFactory.createAccessor(null, null, currentField),
                accessorFactory.createAccessor(navigationType, navigationArgs, baseField), nodeType);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PercentageRepresentationSchemaConfiguration))
            return false;

        PercentageRepresentationSchemaConfiguration configuration = (PercentageRepresentationSchemaConfiguration) o;
        return super.equals(o) && navigationType.equals(configuration.navigationType) &&
                Objects.equals(navigationArgs, configuration.navigationArgs) &&
                Objects.equals(nodeType, configuration.nodeType) && currentField.equals(configuration.currentField) &&
                baseField.equals(configuration.baseField);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(navigationType, navigationArgs, nodeType, currentField, baseField);
    }

    private static String createName(String navigationType, String navigationArgs, String nodeType, String currentField, String baseField) {
        Assert.notNull(navigationType);
        Assert.notNull(currentField);
        Assert.notNull(baseField);

        if (baseField.equals(currentField))
            return "%" + navigationType + "(" + (navigationArgs != null ? (navigationArgs + ":") : "") + baseField + ")" + (nodeType != null ? ("@" + nodeType) : "");
        else
            return "%" + navigationType + "(" + currentField + "," + (navigationArgs != null ? (navigationArgs + ":") : "") + baseField + ")" + (nodeType != null ? ("@" + nodeType) : "");
    }
}
