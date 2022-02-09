/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import com.exametrika.common.json.IJsonCollection;

/**
 * The {@link IMetricValue} represents an immutable measurement metric value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IMetricValue {
    IJsonCollection toJson();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
