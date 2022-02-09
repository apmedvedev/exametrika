/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.AggregationLogFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.DocumentSchemaFactoryConfiguration;


/**
 * The {@link LogSchemaConfiguration} is a log schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogSchemaConfiguration extends MetricTypeSchemaConfiguration {
    private final AggregationLogFilterSchemaConfiguration filter;
    private final boolean fullTextIndex;
    private final DocumentSchemaFactoryConfiguration documentSchemaFactory;
    private final List<AggregationLogTransformerSchemaConfiguration> transformers;

    public LogSchemaConfiguration(String name, List<? extends ObjectRepresentationSchemaConfiguration> representations,
                                  AggregationLogFilterSchemaConfiguration filter, List<? extends AggregationLogTransformerSchemaConfiguration> transformers,
                                  boolean fullTextIndex, DocumentSchemaFactoryConfiguration documentSchemaFactory) {
        super(name, new ObjectValueSchemaConfiguration(name), representations);

        Assert.isTrue(fullTextIndex == (documentSchemaFactory != null));
        Assert.notNull(transformers);

        this.filter = filter;
        this.transformers = Immutables.wrap(transformers);
        this.fullTextIndex = fullTextIndex;
        this.documentSchemaFactory = documentSchemaFactory;
    }

    public AggregationLogFilterSchemaConfiguration getFilter() {
        return filter;
    }

    public List<AggregationLogTransformerSchemaConfiguration> getTransformers() {
        return transformers;
    }

    public boolean isFullTextIndex() {
        return fullTextIndex;
    }

    public DocumentSchemaFactoryConfiguration getDocument() {
        return documentSchemaFactory;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogSchemaConfiguration))
            return false;

        LogSchemaConfiguration configuration = (LogSchemaConfiguration) o;
        return super.equals(o) &&
                Objects.equals(filter, configuration.filter) && transformers.equals(configuration.transformers) &&
                fullTextIndex == configuration.fullTextIndex &&
                Objects.equals(documentSchemaFactory, configuration.documentSchemaFactory);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(filter, transformers, fullTextIndex, documentSchemaFactory);
    }
}
