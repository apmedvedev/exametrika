/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import com.exametrika.common.rawdb.impl.RawDatabaseInterceptor;


/**
 * The {@link DatabaseInterceptor} is a database interceptor.
 *
 * @author AndreyM
 * @threadsafety Implementations of this class and its methods are thread safe.
 */
public class DatabaseInterceptor extends RawDatabaseInterceptor {
    public static DatabaseInterceptor INSTANCE = new DatabaseInterceptor();

    public boolean onBeforeFullTextAdded(int id) {
        return false;
    }

    public void onAfterFullTextAdded(int id) {
    }

    public boolean onBeforeFullTextUpdated(int id) {
        return false;
    }

    public void onAfterFullTextUpdated(int id) {
    }

    public boolean onBeforeFullTextDeleted(int id) {
        return false;
    }

    public void onAfterFullTextDeleted(int id) {
    }

    public boolean onBeforeFullTextSearched(int id) {
        return false;
    }

    public void onAfterFullTextSearched(int id) {
    }

    public boolean onBeforeFullTextSearcherUpdated(int id) {
        return false;
    }

    public void onAfterFullTextSearcherUpdated(int id) {
    }

    public boolean onBeforeFullTextCommitted(int id) {
        return false;
    }

    public void onAfterFullTextCommitted(int id) {
    }

    public int onNodeCacheCreated(int id, String cacheName) {
        return 0;
    }

    public void onNodeCacheClosed(int id) {
    }

    public void onNodeCache(int id, long cacheSize, long maxCacheSize, long quota) {
    }

    public void onNodeLoaded(int id) {
    }

    public void onNodeUnloaded(int id, boolean byTimer) {
    }

    public void onSelected(int id, long selectedTime, int selectedSize) {
    }
}
