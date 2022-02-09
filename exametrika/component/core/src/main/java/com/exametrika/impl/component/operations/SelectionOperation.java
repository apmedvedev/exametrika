/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.operations;


/**
 * The {@link SelectionOperation} is a selection transaction operation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SelectionOperation extends SecuredTimeSnapshotOperation {
    public SelectionOperation(long time) {
        super(time);
    }
}
