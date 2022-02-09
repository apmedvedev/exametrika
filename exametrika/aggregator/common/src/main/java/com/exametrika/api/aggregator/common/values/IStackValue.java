/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import java.util.List;


/**
 * The {@link IStackValue} represents a stack field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IStackValue extends IMetricValue {
    /**
     * Returns inherent fields.
     *
     * @return inherent fields
     */
    List<? extends IFieldValue> getInherentFields();

    /**
     * Returns total fields.
     *
     * @return total fields
     */
    List<? extends IFieldValue> getTotalFields();
}
