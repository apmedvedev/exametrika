/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import java.util.Collection;


/**
 * The {@link IInstanceValue} represents a instance field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IInstanceValue extends IFieldValue {
    /**
     * Returns instance records.
     *
     * @return instance records
     */
    Collection<? extends IInstanceRecord> getRecords();
}
