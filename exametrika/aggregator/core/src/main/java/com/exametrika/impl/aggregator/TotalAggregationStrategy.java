/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.common.utils.Pair;


/**
 * The {@link TotalAggregationStrategy} is an aggregation strategy for stack total measurements.
 *
 * @author AndreyM
 * @threadsafety Implementations of this class and its methods are thread safe.
 */
public class TotalAggregationStrategy {
    private final List<StackInfo> stacks = new ArrayList<StackInfo>(10);
    private int stackCount;

    public void beginStack() {
        stackCount++;
        if (stackCount > stacks.size())
            stacks.add(new StackInfo());
    }

    public void endStack() {
        stackCount--;
    }

    public void beginLevel() {
        StackInfo stack = stacks.get(stackCount - 1);

        stack.levelCount++;
        if (stack.levelCount > stack.levelMetrics.size())
            stack.levelMetrics.add(new ArrayList<Pair<IScopeName, IMetricName>>(10));
    }

    public void endLevel() {
        StackInfo stack = stacks.get(stackCount - 1);

        List<Pair<IScopeName, IMetricName>> metrics = stack.levelMetrics.get(stack.levelCount - 1);
        for (int i = 0; i < metrics.size(); i++) {
            Pair<IScopeName, IMetricName> pair = metrics.get(i);
            stack.metricsSet.put(pair, Boolean.FALSE);
        }
        metrics.clear();
        stack.levelCount--;
    }

    public boolean allowTotal(IScopeName scope, IMetricName metric) {
        StackInfo stack = stacks.get(stackCount - 1);

        Pair<IScopeName, IMetricName> pair = new Pair<IScopeName, IMetricName>(scope, metric);
        if (Boolean.TRUE.equals(stack.metricsSet.get(pair)))
            return false;

        List<Pair<IScopeName, IMetricName>> metrics = stack.levelMetrics.get(stack.levelCount - 1);
        metrics.add(pair);
        stack.metricsSet.put(pair, Boolean.TRUE);
        return true;
    }

    private static class StackInfo {
        private final Map<Pair<IScopeName, IMetricName>, Boolean> metricsSet = new HashMap<Pair<IScopeName, IMetricName>, Boolean>();
        private final List<List<Pair<IScopeName, IMetricName>>> levelMetrics = new ArrayList<List<Pair<IScopeName, IMetricName>>>(100);
        private int levelCount;
    }
}
