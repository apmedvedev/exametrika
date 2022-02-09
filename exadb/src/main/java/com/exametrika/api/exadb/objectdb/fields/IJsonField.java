/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.common.json.IJsonCollection;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IJsonField} represents a JSON node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJsonField extends IField {
    /**
     * Returns field value.
     *
     * @param <T> JSON type (object or array)
     * @return field value
     */
    @Override
    <T> T get();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void set(IJsonCollection value);
}
