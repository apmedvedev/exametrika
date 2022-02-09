/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Collections;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.INavigationAccessorFactory;


/**
 * The {@link PeriodTimeNavigationAccessorFactory} is an implementation of {@link INavigationAccessorFactory} for duration of current period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodTimeNavigationAccessorFactory implements INavigationAccessorFactory {
    @Override
    public Set<String> getTypes() {
        return Collections.asSet("period(ns)", "period(ms)");
    }

    @Override
    public IComponentAccessor createAccessor(String navigationType, String navigationArgs, IComponentAccessor localAccessor) {
        return new PeriodTimeNavigationAccessor(navigationType.equals("period(ns)"));
    }

    private static class PeriodTimeNavigationAccessor implements IComponentAccessor {
        private final long resolution;

        public PeriodTimeNavigationAccessor(boolean nanosecons) {
            if (nanosecons)
                resolution = 1000000;
            else
                resolution = 1;
        }

        @Override
        public Object get(IComponentValue value, IComputeContext context) {
            return (context.getPeriod() + 1) * resolution;
        }

        @Override
        public Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
            return get(componentValue, context);
        }

        @Override
        public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                          IComputeContext context) {
            return get(componentValue, context);
        }
    }
}
