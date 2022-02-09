/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import java.io.InputStream;
import java.io.OutputStream;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IBinaryField} represents a binary node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBinaryField extends IField {
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
     * Creates input stream.
     *
     * @return input stream
     * @throws IllegalStateException if field does not have assigned blob store
     */
    InputStream createInputStream();

    /**
     * Creates output stream.
     *
     * @return output stream
     * @throws IllegalStateException if field does not have assigned blob store
     */
    OutputStream createOutputStream();

    /**
     * Reads value from field.
     *
     * @return value, or null if field does not have written object
     */
    <T> T read();

    /**
     * Writes value to field.
     *
     * @param value value to write, can be null
     */
    <T> void write(T value);

    /**
     * Clears field contents.
     */
    void clear();
}
