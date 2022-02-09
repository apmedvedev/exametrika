/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IIndexField} represents a index field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexField extends IField {
    /**
     * Returns index.
     *
     * @param <T> index type
     * @return index
     */
    <T extends IIndex> T getIndex();
}
