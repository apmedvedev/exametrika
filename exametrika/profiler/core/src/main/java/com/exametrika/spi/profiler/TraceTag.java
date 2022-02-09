/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import java.util.UUID;

import com.exametrika.common.utils.Assert;


/**
 * The {@link TraceTag} is a trace tag.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TraceTag {
    public final String combineId;
    public final UUID stackId;
    public final long transactionId;
    public final long transactionStartTime;
    public final int variant;

    public TraceTag(String combineId, UUID stackId, long transactionId, long transactionStartTime, int variant) {
        Assert.notNull(combineId);
        Assert.notNull(stackId);

        this.combineId = combineId;
        this.stackId = stackId;
        this.transactionId = transactionId;
        this.transactionStartTime = transactionStartTime;
        this.variant = variant;
    }

    public static TraceTag fromString(String value) {
        String[] parts = value.split(";");
        Assert.isTrue(parts.length == 5);

        String combineId = parts[0];
        UUID stackId = UUID.fromString(parts[1]);
        long transactionId = Long.parseLong(parts[2]);
        long transactionStartTime = Long.parseLong(parts[3]);
        int variant = Integer.parseInt(parts[4]);

        return new TraceTag(combineId, stackId, transactionId, transactionStartTime, variant);
    }

    @Override
    public String toString() {
        return combineId + ';' + stackId.toString() + ';' + Long.toString(transactionId) + ';' + Long.toString(transactionStartTime) + ';' + variant;
    }
}
