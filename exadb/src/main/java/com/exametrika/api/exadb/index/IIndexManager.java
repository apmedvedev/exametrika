/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import java.util.List;

import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IIndexManager} represents a manager of indexes.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IIndexManager {
    String NAME = IIndexManager.class.getName();

    class IndexInfo {
        public final IndexSchemaConfiguration schema;
        public final String filePrefix;
        public final int id;

        public IndexInfo(IndexSchemaConfiguration schema, String filePrefix, int id) {
            this.schema = schema;
            this.filePrefix = filePrefix;
            this.id = id;
        }
    }

    /**
     * Creates a new index.
     *
     * @param filePrefix index file prefix
     * @param schema     index schema configuration
     * @return index
     */
    <T extends IIndex> T createIndex(String filePrefix, IndexSchemaConfiguration schema);

    /**
     * Returns index by identifier.
     *
     * @param id index identifier
     * @return index
     * @throws IllegalArgumentException if index is not found
     */
    <T extends IIndex> T getIndex(int id);

    /**
     * Finds index info by index identifier.
     *
     * @param id index identifier
     * @return index info or null if index is not found
     */
    IndexInfo findIndex(int id);

    /**
     * Returns all indexes, registered in index manager.
     *
     * @return all indexes, registered in index manager
     */
    List<IndexInfo> getIndexes();
}
