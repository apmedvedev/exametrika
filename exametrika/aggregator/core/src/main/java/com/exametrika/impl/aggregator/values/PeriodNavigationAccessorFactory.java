/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.Collections;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.config.model.PeriodRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.ILogAggregationField.IAggregationIterator;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.INavigationAccessorFactory;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link PeriodNavigationAccessorFactory} is an implementation of {@link INavigationAccessorFactory} for periods.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNavigationAccessorFactory implements INavigationAccessorFactory {
    @Override
    public Set<String> getTypes() {
        return Collections.singleton(PeriodRepresentationSchemaConfiguration.PERIOD_ACCESSOR);
    }

    @Override
    public IComponentAccessor createAccessor(String navigationType, String navigationArgs, IComponentAccessor localAccessor) {
        return new PeriodNavigationAccessor(localAccessor);
    }

    private static class PeriodNavigationAccessor implements IComponentAccessor {
        private final IComponentAccessor localAccessor;

        public PeriodNavigationAccessor(IComponentAccessor localAccessor) {
            Assert.notNull(localAccessor);

            this.localAccessor = localAccessor;
        }

        @Override
        public Object get(IComponentValue value, IComputeContext context) {
            if (context.getObject() instanceof IAggregationIterator) {
                IAggregationIterator it = (IAggregationIterator) context.getObject();
                IAggregationRecord record = it.getPrevious();
                if (record != null) {
                    IComponentValue componentValue = record.getValue();
                    return localAccessor.get(componentValue, context);
                }
            } else if (context.getObject() instanceof IField) {
                IField field = (IField) context.getObject();
                INodeObject prevPeriodNode = ((IPeriodNode) field.getNode()).getPreviousPeriodNode();
                if (prevPeriodNode != null) {
                    IField prevField = prevPeriodNode.getNode().getField(field.getSchema().getIndex());
                    if (prevField instanceof IPeriodAggregationField) {
                        IPeriodAggregationField aggregationField = (IPeriodAggregationField) prevField;
                        IComponentValue componentValue = aggregationField.get();
                        return localAccessor.get(componentValue, context);
                    } else {
                        ILogAggregationField aggregationLogField = (ILogAggregationField) prevField;
                        IComponentValue componentValue = aggregationLogField.getCurrent().getValue();
                        return localAccessor.get(componentValue, context);
                    }
                }
            }

            return null;
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
