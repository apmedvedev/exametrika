/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.config.model.SimpleMeasurementFilterSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IMeasurementFilter;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleMeasurementFilter} is an implementation of {@link IMeasurementFilter}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SimpleMeasurementFilter implements IMeasurementFilter {
    private final SimpleMeasurementFilterSchemaConfiguration configuration;
    private final IPeriodNameManager nameManager;

    public SimpleMeasurementFilter(SimpleMeasurementFilterSchemaConfiguration configuration, IDatabaseContext context) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
    }

    @Override
    public boolean allow(Measurement measurement) {
        MeasurementId id = (MeasurementId) measurement.getId();

        if (configuration.getScopeFilter() != null) {
            String scope = "";
            if (id.getScopeId() != 0) {
                IPeriodName name = nameManager.findById(id.getScopeId());
                if (name != null)
                    scope = name.getName().toString();
            }

            if (!configuration.getScopeFilter().match(scope))
                return false;
        }

        if (configuration.getMetricFilter() != null) {
            String metric = "";
            if (id.getLocationId() != 0) {
                IPeriodName name = nameManager.findById(id.getLocationId());
                if (name != null)
                    metric = name.getName().toString();
            }

            if (!configuration.getMetricFilter().match(metric))
                return false;
        }

        return true;
    }
}
