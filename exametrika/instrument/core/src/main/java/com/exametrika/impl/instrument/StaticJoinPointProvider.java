/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link StaticJoinPointProvider} represents an implementation of {@link IJoinPointProvider} for buildtime instrumentation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StaticJoinPointProvider implements IJoinPointProvider {
    protected final List<IJoinPoint> joinPoints;
    protected final Map<Pair<String, String>, List<JoinPointEntry>> joinPointsMap;

    public StaticJoinPointProvider(List<IJoinPoint> joinPoints) {
        Assert.notNull(joinPoints);

        this.joinPoints = Immutables.wrap(joinPoints);
        Map<Pair<String, String>, List<JoinPointEntry>> joinPointsMap = new HashMap<Pair<String, String>, List<JoinPointEntry>>();

        int i = 0;
        for (IJoinPoint joinPoint : joinPoints) {
            Pair<String, String> pair = new Pair(joinPoint.getClassName(), joinPoint.getMethodName());
            List<JoinPointEntry> list = joinPointsMap.get(pair);
            if (list == null) {
                list = new ArrayList<JoinPointEntry>();
                joinPointsMap.put(pair, list);
            }

            list.add(new JoinPointEntry(i, 0, joinPoint));
            i++;
        }

        this.joinPointsMap = joinPointsMap;
    }

    public List<IJoinPoint> getJoinPoints() {
        return joinPoints;
    }

    @Override
    public int getJoinPointCount() {
        return joinPoints.size();
    }

    @Override
    public IJoinPoint findJoinPoint(int index, int version) {
        if (index < joinPoints.size())
            return joinPoints.get(index);
        else
            return null;
    }

    @Override
    public List<JoinPointEntry> findJoinPoints(String className, String methodName, Class interceptorClass) {
        List<JoinPointEntry> joinPoints = joinPointsMap.get(new Pair(className, methodName));
        if (joinPoints == null)
            return Collections.emptyList();

        List<JoinPointEntry> list = new ArrayList<JoinPointEntry>();
        for (JoinPointEntry entry : joinPoints) {
            if (((StaticInterceptorConfiguration) entry.joinPoint.getPointcut().getInterceptor()).getInterceptorClass() == interceptorClass)
                list.add(entry);
        }
        return Immutables.wrap(list);
    }
}
