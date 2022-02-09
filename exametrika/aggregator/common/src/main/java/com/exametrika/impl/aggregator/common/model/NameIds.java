/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import java.util.concurrent.atomic.AtomicLong;


/**
 * The {@link NameIds} represents a name identifier sequence holder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameIds {
    private static final AtomicLong nextId = new AtomicLong(1);

    public static long getNextId() {
        return nextId.getAndIncrement();
    }

    public static void reset() {
        nextId.set(1);
    }

    private NameIds() {
    }
}
