/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.strategies;

import org.hyperic.sigar.SigarException;

import com.exametrika.api.profiler.config.HighCpuMeasurementStrategyConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.profiler.IMeasurementStrategy;


/**
 * The {@link HighCpuMeasurementStrategy} is a high-cpu measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HighCpuMeasurementStrategy implements IMeasurementStrategy, ITimerListener {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(HighCpuMeasurementStrategy.class);
    private final HighCpuMeasurementStrategyConfiguration configuration;
    private volatile boolean allowed;
    private long startEstimationTime = Times.getCurrentTime();
    private long lastUpdateTime = startEstimationTime;
    private long totalCounter;
    private long usedCounter;

    public HighCpuMeasurementStrategy(HighCpuMeasurementStrategyConfiguration configuration) {
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

        try {
            totalCounter += SigarHolder.instance.getCpu().getTotal();
            usedCounter += SigarHolder.instance.getProcTime(SigarHolder.instance.getPid()).getTotal();
        } catch (SigarException e) {
            Exceptions.wrapAndThrow(e);
        }

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
