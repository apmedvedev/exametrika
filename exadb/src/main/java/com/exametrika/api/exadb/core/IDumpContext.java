/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IDumpContext} represents a context of dump operation.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IDumpContext {
    /**
     * Dump nodes not reachable from root node.
     */
    int DUMP_ORPHANED = 0x1;
    /**
     * Dump times.
     */
    int DUMP_TIMES = 0x2;
    /**
     * Dump node identifiers.
     */
    int DUMP_ID = 0x4;
    /**
     * Is dump compressed?
     */
    int COMPRESS = 0x8;

    /**
     * Returns dump flags.
     *
     * @return dump flags
     */
    int getFlags();

    /**
     * Returns query parameters.
     *
     * @return query parameters or null if query parameters are not set
     */
    JsonObject getQuery();

    /**
     * Is node with specified identifier already traversed by dump.
     *
     * @param nodeId node identifier
     * @return true if node with specified identifier is already traversed by dump
     */
    boolean isNodeTraversed(long nodeId);
}
