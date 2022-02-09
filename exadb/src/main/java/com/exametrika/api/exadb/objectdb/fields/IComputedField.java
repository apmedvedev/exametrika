/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.util.Map;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IComputedField} represents a computed node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IComputedField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    <T> T get();

    /**
     * Executes expression.
     *
     * @param variables variables
     * @return return value
     */
    <T> T execute(Map<String, ? extends Object> variables);
}
