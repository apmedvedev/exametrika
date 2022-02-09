/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.io.InputStream;
import java.util.List;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.instrument.config.JoinPointLoader;
import com.exametrika.spi.instrument.boot.IInvokeDispatcher;
import com.exametrika.spi.instrument.boot.IInvokeDispatcherFactory;


/**
 * The {@link StaticInvokeDispatcherFactory} represents an factory for {@link StaticInvokeDispatcher}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StaticInvokeDispatcherFactory implements IInvokeDispatcherFactory {
    private static final ILogger logger = Loggers.get(StaticInvokeDispatcherFactory.class);

    @Override
    public IInvokeDispatcher createDispatcher() {
        try {
            JoinPointLoader loader = new JoinPointLoader();
            InputStream stream;
            String instrumentData = System.getProperty("com.exametrika.instrument.data");
            if (instrumentData != null)
                stream = getClass().getClassLoader().getResourceAsStream(instrumentData);
            else
                stream = getClass().getClassLoader().getResourceAsStream("instrument.data");

            if (stream == null)
                return null;

            try {
                List<IJoinPoint> joinPoints = loader.load(stream);
                return new StaticInvokeDispatcher(joinPoints);
            } finally {
                IOs.close(stream);
            }
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);

            return null;
        }
    }
}
