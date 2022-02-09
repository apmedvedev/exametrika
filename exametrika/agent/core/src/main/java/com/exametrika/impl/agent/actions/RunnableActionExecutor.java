/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.actions;

import com.exametrika.spi.agent.IActionContext;
import com.exametrika.spi.agent.IActionExecutor;

/**
 * The {@link RunnableActionExecutor} is a runnable action executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class RunnableActionExecutor implements IActionExecutor {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean supports(Object action) {
        if (action instanceof Runnable)
            return true;
        else
            return false;
    }

    @Override
    public void execute(IActionContext context) {
        ((Runnable) context.getAction()).run();
    }
}
