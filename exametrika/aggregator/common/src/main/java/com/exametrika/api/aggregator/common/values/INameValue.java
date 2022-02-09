/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import java.util.List;


/**
 * The {@link INameValue} represents a name field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface INameValue extends IMetricValue {
    /**
     * Returns fields.
     *
     * @return fields
     */
    List<? extends IFieldValue> getFields();
}
