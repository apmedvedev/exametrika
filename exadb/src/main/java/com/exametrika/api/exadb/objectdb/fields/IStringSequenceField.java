/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;


/**
 * The {@link IStringSequenceField} represents a string sequence field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStringSequenceField extends INumericSequenceField {
    /**
     * Returns next sequence value.
     *
     * @return next sequence value
     */
    String getNextString();
}
