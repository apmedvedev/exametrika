/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.util.List;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link ITagField} represents a tag node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ITagField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    List<String> get();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void set(List<String> value);
}
