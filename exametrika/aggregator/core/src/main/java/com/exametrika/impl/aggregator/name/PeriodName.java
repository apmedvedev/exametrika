/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.name;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;


/**
 * The {@link PeriodName} is a persistent measurement name.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodName implements IPeriodName {
    private static final int NAME_CACHE_SIZE = getNameCacheSize();
    private static final byte NEW_FLAG = 0x1;
    private static final byte STALE_FLAG = 0x2;
    private final long id;
    private final int cacheSize;
    private PeriodNameCache nameCache;
    private IName name;
    private Element<PeriodName> element = new Element<PeriodName>(this);
    private int refreshIndex;
    private int lastAccessTime;
    private byte flags;

    public PeriodName(PeriodNameCache nameCache, IName name, long id, boolean created) {
        Assert.notNull(nameCache);
        Assert.notNull(name);
        Assert.isTrue(id != 0);

        this.nameCache = nameCache;
        this.id = id;
        this.name = name;
        refreshIndex = nameCache.getRefreshIndex();
        this.cacheSize = ((ICacheable) name).getCacheSize() + NAME_CACHE_SIZE;
        this.flags = created ? NEW_FLAG : 0;
    }

    public int getRefreshIndex() {
        return refreshIndex;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public Element<PeriodName> getElement() {
        return element;
    }

    public boolean isNew() {
        return (flags & NEW_FLAG) != 0;
    }

    public void clearNew() {
        flags &= ~NEW_FLAG;
    }

    public void setStale() {
        flags = STALE_FLAG;
        element = null;
        nameCache = null;
        name = null;
    }

    public int getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(int time) {
        lastAccessTime = time;
    }

    @Override
    public boolean isStale() {
        boolean stale = (flags & STALE_FLAG) != 0;
        if (stale || refreshIndex == nameCache.getRefreshIndex())
            return stale;

        return refreshStale();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public IName getName() {
        return name;
    }

    @Override
    public void refresh() {
        Assert.checkState(!isStale());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodName))
            return false;

        PeriodName name = (PeriodName) o;
        return id == name.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return (name != null ? name.toString() : "<unknown>") + ":" + Long.toString(id);
    }

    private boolean refreshStale() {
        if ((flags & STALE_FLAG) != 0)
            return true;
        else {
            refreshIndex = nameCache.renewName(this, true);
            return false;
        }
    }

    private static int getNameCacheSize() {
        return Memory.getShallowSize(PeriodName.class) + Memory.getShallowSize(SimpleList.Element.class);
    }
}
