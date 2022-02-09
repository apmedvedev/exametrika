/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.List;


/**
 * The {@link Operation} is a default implementation of operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Operation implements IOperation {
    private final int options;

    public Operation() {
        options = 0;
    }

    public Operation(boolean readOnly) {
        options = readOnly ? READ_ONLY : 0;
    }

    public Operation(int options) {
        this.options = options;
    }

    @Override
    public int getOptions() {
        return options;
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public List<String> getBatchLockPredicates() {
        return null;
    }

    @Override
    public void onCommitted() {
    }

    @Override
    public void onRolledBack() {
    }
}
