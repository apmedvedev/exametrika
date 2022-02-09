/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.actions;

import java.util.concurrent.Callable;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.spi.agent.IActionContext;
import com.exametrika.spi.agent.IActionExecutor;

/**
 * The {@link CallableActionExecutor} is a callable action executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CallableActionExecutor implements IActionExecutor {
    private static final ILogger logger = Loggers.get(CallableActionExecutor.class);

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean supports(Object action) {
        if (action instanceof Callable)
            return true;
        else
            return false;
    }

    @Override
    public void execute(IActionContext context) {
        Callable callable = context.getAction();
        try {
            Object result = callable.call();
            context.sendResult(result, null);
        } catch (Exception e) {
            context.sendError(e);

            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }
}
