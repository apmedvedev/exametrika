/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.discovery.TransactionDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.config.model.ComponentDeletionStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link TransactionDeletionStrategySchemaConfiguration} is a transaction component deletion strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TransactionDeletionStrategySchemaConfiguration extends ComponentDeletionStrategySchemaConfiguration {
    private int retentionPeriodCount;

    public TransactionDeletionStrategySchemaConfiguration(int retentionPeriodCount) {
        this.retentionPeriodCount = retentionPeriodCount;
    }

    @Override
    public IComponentDeletionStrategy createStrategy(IDatabaseContext context) {
        return new TransactionDeletionStrategy(retentionPeriodCount, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TransactionDeletionStrategySchemaConfiguration))
            return false;

        TransactionDeletionStrategySchemaConfiguration configuration = (TransactionDeletionStrategySchemaConfiguration) o;
        return retentionPeriodCount == configuration.retentionPeriodCount;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(retentionPeriodCount);
    }
}
