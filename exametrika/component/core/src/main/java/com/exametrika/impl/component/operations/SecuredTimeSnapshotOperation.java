/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.operations;

import com.exametrika.api.exadb.security.SecuredOperation;
import com.exametrika.spi.component.ITimeSnapshotOperation;


/**
 * The {@link SecuredTimeSnapshotOperation} is a secured time snapshot transaction operation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SecuredTimeSnapshotOperation extends SecuredOperation implements ITimeSnapshotOperation {
    private long time;

    public SecuredTimeSnapshotOperation(long time) {
        super(READ_ONLY | DISABLE_NODES_UNLOAD);
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
