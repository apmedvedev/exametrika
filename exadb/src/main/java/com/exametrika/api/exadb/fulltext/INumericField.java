/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;


/**
 * The {@link INumericField} represents an index numeric field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INumericField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    Number get();
}
