/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;


/**
 * The {@link IObjectValue} represents a object field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IObjectValue extends IMetricValue {
    /**
     * Returns value of one of the supported Json types.
     *
     * @return value
     */
    Object getObject();
}
