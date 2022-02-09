/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.util.List;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.IInvokeDispatcher;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link StaticInvokeDispatcher} represents an implementation of {@link IInvokeDispatcher} for buildtime instrumentation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StaticInvokeDispatcher extends StaticJoinPointProvider implements IInvokeDispatcher {
    private static final ILogger logger = Loggers.get(StaticInvokeDispatcher.class);
    private final IDynamicInterceptor[] interceptors;
    ;

    public StaticInvokeDispatcher(List<IJoinPoint> joinPoints) {
        super(joinPoints);

        interceptors = new IDynamicInterceptor[joinPoints.size()];

        for (int i = 0; i < joinPoints.size(); i++) {
            IJoinPoint joinPoint = joinPoints.get(i);

            try {
                if (joinPoint.getPointcut().getInterceptor() instanceof DynamicInterceptorConfiguration) {
                    DynamicInterceptorConfiguration configuration = (DynamicInterceptorConfiguration) joinPoint.getPointcut().getInterceptor();
                    interceptors[i] = configuration.createInterceptor();

                    interceptors[i].start(joinPoint);
                }
            } catch (Exception e) {
                throw new InstrumentationException(e);
            }
        }
    }

    @Override
    public boolean invoke(int interceptorIndex, int version, IInvocation invocation) {
        if (interceptorIndex < interceptors.length) {
            try {
                return interceptors[interceptorIndex].intercept(invocation);
            } catch (Throwable e) {
                logger.log(LogLevel.ERROR, e);
            }
        }

        return false;
    }
}
