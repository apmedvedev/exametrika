/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IBehaviorTypeLabelStrategy;

/**
 * The {@link BehaviorTypeLabelStrategySchemaConfiguration} represents a configuration of schema of behavior type label strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BehaviorTypeLabelStrategySchemaConfiguration extends Configuration {
    public abstract IBehaviorTypeLabelStrategy createStrategy();
}
