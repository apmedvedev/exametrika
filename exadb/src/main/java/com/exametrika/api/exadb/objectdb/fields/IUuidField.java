/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.util.UUID;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IUuidField} represents a UUID node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IUuidField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    UUID get();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void set(UUID value);
}
