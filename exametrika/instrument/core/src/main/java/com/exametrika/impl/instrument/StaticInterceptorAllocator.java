/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider.JoinPointEntry;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.spi.instrument.IInterceptorAllocator;


/**
 * The {@link StaticInterceptorAllocator} represents an implementation of {@link IInterceptorAllocator} for buildtime instrumentation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StaticInterceptorAllocator implements IInterceptorAllocator {
    private List<IJoinPoint> joinPoints;
    private final Map<Pointcut, JoinPointEntry> singletonJoinPoints = new HashMap<Pointcut, JoinPointEntry>();

    public StaticInterceptorAllocator() {
        joinPoints = new ArrayList<IJoinPoint>();
    }

    public void setJoinPoints(List<IJoinPoint> joinPoints) {
        Assert.notNull(joinPoints);

        this.joinPoints = joinPoints;
    }

    public List<IJoinPoint> getJoinPoints() {
        return joinPoints;
    }

    @Override
    public JoinPointInfo allocate(ClassLoader classLoader, IJoinPoint joinPoint) {
        Assert.notNull(joinPoint);

        if (joinPoint.getPointcut().isSingleton()) {
            JoinPointEntry entry = singletonJoinPoints.get(joinPoint.getPointcut());
            if (entry != null)
                return new JoinPointInfo(entry.index, entry.version);

            joinPoint = new JoinPoint(joinPoint.getKind(), 0, 0, joinPoint.getPointcut().getName(), "", "", 0, joinPoint.getPointcut(),
                    null, null, null, null, null, 0);
        }

        joinPoints.add(joinPoint);
        JoinPointInfo info = new JoinPointInfo(joinPoints.size() - 1, 0);

        if (joinPoint.getPointcut().isSingleton())
            singletonJoinPoints.put(joinPoint.getPointcut(), new JoinPointEntry(info.index, info.version, joinPoint));

        return info;
    }
}
