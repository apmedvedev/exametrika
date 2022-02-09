/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.ops.SimpleArchivePolicy;
import com.exametrika.spi.aggregator.IArchivePolicy;
import com.exametrika.spi.aggregator.config.schema.ArchivePolicySchemaConfiguration;


/**
 * The {@link SimpleArchivePolicySchemaConfiguration} is a simple archive policy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleArchivePolicySchemaConfiguration extends ArchivePolicySchemaConfiguration {
    private final long maxFileSize;

    public SimpleArchivePolicySchemaConfiguration(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    @Override
    public IArchivePolicy createPolicy() {
        return new SimpleArchivePolicy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleArchivePolicySchemaConfiguration))
            return false;

        SimpleArchivePolicySchemaConfiguration configuration = (SimpleArchivePolicySchemaConfiguration) o;
        return maxFileSize == configuration.maxFileSize;
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(maxFileSize);
    }
}
