/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.util.Map;
import java.util.Random;

import com.exametrika.api.tester.config.SimpleTestActionConfiguration;
import com.exametrika.common.tasks.ITaskContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.tester.ITestAction;


/**
 * The {@link SimpleTestAction} is a simple test action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleTestAction implements ITestAction {
    private final SimpleTestActionConfiguration configuration;
    private final Random random;
    private long nextTime;

    public SimpleTestAction(SimpleTestActionConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        nextTime = Times.getCurrentTime() + configuration.getStartDelay();
        if (configuration.isRandom())
            random = new Random();
        else
            random = null;
    }

    @Override
    public boolean canActivate(long currentTime, ITaskContext context) {
        if (currentTime < nextTime)
            return false;

        if (!configuration.isRandom())
            nextTime = currentTime + configuration.getPeriod();
        else
            nextTime = currentTime + random.nextInt((int) configuration.getPeriod());

        return true;
    }

    @Override
    public void onCompleted(ITaskContext context) {
    }

    @Override
    public Map<String, Object> getParameters() {
        return configuration.getParameters();
    }
}
