/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.ITruncationPolicy;

/**
 * The {@link TruncationPolicySchemaConfiguration} represents a configuration of truncation policy for space data files.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TruncationPolicySchemaConfiguration extends Configuration {
    public abstract ITruncationPolicy createPolicy();
}
