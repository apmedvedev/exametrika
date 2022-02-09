/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.strategies;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;


/**
 * The {@link MeasurementStrategyManager} is a manager of measurement strategies.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementStrategyManager {
    private ProfilerConfiguration configuration;
    private volatile Map<String, IMeasurementStrategy> measurementStrategies = new HashMap<String, IMeasurementStrategy>();

    public <T extends IMeasurementStrategy> T findMeasurementStrategy(String name) {
        Assert.notNull(name);

        Map<String, IMeasurementStrategy> measurementStrategies = this.measurementStrategies;
        return (T) measurementStrategies.get(name);
    }

    public synchronized void setConfiguration(ProfilerConfiguration configuration) {
        if (this.configuration == null) {
            Map<String, IMeasurementStrategy> measurementStrategies = new HashMap<String, IMeasurementStrategy>();
            for (MeasurementStrategyConfiguration measurementStrategy : configuration.getMeasurementStrategies())
                measurementStrategies.put(measurementStrategy.getName(), measurementStrategy.createStrategy());

            this.measurementStrategies = measurementStrategies;
        } else if (configuration == null)
            measurementStrategies = new HashMap<String, IMeasurementStrategy>();
        else {
            Map<String, MeasurementStrategyConfiguration> oldStrategiesMap = new LinkedHashMap<String, MeasurementStrategyConfiguration>();
            for (MeasurementStrategyConfiguration strategy : this.configuration.getMeasurementStrategies())
                oldStrategiesMap.put(strategy.getName(), strategy);

            Map<String, MeasurementStrategyConfiguration> newStrategiesMap = new LinkedHashMap<String, MeasurementStrategyConfiguration>();
            for (MeasurementStrategyConfiguration strategy : configuration.getMeasurementStrategies())
                newStrategiesMap.put(strategy.getName(), strategy);

            Set<MeasurementStrategyConfiguration> newStrategies = new LinkedHashSet<MeasurementStrategyConfiguration>();
            Set<String> removedStrategies = new LinkedHashSet<String>();

            for (MeasurementStrategyConfiguration strategy : this.configuration.getMeasurementStrategies()) {
                MeasurementStrategyConfiguration newStrategy = newStrategiesMap.get(strategy.getName());
                if (newStrategy == null)
                    removedStrategies.add(strategy.getName());
                else if (!strategy.equals(newStrategy)) {
                    removedStrategies.add(strategy.getName());
                    newStrategies.add(newStrategy);
                }
            }

            for (MeasurementStrategyConfiguration strategy : configuration.getMeasurementStrategies()) {
                if (!oldStrategiesMap.containsKey(strategy.getName()))
                    newStrategies.add(strategy);
            }

            Map<String, IMeasurementStrategy> measurementStrategies = new HashMap<String, IMeasurementStrategy>();

            for (Map.Entry<String, IMeasurementStrategy> entry : this.measurementStrategies.entrySet()) {
                if (!removedStrategies.contains(entry.getKey()))
                    measurementStrategies.put(entry.getKey(), entry.getValue());
            }

            for (MeasurementStrategyConfiguration strategy : newStrategies)
                measurementStrategies.put(strategy.getName(), strategy.createStrategy());

            this.measurementStrategies = measurementStrategies;
        }

        this.configuration = configuration;
    }

    public void onTimer() {
        Map<String, IMeasurementStrategy> measurementStrategies = this.measurementStrategies;
        for (IMeasurementStrategy strategy : measurementStrategies.values()) {
            if (strategy instanceof ITimerListener)
                ((ITimerListener) strategy).onTimer();
        }
    }

    public void dump(Json json, int flags) {
        Json strategiesDump = json.putObject("measurementStrategies");
        for (IMeasurementStrategy strategy : measurementStrategies.values()) {
            if (!(strategy instanceof IDumpProvider))
                continue;

            IDumpProvider dumpProvider = (IDumpProvider) strategy;
            strategiesDump.put(dumpProvider.getName(), dumpProvider.dump(flags));
        }
    }
}
