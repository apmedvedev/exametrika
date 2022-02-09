/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.ops.TruncationOperation;
import com.exametrika.spi.aggregator.config.schema.TruncationPolicySchemaConfiguration;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;


/**
 * The {@link TruncationOperationSchemaConfiguration} is a period cycles truncation operation configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TruncationOperationSchemaConfiguration extends JobOperationSchemaConfiguration {
    private final NameFilter spaceFilter;
    private final List<String> periods;
    private final TruncationPolicySchemaConfiguration truncationPolicy;

    public TruncationOperationSchemaConfiguration(NameFilter spaceFilter, List<String> periods, TruncationPolicySchemaConfiguration truncationPolicy) {
        Assert.notNull(truncationPolicy);

        this.spaceFilter = spaceFilter;
        this.periods = Immutables.wrap(periods);
        this.truncationPolicy = truncationPolicy;
    }

    public NameFilter getSpaceFilter() {
        return spaceFilter;
    }

    public List<String> getPeriods() {
        return periods;
    }

    public TruncationPolicySchemaConfiguration getTruncationPolicy() {
        return truncationPolicy;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public Runnable createOperation(IJobContext context) {
        return new TruncationOperation(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TruncationOperationSchemaConfiguration))
            return false;

        TruncationOperationSchemaConfiguration configuration = (TruncationOperationSchemaConfiguration) o;
        return Objects.equals(spaceFilter, configuration.spaceFilter) && Objects.equals(periods, configuration.periods) &&
                truncationPolicy.equals(configuration.truncationPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(spaceFilter, periods, truncationPolicy);
    }
}
