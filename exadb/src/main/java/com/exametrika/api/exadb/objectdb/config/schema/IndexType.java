/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

/**
 * The {@link IndexType} is a supported index type.
 *
 * @author apmedvedev
 */
public enum IndexType {
    /**
     * B+ tree index.
     */
    BTREE,

    /**
     * In-memory hash index.
     */
    HASH,

    /**
     * In-memory tree index.
     */
    TREE
}