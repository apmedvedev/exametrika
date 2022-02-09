/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.common.utils.Assert;


public class UltraFastMethod implements Comparable<UltraFastMethod> {
    public final String name;
    public final String className;
    public final int index;
    public final long count;
    public final long duration;

    public UltraFastMethod(String name, String className, int index, long count, long duration) {
        Assert.notNull(name);
        Assert.notNull(className);

        this.name = name;
        this.className = className;
        this.index = index;
        this.count = count;
        this.duration = duration;
    }

    @Override
    public int compareTo(UltraFastMethod o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof UltraFastMethod))
            return false;

        UltraFastMethod method = (UltraFastMethod) o;
        return name.equals(method.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + ":" + count + ":" + duration;
    }
}