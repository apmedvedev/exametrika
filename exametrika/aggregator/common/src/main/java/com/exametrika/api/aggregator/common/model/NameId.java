/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import com.exametrika.common.utils.Assert;


/**
 * The {@link NameId} is an name identifier.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameId {
    private final IName name;
    private final long id;

    public NameId(IName name, long id) {
        Assert.notNull(name);

        this.name = name;
        this.id = id;
    }

    public NameId(IName name) {
        Assert.notNull(name);

        this.name = name;
        this.id = 0;
    }

    public NameId(long id) {
        this.name = null;
        this.id = id;
    }

    public IName getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameId))
            return false;

        NameId nameId = (NameId) o;
        if (name != null)
            return name.equals(nameId.name);
        else
            return id == nameId.id;
    }

    @Override
    public int hashCode() {
        if (name != null)
            return name.hashCode();
        else
            return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return name != null ? name.toString() : Long.toString(id);
    }
}
