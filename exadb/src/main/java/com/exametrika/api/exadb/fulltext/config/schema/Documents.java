/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;


/**
 * The {@link Documents} represents a helper class to build index documents.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Documents {
    public static DocumentSchemaBuilder doc() {
        return new DocumentSchemaBuilder();
    }

    private Documents() {
    }
}