/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.io.Reader;
import java.io.Writer;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link ITextField} represents a text node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ITextField extends IField {
    /**
     * Returns blob store node.
     *
     * @return blob store node or null if blob store node is not set
     */
    <T> T getStore();

    /**
     * Sets blob store node and creates blob to store field data. If field already has assigned blob store, deleting old blob.
     *
     * @param store blob store node
     */
    <T> void setStore(T store);

    /**
     * Creates text reader.
     *
     * @return text reader
     * @throws IllegalStateException if field does not have assigned blob store
     */
    Reader createReader();

    /**
     * Creates text writer.
     *
     * @return text writer
     * @throws IllegalStateException if field does not have assigned blob store
     */
    Writer createWriter();

    /**
     * Clears field contents.
     */
    void clear();
}
