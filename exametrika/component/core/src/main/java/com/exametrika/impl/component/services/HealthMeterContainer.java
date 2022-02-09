/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.component.config.model.HealthComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.common.json.Json;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;

/**
 * The {@link HealthMeterContainer} is a health meter container.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class HealthMeterContainer extends MeterContainer {
    private final HealthSchemaConfiguration configuration;
    private final ComponentMeasurementContext context;
    private ICounter totalTime;
    private ICounter upTime;
    private ICounter downTime;
    private ICounter failureTime;
    private ICounter maintenanceTime;

    public HealthMeterContainer(HealthSchemaConfiguration configuration, ComponentMeasurementContext context) {
        super(new HealthIdProvider(), context, context);

        Assert.notNull(configuration);

        this.configuration = configuration;
        this.context = context;

        createMeters();
    }

    public Measurement measureStateCompleted(IHealthComponent component, State oldState, State newState, long startTime,
                                             long endTime, boolean log) {
        setId(component, newState.toString().toLowerCase());
        long period = endTime - startTime;
        if (period < 0)
            return null;

        long delta = period * 1000000;

        switch (oldState) {
            case CREATED:
                break;
            case NORMAL:
            case HEALTH_WARNING:
                if (upTime != null)
                    upTime.measureDelta(delta);
                totalTime.measureDelta(delta);
                break;
            case HEALTH_ERROR:
            case UNAVAILABLE:
                if (downTime != null)
                    downTime.measureDelta(delta);
                if (failureTime != null)
                    failureTime.measureDelta(delta);
                totalTime.measureDelta(delta);
                break;
            case MAINTENANCE:
                if (downTime != null)
                    downTime.measureDelta(delta);
                if (maintenanceTime != null)
                    maintenanceTime.measureDelta(delta);
                totalTime.measureDelta(delta);
                break;
            default:
                Assert.error();
        }

        return extract(period, 0, false, true);
    }

    private void createMeters() {
        totalTime = addMeter("component.totalTime", configuration.getTotalCounter(), null);
        if (configuration.getUpCounter().isEnabled())
            upTime = addMeter("component.upTime", configuration.getUpCounter(), null);
        if (configuration.getDownCounter().isEnabled())
            downTime = addMeter("component.downTime", configuration.getDownCounter(), null);
        if (configuration.getFailureCounter().isEnabled())
            failureTime = addMeter("component.failureTime", configuration.getFailureCounter(), null);
        if (configuration.getMaintenanceCounter().isEnabled())
            maintenanceTime = addMeter("component.maintenanceTime", configuration.getMaintenanceCounter(), null);
    }

    private void setId(IHealthComponent component, String state) {
        ComponentNode node = (ComponentNode) component;
        context.setContext(Json.object()
                .put("type", node.getSchema().getConfiguration().getComponent().getName())
                .put("state", state)
                .toObject());
        String componentType = ((HealthComponentSchemaConfiguration) node.getSchema().getConfiguration().getComponent()).getHealthComponentType();
        ((HealthIdProvider) idProvider).set(component.getScopeId(), componentType);
    }

    private static class HealthIdProvider implements IMeasurementIdProvider {
        private long scopeId;
        private String componentType;

        @Override
        public IMeasurementId get() {
            return new MeasurementId(scopeId, 0, componentType);
        }

        public void set(long scopeId, String componentType) {
            this.scopeId = scopeId;
            this.componentType = componentType;
        }
    }
}