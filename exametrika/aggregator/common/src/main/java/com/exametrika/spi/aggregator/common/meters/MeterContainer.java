/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.InfoConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;


/**
 * The {@link MeterContainer} is an abstract container of meters.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class MeterContainer implements IMeterContainer {
    protected final IMeasurementIdProvider idProvider;
    protected final IMeasurementContext context;
    private final IInstanceContextProvider contextProvider;
    private final Element<MeterContainer> element = new Element<MeterContainer>(this);
    private List<IMeter> meters;
    private boolean alwaysExtractMetadata;
    private boolean hasInstanceFields;
    private JsonObject metadata;
    private boolean metadataChanged;
    private boolean processed;
    private Measurement lastMeasurement;

    public MeterContainer(IMeasurementId id, IMeasurementContext context, IInstanceContextProvider contextProvider) {
        this(new MeasurementIdProvider(id), context, contextProvider);
    }

    public MeterContainer(IMeasurementIdProvider idProvider, IMeasurementContext context, IInstanceContextProvider contextProvider) {
        Assert.notNull(idProvider);
        Assert.notNull(context);
        Assert.notNull(contextProvider);

        this.idProvider = idProvider;
        this.context = context;
        this.contextProvider = contextProvider;
    }

    public Element<MeterContainer> getElement() {
        return element;
    }

    @Override
    public void setAlwaysExtractMetadata() {
        alwaysExtractMetadata = true;
    }

    @Override
    public IMeasurementId getId() {
        return idProvider.get();
    }

    @Override
    public IMeasurementIdProvider getIdProvider() {
        return idProvider;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    @Override
    public void setProcessed(boolean value) {
        this.processed = value;
    }

    @Override
    public int getMeterCount() {
        if (meters != null)
            return meters.size();
        else
            return 0;
    }

    @Override
    public <T extends IMeter> T getMeter(int index) {
        if (meters != null)
            return (T) meters.get(index);
        else
            return Assert.error();
    }

    @Override
    public <T extends IMeter> T findMeter(String metricType) {
        if (meters != null) {
            for (int i = 0; i < meters.size(); i++) {
                IMeter meter = meters.get(i);
                if (meter.getMetricType().equals(metricType))
                    return (T) meter;
            }

            return null;
        } else
            return Assert.error();
    }

    @Override
    public void buildMetricTypes(List<String> metricTypes) {
        if (meters != null) {
            for (IMeter meter : meters) {
                if (!(meter instanceof ILog))
                    metricTypes.add(meter.getMetricType());
            }
        }
    }

    public boolean hasInstanceFields() {
        return hasInstanceFields;
    }

    @Override
    public <T extends IMeter> T addMeter(String metricType, MeterConfiguration configuration, IMeasurementProvider provider) {
        return addMeter(idProvider, metricType, configuration, provider);
    }

    @Override
    public IGauge addGauge(String metricType, IMeasurementProvider provider) {
        return addMeter(metricType, new GaugeConfiguration(true), provider);
    }

    @Override
    public ICounter addCounter(String metricType, boolean useDeltas, int smoothingSize, IMeasurementProvider provider) {
        return addMeter(metricType, new CounterConfiguration(true, useDeltas, smoothingSize), provider);
    }

    @Override
    public IInfo addInfo(String metricType, IMeasurementProvider provider) {
        return addMeter(metricType, new InfoConfiguration(true), provider);
    }

    @Override
    public ILog addLog(String metricType, LogConfiguration configuration) {
        return addMeter(idProvider, metricType, configuration, null);
    }

    @Override
    public ILog addLog(IMeasurementIdProvider idProvider, LogConfiguration configuration) {
        return addMeter(idProvider, null, configuration, null);
    }

    @Override
    public JsonObject getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(JsonObject metadata) {
        this.metadata = metadata;
        metadataChanged = true;

        if (meters != null) {
            for (IMeter meter : meters) {
                if (meter instanceof ILog)
                    ((ILog) meter).setMetadata(metadata);
            }
        }
    }

    public void measure() {
        if (meters == null)
            return;

        for (IMeter meter : meters) {
            if (meter.hasProvider())
                meter.measure();
        }
    }

    public Measurement extract(long period, double approximationMultiplier, boolean force, boolean clear) {
        boolean hasValue = false;
        List<IMetricValue> metrics;
        if (meters != null) {
            metrics = new ArrayList<IMetricValue>(meters.size());
            for (IMeter meter : meters) {
                IMetricValue value = meter.extract(approximationMultiplier, force, clear);
                if (!(meter instanceof ILog)) {
                    metrics.add(value);

                    if (value != null)
                        hasValue = true;
                }
            }
        } else
            metrics = Collections.emptyList();

        boolean metadataChanged = this.metadataChanged;

        JsonObject metadata = null;
        if (metadataChanged || force || alwaysExtractMetadata)
            metadata = this.metadata;

        if (clear)
            this.metadataChanged = false;

        if (hasValue || metadataChanged || force) {
            lastMeasurement = new Measurement(idProvider.get(), new ComponentValue(metrics, metadata), period, null);
            return lastMeasurement;
        } else
            return null;
    }

    public void extract(long period, double approximationMultiplier, boolean force, boolean clear, List<IMetricValue> metrics) {
        if (meters == null)
            return;

        for (IMeter meter : meters) {
            IMetricValue value = meter.extract(approximationMultiplier, force, clear);
            if (!(meter instanceof ILog))
                metrics.add(value);
        }
    }

    public void extractLogs() {
        if (meters == null)
            return;

        for (IMeter meter : meters) {
            if (!(meter instanceof ILog))
                continue;

            meter.extract(0, false, true);
        }
    }

    public void dump(Json json, boolean extract, double approximationMultiplier) {
        List<String> metricTypes;
        if (meters != null) {
            metricTypes = new ArrayList<String>();
            buildMetricTypes(metricTypes);
        } else
            metricTypes = Collections.emptyList();

        if (extract)
            extract(0, approximationMultiplier, true, false);

        if (lastMeasurement != null)
            json.add(Measurements.toJson(lastMeasurement, metricTypes, metadata));
    }

    @Override
    public void delete() {
        element.remove();
    }

    private <T extends IMeter> T addMeter(IMeasurementIdProvider idProvider, String metricType, MeterConfiguration configuration, IMeasurementProvider provider) {
        IMeter meter = Meters.createMeter(this, metricType, configuration, idProvider, provider, contextProvider, context);

        if (meters == null)
            meters = new ArrayList<IMeter>();

        if (meter instanceof IFieldMeter && ((IFieldMeter) meter).hasInstanceFields())
            hasInstanceFields = true;
        else if (meter instanceof ILog)
            ((ILog) meter).setMetadata(metadata);

        meters.add(meter);

        return (T) meter;
    }
}
