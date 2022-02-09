/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import java.util.List;


/**
 * The {@link SecuredOperation} is a default implementation of secured operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class SecuredOperation implements ISecuredOperation {
    private final int options;

    public SecuredOperation() {
        options = 0;
    }

    public SecuredOperation(boolean readOnly) {
        options = readOnly ? READ_ONLY : 0;
    }

    public SecuredOperation(int options) {
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
