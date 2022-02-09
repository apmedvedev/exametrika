/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.fields;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IAggregationField} represents an aggregation field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IAggregationField extends IField {
    @Override
    IAggregationFieldSchema getSchema();

    /**
     * Returns compute context. Compute context is shared and must be used immediately.
     *
     * @return compute context
     */
    IComputeContext getComputeContext();

    /**
     * Returns value.
     *
     * @param copy if true immutable value copy is created
     * @return value
     */
    IComponentValue getValue(boolean copy);

    /**
     * Returns Json representation of current value.
     *
     * @param index           representation index
     * @param includeTime     if true include aggregation time and period
     * @param includeMetadata if true metadata are included
     * @return Json representation of current value
     */
    Object getRepresentation(int index, boolean includeTime, boolean includeMetadata);
}
