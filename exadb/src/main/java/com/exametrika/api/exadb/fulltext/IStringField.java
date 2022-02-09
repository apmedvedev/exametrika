/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;


/**
 * The {@link IStringField} represents an index string field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStringField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    String get();
}
