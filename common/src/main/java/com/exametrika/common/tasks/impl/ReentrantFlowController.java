/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ReentrantFlowController} is a {@link IFlowController} implementation that
 * does not control any flow.
 *
 * @param <T> flow type
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ReentrantFlowController<T> implements IFlowController<T> {
    private final IFlowController<T> flowController;
    private int lockCount;

    public ReentrantFlowController(IFlowController<T> flowController) {
        Assert.notNull(flowController);

        this.flowController = flowController;
    }

    @Override
    public synchronized void lockFlow(T flow) {
        if (lockCount == 0)
            flowController.lockFlow(flow);

        lockCount++;
    }

    @Override
    public synchronized void unlockFlow(T flow) {
        lockCount--;
        Assert.checkState(lockCount >= 0);

        if (lockCount == 0)
            flowController.unlockFlow(flow);
    }
}
