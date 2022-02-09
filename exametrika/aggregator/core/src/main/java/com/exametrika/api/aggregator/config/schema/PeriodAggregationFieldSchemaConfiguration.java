/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.fields.PeriodAggregationField;
import com.exametrika.impl.aggregator.schema.PeriodAggregationFieldSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link PeriodAggregationFieldSchemaConfiguration} represents a configuration of schema of aggregation field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodAggregationFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    public static final String METADATA_FIELD_SUFFIX = ".metadata";
    public static final String LOG_FIELD_SUFFIX = ".log";
    public static final String ANALYSIS_FIELD_SUFFIX = ".analysis";
    private final AggregationComponentTypeSchemaConfiguration componentType;
    private final String aggregationLogNodeType;

    public PeriodAggregationFieldSchemaConfiguration(String name, String alias, String description,
                                                     AggregationComponentTypeSchemaConfiguration componentType, String aggregationLogNodeType) {
        super(name, name, "Aggregation field.", Constants.COMPLEX_FIELD_AREA_DATA_SIZE, Memory.getShallowSize(PeriodAggregationField.class));

        Assert.notNull(componentType);
        Assert.isTrue(componentType.hasLog() == (aggregationLogNodeType != null));

        this.componentType = componentType;
        this.aggregationLogNodeType = aggregationLogNodeType;
    }

    public AggregationComponentTypeSchemaConfiguration getComponentType() {
        return componentType;
    }

    public String getAggregationLogNodeType() {
        return aggregationLogNodeType;
    }

    @Override
    public List<FieldSchemaConfiguration> getAdditionalFields() {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>(2);
        fields.add(new JsonFieldSchemaConfiguration(getName() + METADATA_FIELD_SUFFIX));
        if (componentType.hasLog())
            fields.add(new SingleReferenceFieldSchemaConfiguration(getName() + LOG_FIELD_SUFFIX, aggregationLogNodeType));
        if (!componentType.getAnalyzers().isEmpty())
            fields.add(new JsonFieldSchemaConfiguration(getName() + ANALYSIS_FIELD_SUFFIX));

        Assert.isTrue(!fields.isEmpty());
        return fields;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new PeriodAggregationFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof PeriodAggregationFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return null;
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodAggregationFieldSchemaConfiguration))
            return false;

        PeriodAggregationFieldSchemaConfiguration configuration = (PeriodAggregationFieldSchemaConfiguration) o;
        return super.equals(configuration) && componentType.equals(configuration.componentType) &&
                Objects.equals(aggregationLogNodeType, configuration.aggregationLogNodeType);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof PeriodAggregationFieldSchemaConfiguration))
            return false;

        PeriodAggregationFieldSchemaConfiguration configuration = (PeriodAggregationFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && componentType.equalsStructured(configuration.componentType) &&
                Objects.equals(aggregationLogNodeType, configuration.aggregationLogNodeType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentType, aggregationLogNodeType);
    }
}
