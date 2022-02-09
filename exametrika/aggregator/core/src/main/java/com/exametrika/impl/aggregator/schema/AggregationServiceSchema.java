/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import com.exametrika.api.aggregator.config.schema.AggregationServiceSchemaConfiguration;
import com.exametrika.impl.exadb.core.schema.DomainServiceSchema;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link AggregationServiceSchema} represents a schema of aggregation service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AggregationServiceSchema extends DomainServiceSchema {
    private IAggregationSchema aggregationSchema;

    public AggregationServiceSchema(IDatabaseContext context, AggregationServiceSchemaConfiguration configuration) {
        super(context, configuration);
    }

    @Override
    public AggregationServiceSchemaConfiguration getConfiguration() {
        return (AggregationServiceSchemaConfiguration) super.getConfiguration();
    }

    public IAggregationSchema getAggregationSchema() {
        return aggregationSchema;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        aggregationSchema = getConfiguration().getAggregationSchema().createSchema();
    }
}
