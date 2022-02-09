/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IFullTextField} represents an fulltext indexable field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFullTextField extends IField {
    /**
     * Is field value modified.
     *
     * @return true if field value modified
     */
    boolean isModified();

    /**
     * Returns field value.
     *
     * @return field value
     */
    Object getFullTextValue();
}
