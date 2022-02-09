/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.json.IJsonHandler;


/**
 * The {@link INodeObject} represents a node object which implements typed access to node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface INodeObject {
    /**
     * Returns node this object bound to.
     *
     * @return node
     */
    INode getNode();

    /**
     * Is node object stale? Stale node object can not be used and must be replaced by the new loaded node object from database.
     * Node object becomes stale only when all database caches are cleared.
     *
     * @return true if node is unloaded
     */
    boolean isStale();

    /**
     * Is modification is allowed?
     *
     * @return true if modification is allowed
     */
    boolean allowModify();

    /**
     * Is deletion is allowed?
     *
     * @return true if deletion is allowed
     */
    boolean allowDeletion();

    /**
     * Validates persisted contents.
     */
    void validate();

    /**
     * Called before new node is created. Used to initialize node fields.
     *
     * @param primaryKey        primary key of node or null if node does not have primary key
     * @param args              additional creation arguments
     * @param fieldInitializers field initializers in field order
     */
    void onBeforeCreated(Object primaryKey, Object[] args, Object[] fieldInitializers);

    /**
     * Called when new node is created.
     *
     * @param primaryKey primary key of node or null if node does not have primary key
     * @param args       additional creation arguments
     */
    void onCreated(Object primaryKey, Object[] args);

    /**
     * Called when existing node is opened.
     */
    void onOpened();

    /**
     * Called on new node before node is migrated to new space.
     *
     * @param primaryKey primary key of node or null if node does not have primary key
     */
    void onBeforeMigrated(Object primaryKey);

    /**
     * Called on new node when node is migrated to new space.
     */
    void onMigrated();

    /**
     * Called when node is deleted.
     */
    void onDeleted();

    /**
     * Called when node is unloaded from memory.
     */
    void onUnloaded();

    /**
     * Called before modified node flush. Flushes all cached modifications (if any) to node.
     */
    void onBeforeFlush();

    /**
     * Called after modified node flush.
     */
    void onAfterFlush();

    /**
     * Dumps node contents to Json.
     *
     * @param json    json
     * @param context dump context
     */
    void dump(IJsonHandler json, IDumpContext context);
}
