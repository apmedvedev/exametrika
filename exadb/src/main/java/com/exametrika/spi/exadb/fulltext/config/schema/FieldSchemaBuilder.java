/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;


/**
 * The {@link FieldSchemaBuilder} is a builder of configuration of schema of index field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldSchemaBuilder {
    public abstract FieldSchemaConfiguration toConfiguration();
}
