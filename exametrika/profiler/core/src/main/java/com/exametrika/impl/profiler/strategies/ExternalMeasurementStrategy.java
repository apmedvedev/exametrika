/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.strategies;

import com.exametrika.api.profiler.IExternalMeasurementStrategy;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;


/**
 * The {@link ExternalMeasurementStrategy} is an external measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExternalMeasurementStrategy implements IExternalMeasurementStrategy, ITimerListener {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ExternalMeasurementStrategy.class);

    private final ExternalMeasurementStrategyConfiguration configuration;
    private boolean allowed;
    private boolean warmedUp = true;
    private long startTime = Times.getCurrentTime();

    public ExternalMeasurementStrategy(ExternalMeasurementStrategyConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        allowed = configuration.isEnabled();
        if (configuration.getWarmupDelay() == 0)
            warmedUp = false;

        if (logger.isLogEnabled(LogLevel.DEBUG)) {
            if (allowed)
                logger.log(LogLevel.DEBUG, messages.strategyAllowed(configuration.getName()));
            else
                logger.log(LogLevel.DEBUG, messages.strategyDenied(configuration.getName()));

            if (warmedUp)
                logger.log(LogLevel.DEBUG, messages.warmupStarted(configuration.getName()));
        }
    }

    @Override
    public boolean allow() {
        return allowed || warmedUp;
    }

    @Override
    public void setAllowed(boolean value) {
        allowed = value;

        if (logger.isLogEnabled(LogLevel.DEBUG)) {
            if (allowed)
                logger.log(LogLevel.DEBUG, messages.strategyAllowed(configuration.getName()));
            else
                logger.log(LogLevel.DEBUG, messages.strategyDenied(configuration.getName()));
        }
    }

    @Override
    public void onTimer() {
        if (!warmedUp)
            return;

        if (Times.getCurrentTime() > startTime + configuration.getWarmupDelay()) {
            warmedUp = false;

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.warmupFinished(configuration.getName()));
        }
    }

    private interface IMessages {
        @DefaultMessage("Measurement strategy ''{0}'' is allowed.")
        ILocalizedMessage strategyAllowed(String name);

        @DefaultMessage("Measurement strategy ''{0}'' is denied.")
        ILocalizedMessage strategyDenied(String name);

        @DefaultMessage("Warmup period of measurement strategy ''{0}'' has been started.")
        ILocalizedMessage warmupStarted(String name);

        @DefaultMessage("Warmup period of measurement strategy ''{0}'' has been finished.")
        ILocalizedMessage warmupFinished(String name);
    }
}
