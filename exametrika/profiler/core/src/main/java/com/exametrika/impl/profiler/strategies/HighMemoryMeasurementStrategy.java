/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.strategies;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import com.exametrika.api.profiler.config.HighMemoryMeasurementStrategyConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.profiler.IMeasurementStrategy;


/**
 * The {@link HighMemoryMeasurementStrategy} is a high-memory measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HighMemoryMeasurementStrategy implements IMeasurementStrategy, ITimerListener {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(HighMemoryMeasurementStrategy.class);
    private static MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final HighMemoryMeasurementStrategyConfiguration configuration;
    private volatile boolean allowed;
    private long startEstimationTime = Times.getCurrentTime();
    private long lastUpdateTime = startEstimationTime;
    private long totalCounter;
    private long usedCounter;

    public HighMemoryMeasurementStrategy(HighMemoryMeasurementStrategyConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.strategyDenied(configuration.getName()));
    }

    @Override
    public boolean allow() {
        return allowed;
    }

    @Override
    public void onTimer() {
        long currentTime = Times.getCurrentTime();
        if (currentTime < lastUpdateTime + 1000)
            return;

        lastUpdateTime = currentTime;

        totalCounter += memory.getHeapMemoryUsage().getMax();
        usedCounter += memory.getHeapMemoryUsage().getUsed();

        if (currentTime > startEstimationTime + configuration.getEstimationPeriod()) {
            double usagePercentage = (double) usedCounter / totalCounter * 100;
            allowed = usagePercentage > configuration.getThreshold();

            startEstimationTime = currentTime;
            totalCounter = 0;
            usedCounter = 0;

            if (logger.isLogEnabled(LogLevel.DEBUG)) {
                if (allowed)
                    logger.log(LogLevel.DEBUG, messages.strategyAllowed(configuration.getName()));
                else
                    logger.log(LogLevel.DEBUG, messages.strategyDenied(configuration.getName()));
            }
        }
    }

    private interface IMessages {
        @DefaultMessage("Measurement strategy ''{0}'' is allowed.")
        ILocalizedMessage strategyAllowed(String name);

        @DefaultMessage("Measurement strategy ''{0}'' is denied.")
        ILocalizedMessage strategyDenied(String name);
    }
}
