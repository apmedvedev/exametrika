/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link IFieldValueBuilder} represents a mutable value builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public interface IFieldValueBuilder extends IFieldValue, ICacheable {
    /**
     * Assigns value to this builder.
     *
     * @param value value to set
     */
    void set(IFieldValue value);

    /**
     * Converts value to immutable implementation of {@link IFieldValue}.
     *
     * @return value
     */
    IFieldValue toValue();

    /**
     * Clears measurement results.
     */
    void clear();

    /**
     * Normalizes end value.
     *
     * @param count count
     */
    void normalizeEnd(long count);

    /**
     * Normalizes derived value.
     *
     * @param fieldSchemaConfiguration field schema configuration
     * @param sum                      sum
     */
    void normalizeDerived(FieldValueSchemaConfiguration fieldSchemaConfiguration, long sum);
}
