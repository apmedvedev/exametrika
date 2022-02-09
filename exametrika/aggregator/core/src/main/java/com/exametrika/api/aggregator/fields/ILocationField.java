/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.fields;

import com.exametrika.api.aggregator.Location;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link ILocationField} represents a location node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ILocationField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    Location get();
}
