/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import com.exametrika.api.aggregator.config.model.AggregationSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.AggregationService;
import com.exametrika.impl.aggregator.schema.AggregationServiceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link AggregationServiceSchemaConfiguration} represents a configuration of aggregation service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AggregationServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String NAME = "AggregationService";
    private final AggregationSchemaConfiguration aggregationSchema;

    public AggregationServiceSchemaConfiguration(AggregationSchemaConfiguration aggregationSchema) {
        super(NAME, NAME, "Aggregation service.");

        Assert.notNull(aggregationSchema);

        this.aggregationSchema = aggregationSchema;
    }

    public AggregationSchemaConfiguration getAggregationSchema() {
        return aggregationSchema;
    }

    @Override
    public IDomainServiceSchema createSchema(IDatabaseContext context) {
        return new AggregationServiceSchema(context, this);
    }

    @Override
    public IDomainService createService() {
        return new AggregationService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        return (T) new AggregationServiceSchemaConfiguration(aggregationSchema.combine(((AggregationServiceSchemaConfiguration) schema).aggregationSchema));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AggregationServiceSchemaConfiguration))
            return false;

        AggregationServiceSchemaConfiguration configuration = (AggregationServiceSchemaConfiguration) o;
        return super.equals(configuration) && aggregationSchema.equals(configuration.aggregationSchema);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(aggregationSchema);
    }
}