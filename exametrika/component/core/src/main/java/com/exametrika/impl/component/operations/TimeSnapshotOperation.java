/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.operations;

import com.exametrika.api.exadb.core.Operation;
import com.exametrika.spi.component.ITimeSnapshotOperation;


/**
 * The {@link TimeSnapshotOperation} is a time snapshot transaction operation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TimeSnapshotOperation extends Operation implements ITimeSnapshotOperation {
    private long time;

    public TimeSnapshotOperation(long time) {
        super(true);
        this.time = time;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }
}
