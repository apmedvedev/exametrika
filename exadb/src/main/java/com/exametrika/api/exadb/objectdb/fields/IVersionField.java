/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IVersionField} represents a version node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IVersionField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    long getLong();
}
