/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link ExitPointProbeCollector} is an exit point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExitPointProbeCollector extends StackProbeCollector {
    private final ExitPointProbeConfiguration configuration;
    private final String name;
    private final UUID stackId;
    private IRequest request;
    private Map<String, ExitPointProbeCollector> collectors;
    private boolean calibrating;
    private final ExitPointProbeCalibrateInfo calibrateInfo;
    private final boolean leaf;

    public ExitPointProbeCollector(ExitPointProbeConfiguration configuration, int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                                   StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(index, callPath, root, parent, metadata, new NameMeasurementId(parent.getId().getScope(),
                getMeasurementCallPath((ICallPath) parent.getId().getLocation(), callPath.getLastSegment(), leaf),
                configuration.getComponentType()));

        Assert.notNull(configuration);
        Assert.notNull(name);
        Assert.notNull(calibrateInfo);

        this.configuration = configuration;
        this.name = name;
        this.stackId = stackId;
        this.calibrateInfo = calibrateInfo;
        this.leaf = leaf;
        if (configuration.isIntermediate())
            setParentNonSamplingRoot();
    }

    public final String getName() {
        return name;
    }

    public final UUID getStackId() {
        return stackId;
    }

    public boolean isLeaf() {
        return leaf;
    }

    @Override
    protected boolean canExtract() {
        return leaf;
    }

    @Override
    protected boolean isPermanentHotspotCollector() {
        return configuration.isPermanentHotspot();
    }

    @Override
    protected void setCalibrate(boolean hotspot, boolean fast) {
        calibrating = true;
        super.setCalibrate(hotspot, fast);
    }

    @Override
    protected ExitPointProbeCollector findChild(int index, Object param) {
        if (collectors != null)
            return collectors.get(((IRequest) param).getName());
        else
            return null;
    }

    @Override
    protected StackProbeCollector createCollector(int index, int version, IStackProbeCollectorFactory collectorFactory,
                                                  Object param) {
        if (leaf || calibrating)
            return null;

        ExitPointProbeCollector collector = (ExitPointProbeCollector) super.createCollector(index, version, collectorFactory, param);

        if (collectors == null)
            collectors = new LinkedHashMap<String, ExitPointProbeCollector>();

        collectors.put(collector.getName(), collector);

        return collector;
    }

    @Override
    protected void removeCollector(StackProbeCollector collector) {
        if (!leaf && !calibrating) {
            ExitPointProbeCollector exitPointCollector = (ExitPointProbeCollector) collector;
            if (collectors != null)
                collectors.remove(exitPointCollector.getName());
        }

        super.removeCollector(collector);
    }

    @Override
    protected final void createMeters() {
        super.createMeters();

        doCreateMeters();
    }

    @Override
    protected final void clearMeters() {
        super.clearMeters();

        collectors = null;

        doClearMeters();
    }

    @Override
    protected final void beginMeasureMeters(Object param) {
        request = (IRequest) param;

        IProbeContext context = getRoot().getContext();

        boolean hasInstanceFields = getMeters().hasInstanceFields();
        if (hasInstanceFields)
            context.setInstanceContext(request.getParameters());

        doBeginMeasure(request);

        super.beginMeasureMeters(param);

        if (hasInstanceFields)
            context.setInstanceContext(null);
    }

    @Override
    protected final void endMeasureMeters(boolean disableInherent) {
        IProbeContext context = getRoot().getContext();

        boolean hasInstanceFields = getMeters().hasInstanceFields();
        if (hasInstanceFields)
            context.setInstanceContext(request.getParameters());

        super.endMeasureMeters(disableInherent);

        doEndMeasure(request);

        if (hasInstanceFields)
            context.setInstanceContext(null);

        request = null;
    }

    @Override
    protected final String getComponentType() {
        return configuration.getComponentType();
    }

    @Override
    protected boolean hasOutliers() {
        return false;
    }

    @Override
    protected boolean allowConcurrency() {
        return false;
    }

    protected void doCreateMeters() {
    }

    protected void doClearMeters() {
    }

    protected void doBeginMeasure(IRequest request) {
    }

    protected void doEndMeasure(IRequest request) {
    }

    @Override
    protected final long getNormalCollectorFullOuterOverhead(boolean classify) {
        if (!leaf || classify)
            return calibrateInfo.normalCollectorFullOuterOverhead;
        else
            return 0;
    }

    @Override
    protected final long getOuterOverhead() {
        if (isHotspot()) {
            if (isFast()) {
                if (!leaf)
                    return calibrateInfo.fastCollectorFullOuterOverhead;
                else
                    return 0;
            } else {
                if (!leaf)
                    return calibrateInfo.normalCollectorFullOuterOverhead;
                else
                    return 0;
            }
        } else {
            StackProbeCalibrateInfo info = getRoot().getCalibrateInfo();
            if (isFast())
                return info.fastCollectorEstimatingOuterOverhead;
            else
                return info.normalCollectorEstimatingOuterOverhead;
        }
    }

    private static ICallPath getMeasurementCallPath(ICallPath parentCallPath, IMetricName metricName, boolean leaf) {
        if (leaf) {
            ICallPath callPath = Names.getCallPath(parentCallPath.getSegments().subList(0, parentCallPath.getSegments().size() - 1));
            return callPath.getChild(metricName);
        } else
            return parentCallPath.getChild(metricName);
    }
}
