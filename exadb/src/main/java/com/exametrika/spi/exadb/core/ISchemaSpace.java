/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import java.util.List;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.common.rawdb.IRawTransaction;


/**
 * The {@link ISchemaSpace} represents a main database schema space.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISchemaSpace extends IDataFileAllocator {
    /**
     * Returns database.
     *
     * @return database
     */
    IDatabase getDatabase();

    /**
     * Returns current database schema.
     *
     * @return current database schema
     */
    IDatabaseSchema getCurrentSchema();

    /**
     * Returns new (created but not yet comitted) database schema.
     *
     * @return new database schema
     */
    IDatabaseSchema getNewSchema();

    /**
     * Finds schema by time.
     *
     * @param time time
     * @return schema of null if schema is not found
     */
    IDatabaseSchema findSchema(long time);

    /**
     * Returns list of available schemas.
     *
     * @return list of available schemas
     */
    List<IDatabaseSchema> getSchemas();

    /**
     * Allocates space in schema space.
     *
     * @param transaction transaction
     * @param size        allocated size (must be less than page size of schema space)
     * @return file offset in schema space
     */
    long allocate(IRawTransaction transaction, int size);
}
