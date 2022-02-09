/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import java.util.List;


/**
 * The {@link IDocument} represents an index document as collection of fields.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IDocument {
    /**
     * Returns user defined context associated with document.
     *
     * @return user defined context associated with document or null if context is not set
     */
    <T> T getContext();

    /**
     * Returns document fields.
     *
     * @return document fields
     */
    List<IField> getFields();

    /**
     * Finds field by name.
     *
     * @param <T>       field type
     * @param fieldName field name
     * @return field or null if field with given name is not found
     */
    <T extends IField> T findField(String fieldName);
}
