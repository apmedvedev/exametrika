/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.rawdb.RawBatchLock;


/**
 * The {@link BatchOperation} is a default implementation of batch operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BatchOperation implements IBatchOperation {
    public static final UUID EXTENTION_ID = UUID.fromString("318f35f7-7958-42fc-9013-269af9889918");
    private final int options;

    public BatchOperation() {
        options = 0;
    }

    public BatchOperation(boolean readOnly) {
        options = readOnly ? IOperation.READ_ONLY : 0;
    }

    public BatchOperation(int options) {
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
    public List<RawBatchLock> getLocks() {
        return Collections.emptyList();
    }

    @Override
    public void validate(ITransaction transaction) {
    }

    @Override
    public void onCommitted() {
    }

    @Override
    public void onRolledBack() {
    }
}
