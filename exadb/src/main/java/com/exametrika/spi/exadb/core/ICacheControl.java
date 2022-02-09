/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;


/**
 * The {@link ICacheControl} represents a control of database cache.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICacheControl {
    /**
     * Validates modified cache contents before committing changes in transaction.
     */
    void validate();

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
     * Flushes unsaved contents of cache to disk.
     *
     * @param full if true explicit flush is called, if false periodic implicit flush is called
     */
    void flush(boolean full);

    /**
     * Clears cache.
     *
     * @param full true if full clear is requested, false if unused cache contents are cleared
     */
    void clear(boolean full);

    /**
     * Unloads excessive cache contents.
     */
    void unloadExcessive();

    /**
     * Enables or disables caching.
     *
     * @param value true if caching is enabled
     */
    void setCachingEnabled(boolean value);

    /**
     * Sets maximum cache size for specified category.
     *
     * @param category category
     * @param value    maximum cache size
     */
    void setMaxCacheSize(String category, long value);
}
