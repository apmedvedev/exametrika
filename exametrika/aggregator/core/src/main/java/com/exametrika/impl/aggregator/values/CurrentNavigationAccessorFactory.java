/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.Collections;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.INavigationAccessorFactory;


/**
 * The {@link CurrentNavigationAccessorFactory} is an implementation of {@link INavigationAccessorFactory} for current node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CurrentNavigationAccessorFactory implements INavigationAccessorFactory {
    @Override
    public Set<String> getTypes() {
        return Collections.singleton("current");
    }

    @Override
    public IComponentAccessor createAccessor(String navigationType, String navigationArgs, IComponentAccessor localAccessor) {
        return new CurrentNavigationAccessor(localAccessor);
    }

    private static class CurrentNavigationAccessor implements IComponentAccessor {
        private final IComponentAccessor localAccessor;

        public CurrentNavigationAccessor(IComponentAccessor localAccessor) {
            Assert.notNull(localAccessor);

            this.localAccessor = localAccessor;
        }

        @Override
        public Object get(IComponentValue value, IComputeContext context) {
            return localAccessor.get(value, context);
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
