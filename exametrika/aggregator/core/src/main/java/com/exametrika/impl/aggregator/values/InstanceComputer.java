/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IInstanceRecord;
import com.exametrika.api.aggregator.common.values.IInstanceValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link InstanceComputer} is an implementation of {@link IFieldComputer} for instance fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceComputer implements IFieldComputer {
    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IInstanceValue value = (IInstanceValue) v;

        JsonArrayBuilder fields = new JsonArrayBuilder();
        for (IInstanceRecord record : value.getRecords()) {
            JsonObjectBuilder recordBuilder = new JsonObjectBuilder();

            if (record.getId() instanceof NameMeasurementId)
                recordBuilder.put("id", Measurements.toJson(record.getId()));
            else {
                JsonObjectBuilder idBuilder = new JsonObjectBuilder();

                MeasurementId id = (MeasurementId) record.getId();
                IPeriodNameManager nameManager = context.getNameManager();

                IPeriodName scope = nameManager.findById(id.getScopeId());
                if (scope != null)
                    idBuilder.put("scope", scope.getName().toString());
                else
                    idBuilder.put("scope-id", id.getScopeId());

                IPeriodName location = nameManager.findById(id.getLocationId());
                if (location != null)
                    idBuilder.put("location", location.getName().toString());
                else
                    idBuilder.put("location-id", id.getLocationId());

                idBuilder.put("type", id.getComponentType());

                recordBuilder.put("id", idBuilder);
            }

            if (!record.getContext().isEmpty())
                recordBuilder.put("context", record.getContext());

            recordBuilder.put("time", record.getTime());
            recordBuilder.put("value", record.getValue());

            fields.add(recordBuilder);
        }

        return fields.toJson();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }
}
