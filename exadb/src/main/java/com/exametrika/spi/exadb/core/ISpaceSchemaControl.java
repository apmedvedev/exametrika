/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;


/**
 * The {@link ISpaceSchemaControl} represents a  control interface for space schema. Each space schema must implement
 * this interface.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISpaceSchemaControl {
    /**
     * Called when transaction has been started.
     */
    void onTransactionStarted();

    /**
     * Called when transaction has been committed.
     */
    void onTransactionCommitted();

    /**
     * Called before transaction has been rolled back.
     *
     * @return if true all internal caches must be cleared
     */
    boolean onBeforeTransactionRolledBack();

    /**
     * Called when transaction has been rolled back.
     *
     * @param clearCache if true all internal caches must be cleared
     */
    void onTransactionRolledBack();

    /**
     * Sets domain schema.
     *
     * @param domain        schema
     * @param schemaObjects schema objects map
     */
    void setParent(IDomainSchema domain, Map<String, ISchemaObject> schemaObjects);

    /**
     * Resolves schema dependencies.
     */
    void resolveDependencies();

    /**
     * Is current schema compatible with specified schema.
     *
     * @param schema       second schema
     * @param dataMigrator data migrator or null if data migrator is not used
     * @return true if schemas are compatible
     */
    boolean isCompatible(ISpaceSchema schema, IDataMigrator dataMigrator);

    /**
     * Reads schema.
     *
     * @param deserialization deserialization
     */
    void read(RawPageDeserialization deserialization);

    /**
     * Writes schema.
     *
     * @param serialization serialization
     */
    void write(RawPageSerialization serialization);

    /**
     * Begins snapshot and returns list of names of files of space schema.
     *
     * @return list of schema's file names
     */
    List<String> beginSnapshot();

    /**
     * Ends snapshot.
     */
    void endSnapshot();

    /**
     * Clears internal caches.
     */
    void clearCaches();

    /**
     * Called in transaction on timer.
     *
     * @param currentTime current time
     */
    void onTimer(long currentTime);

    /**
     * Called when new space schema is created in database.
     */
    void onCreated();

    /**
     * Called when space schema is modified in database.
     *
     * @param oldSchema    old schema
     * @param dataMigrator data migrator or null if data migrator is not used
     */
    void onModified(ISpaceSchema oldSchema, IDataMigrator dataMigrator);

    /**
     * Called after space schema is modified in database.
     *
     * @param oldSchema old schema
     */
    void onAfterModified(ISpaceSchema oldSchema);

    /**
     * Called when space schema is deleted from database.
     */
    void onDeleted();

    /**
     * Dumps contents of space.
     *
     * @param path    dump path
     * @param context dump context
     */
    void dump(File path, IDumpContext context);
}
