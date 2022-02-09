/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.actions;

import com.exametrika.api.profiler.IExternalMeasurementStrategy;
import com.exametrika.api.profiler.IProfilingService;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.actions.MeasurementStrategyAction;
import com.exametrika.spi.agent.IActionContext;
import com.exametrika.spi.agent.IActionExecutor;
import com.exametrika.spi.profiler.IMeasurementStrategy;

/**
 * The {@link MeasurementStrategyActionExecutor} is a measurement strategy action executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeasurementStrategyActionExecutor implements IActionExecutor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(MeasurementStrategyActionExecutor.class);

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean supports(Object action) {
        if (action instanceof MeasurementStrategyAction)
            return true;
        else
            return false;
    }

    @Override
    public void execute(IActionContext context) {
        MeasurementStrategyAction action = context.getAction();
        IProfilingService profilingService = context.findService(IProfilingService.NAME);
        Assert.checkState(profilingService != null);

        IMeasurementStrategy strategy = profilingService.findMeasurementStrategy(action.getStrategyName());
        if (strategy instanceof IExternalMeasurementStrategy) {
            ((IExternalMeasurementStrategy) strategy).setAllowed(action.isAllowed());

            if (action.isAllowed()) {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.strategyAllowed(action.getStrategyName()));
            } else {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.strategyDenied(action.getStrategyName()));
            }
        } else if (strategy == null) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.strategyNotFound(action.getStrategyName()));
        } else {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.strategyNotExternal(action.getStrategyName()));
        }
    }

    private interface IMessages {
        @DefaultMessage("Measurement strategy ''{0}'' is allowed.")
        ILocalizedMessage strategyAllowed(String name);

        @DefaultMessage("Measurement strategy ''{0}'' is denied.")
        ILocalizedMessage strategyDenied(String name);

        @DefaultMessage("Measurement strategy ''{0}'' is not found.")
        ILocalizedMessage strategyNotFound(String name);

        @DefaultMessage("Measurement strategy ''{0}'' is not external.")
        ILocalizedMessage strategyNotExternal(String name);
    }
}
