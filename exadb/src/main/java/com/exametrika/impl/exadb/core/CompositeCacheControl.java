/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.ICacheControl;


/**
 * The {@link CompositeCacheControl} is a composite cache control.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeCacheControl implements ICacheControl {
    private final List<ICacheControl> cacheControls;

    public CompositeCacheControl(List<ICacheControl> cacheControls) {
        Assert.notNull(cacheControls);

        this.cacheControls = cacheControls;
    }

    @Override
    public void validate() {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.validate();
    }

    @Override
    public void onTransactionStarted() {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.onTransactionStarted();
    }

    @Override
    public void onTransactionCommitted() {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.onTransactionCommitted();
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        boolean res = false;
        for (ICacheControl cacheControl : cacheControls)
            res = cacheControl.onBeforeTransactionRolledBack() || res;

        return res;
    }

    @Override
    public void onTransactionRolledBack() {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.onTransactionRolledBack();
    }

    @Override
    public void flush(boolean full) {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.flush(full);
    }

    @Override
    public void clear(boolean full) {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.clear(full);
    }

    @Override
    public void unloadExcessive() {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.unloadExcessive();
    }

    @Override
    public void setCachingEnabled(boolean value) {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.setCachingEnabled(value);
    }

    @Override
    public void setMaxCacheSize(String category, long value) {
        for (ICacheControl cacheControl : cacheControls)
            cacheControl.setMaxCacheSize(category, value);
    }
}
