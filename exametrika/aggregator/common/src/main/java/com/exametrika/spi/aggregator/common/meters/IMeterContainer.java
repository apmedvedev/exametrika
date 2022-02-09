/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import java.util.List;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;


/**
 * The {@link IMeterContainer} represents a container of meters.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IMeterContainer {
    /**
     * Returns measurement identifier of meter container.
     *
     * @return measurement identifier of meter container
     */
    IMeasurementId getId();

    /**
     * Returns measurement identifier provider.
     *
     * @return measurement identifier provider
     */
    IMeasurementIdProvider getIdProvider();

    /**
     * Is container processed by updating procedure?
     *
     * @return true if container is processed by updating procedure
     */
    boolean isProcessed();

    /**
     * Marks container as processed by updating procedure.
     *
     * @param value true if container is processed
     */
    void setProcessed(boolean value);

    /**
     * Forces unconditional extraction of metadata.
     */
    void setAlwaysExtractMetadata();

    /**
     * Returns meter count.
     *
     * @return meter count
     */
    int getMeterCount();

    /**
     * Returns meter.
     *
     * @param index index
     * @return meter
     */
    <T extends IMeter> T getMeter(int index);

    /**
     * Finds meter.
     *
     * @param metricType metric type
     * @return meter or null if meter is not found
     */
    <T extends IMeter> T findMeter(String metricType);

    /**
     * Builds list of metric types for corresponding measurements measured by meter container.
     *
     * @param metricTypes metric types list
     */
    void buildMetricTypes(List<String> metricTypes);

    /**
     * Adds meter to container.
     *
     * @param metricType    metric type
     * @param configuration meter configuration
     * @param provider      measurement provider or null if measurement provider is not used
     * @return meter
     */
    <T extends IMeter> T addMeter(String metricType, MeterConfiguration configuration, IMeasurementProvider provider);

    /**
     * Adds gauge.
     *
     * @param metricType metric type
     * @param provider   measurement provider or null if measurement provider is not used
     * @return gauge
     */
    IGauge addGauge(String metricType, IMeasurementProvider provider);

    /**
     * Adds counter.
     *
     * @param metricType    metric type
     * @param useDeltas     if true deltas are used
     * @param smoothingSize smoothing size, if 0 value is not smoothed
     * @param provider      measurement provider or null if measurement provider is not used
     * @return counter
     */
    ICounter addCounter(String metricType, boolean useDeltas, int smoothingSize, IMeasurementProvider provider);

    /**
     * Adds info.
     *
     * @param metricType metric type
     * @param provider   measurement provider or null if measurement provider is not used
     * @return info
     */
    IInfo addInfo(String metricType, IMeasurementProvider provider);

    /**
     * Adds log.
     *
     * @param metricType    metric type
     * @param configuration log configuration.
     * @return log
     */
    ILog addLog(String metricType, LogConfiguration configuration);

    /**
     * Adds log.
     *
     * @param idProvider    measurement id provider
     * @param configuration log configuration.
     * @return log
     */
    ILog addLog(IMeasurementIdProvider idProvider, LogConfiguration configuration);

    /**
     * Returns metadata.
     *
     * @return metadata or null if metadata are not set
     */
    JsonObject getMetadata();

    /**
     * Sets metadata.
     *
     * @param metadata metadata or null if metadata are not used
     */
    void setMetadata(JsonObject metadata);

    /**
     * Deletes meter container.
     */
    void delete();
}
