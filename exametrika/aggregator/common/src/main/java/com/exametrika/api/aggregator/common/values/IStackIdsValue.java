/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import java.util.Set;
import java.util.UUID;


/**
 * The {@link IStackIdsValue} represents a stackIds value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IStackIdsValue extends IMetricValue {
    /**
     * Returns stack identifiers.
     *
     * @return stack identifiers or null if stack identifiers are not set
     */
    Set<UUID> getIds();
}
