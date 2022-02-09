/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.time.impl;

import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;

/**
 * The {@link TimeServiceFactory} is a factory for {@link SystemTimeService}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TimeServiceFactory extends AbstractSingletonComponentFactory<SystemTimeService> {
    public TimeServiceFactory() {
        super(true);
    }

    @Override
    protected SystemTimeService createInstance() {
        return new SystemTimeService();
    }
}
