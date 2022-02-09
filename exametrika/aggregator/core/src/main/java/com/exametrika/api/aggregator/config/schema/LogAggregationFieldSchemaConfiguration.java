/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.schema.LogAggregationFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link LogAggregationFieldSchemaConfiguration} represents a configuration of schema of aggregation log field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogAggregationFieldSchemaConfiguration extends StructuredBlobFieldSchemaConfiguration {
    public static final String METADATA_FIELD_SUFFIX = ".metadata";
    private final AggregationComponentTypeSchemaConfiguration componentType;

    public LogAggregationFieldSchemaConfiguration(String name, String alias, String description, String blobStoreFieldName,
                                                  AggregationComponentTypeSchemaConfiguration componentType) {
        super(name, alias, description, null, blobStoreFieldName, true, false, null, false, 0,
                Collections.<StructuredBlobIndexSchemaConfiguration>singletonList(new StructuredBlobIndexSchemaConfiguration(
                        "recordsIndex", 0, IndexType.BTREE, true, 8, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG),
                        false, true, "timeIndex")), isFullText(componentType), new AggregationRecordIndexerSchemaConfiguration());

        Assert.notNull(componentType);

        this.componentType = componentType;
    }

    public AggregationComponentTypeSchemaConfiguration getComponentType() {
        return componentType;
    }

    @Override
    public boolean hasSerializationRegistry() {
        return false;
    }

    @Override
    public List<FieldSchemaConfiguration> getAdditionalFields() {
        return Arrays.<FieldSchemaConfiguration>asList(new JsonFieldSchemaConfiguration(getName() + METADATA_FIELD_SUFFIX));
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new LogAggregationFieldSchema(this, index, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogAggregationFieldSchemaConfiguration))
            return false;

        LogAggregationFieldSchemaConfiguration configuration = (LogAggregationFieldSchemaConfiguration) o;
        return super.equals(configuration) && componentType.equals(configuration.componentType);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof LogAggregationFieldSchemaConfiguration))
            return false;

        LogAggregationFieldSchemaConfiguration configuration = (LogAggregationFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && componentType.equalsStructured(configuration.componentType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentType);
    }

    private static boolean isFullText(AggregationComponentTypeSchemaConfiguration componentType) {
        if (componentType.isLog() && ((LogSchemaConfiguration) componentType.getMetricTypes().get(0)).isFullTextIndex())
            return true;
        else
            return false;
    }
}
