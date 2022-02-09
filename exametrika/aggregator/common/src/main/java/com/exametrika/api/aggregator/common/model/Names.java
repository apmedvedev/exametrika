/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import java.util.List;

import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;


/**
 * The {@link Names} represents a utility class for creating names.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Names {
    public static String escape(String name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            builder.append(ch);
            if (ch == '.')
                builder.append('.');
        }

        return builder.toString();
    }

    public static String unescape(String name) {
        StringBuilder builder = new StringBuilder();
        boolean prevPeriod = false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '.') {
                if (!prevPeriod) {
                    builder.append('.');
                    prevPeriod = true;
                } else
                    prevPeriod = false;
            } else {
                prevPeriod = false;
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    public static IMetricName rootMetric() {
        return MetricName.root();
    }

    public static IMetricName getMetric(String name) {
        return MetricName.get(name);
    }

    public static IMetricName getMetric(List<String> segments) {
        return MetricName.get(segments);
    }

    public static IScopeName rootScope() {
        return ScopeName.root();
    }

    public static IScopeName getScope(String name) {
        return ScopeName.get(name);
    }

    public static IScopeName getScope(List<String> segments) {
        return ScopeName.get(segments);
    }

    public static ICallPath rootCallPath() {
        return CallPath.root();
    }

    public static ICallPath getCallPath(String callPath) {
        return CallPath.get(callPath);
    }

    public static ICallPath getCallPath(ICallPath parent, IMetricName segment) {
        return CallPath.get((CallPath) parent, (MetricName) segment);
    }

    public static ICallPath getCallPath(List<? extends IMetricName> segments) {
        return CallPath.get(segments);
    }

    private Names() {
    }
}
