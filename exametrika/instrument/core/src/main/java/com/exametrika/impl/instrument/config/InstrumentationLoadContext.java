/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.common.CommonConfiguration;
import com.exametrika.common.config.common.ICommonLoadContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.config.IInstrumentationLoadContext;


/**
 * The {@link InstrumentationLoadContext} is a helper class that is used to load {@link InstrumentationConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class InstrumentationLoadContext implements IInstrumentationLoadContext, IContextFactory, IConfigurationFactory {
    private final Set<Pointcut> pointcuts = new LinkedHashSet<Pointcut>();
    private boolean debug = false;
    private File debugPath = new File(System.getProperty("com.exametrika.workPath"), "/instrument/debug");
    private int maxJoinPointCount = Integer.MAX_VALUE;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setWorkPath(File workPath) {
        Assert.notNull(workPath);

        this.debugPath = new File(workPath, "/instrument/debug");
    }

    @Override
    public void addPointcut(Pointcut pointcut) {
        Assert.notNull(pointcut);

        pointcuts.add(pointcut);
    }

    @Override
    public void setMaxJoinPointCount(int value) {
        this.maxJoinPointCount = Math.min(maxJoinPointCount, value);
    }

    @Override
    public InstrumentationConfiguration createConfiguration(ILoadContext context) {
        ICommonLoadContext commonContext = context.get(CommonConfiguration.SCHEMA);
        return new InstrumentationConfiguration(commonContext.getRuntimeMode(), pointcuts, debug, debugPath,
                maxJoinPointCount);
    }

    @Override
    public IConfigurationFactory createContext() {
        return new InstrumentationLoadContext();
    }
}
