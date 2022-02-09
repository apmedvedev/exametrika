/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link ComputeContext} is an implementation of {@link IComputeContext}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ComputeContext implements IComputeContext {
    private String nodeType;
    private Object object;
    private boolean inherent;
    private boolean total;
    private long time;
    private long period;
    private IPeriodNameManager nameManager;
    private List<Measurement> measurements;

    @Override
    public String getNodeType() {
        return nodeType;
    }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public boolean isInherent() {
        return inherent;
    }

    @Override
    public boolean isTotal() {
        return total;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public long getPeriod() {
        return period;
    }

    @Override
    public IPeriodNameManager getNameManager() {
        return nameManager;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void setInherent(boolean inherent) {
        this.inherent = inherent;
    }

    public void setTotal(boolean total) {
        this.total = total;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public void setPeriod(long period) {
        this.period = period;
    }

    public void setNameManager(IPeriodNameManager nameManager) {
        this.nameManager = nameManager;
    }

    public List<Measurement> takeMeasurements() {
        List<Measurement> measurements = this.measurements;
        this.measurements = null;
        return measurements;
    }

    @Override
    public void addMeasurement(Measurement measurement) {
        if (measurements == null)
            measurements = new ArrayList<Measurement>();

        measurements.add(measurement);
    }
}
