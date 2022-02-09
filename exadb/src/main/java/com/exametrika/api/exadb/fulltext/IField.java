/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;


/**
 * The {@link IField} represents an index field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IField {
    /**
     * Returns field name.
     *
     * @return field name
     */
    String getName();

    /**
     * Returns field boost factor.
     *
     * @return field boost factor
     */
    float getBoost();
}
