/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IArchivePolicy;

/**
 * The {@link ArchivePolicySchemaConfiguration} represents a configuration of archive policy for space data files.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ArchivePolicySchemaConfiguration extends Configuration {
    public abstract IArchivePolicy createPolicy();
}
