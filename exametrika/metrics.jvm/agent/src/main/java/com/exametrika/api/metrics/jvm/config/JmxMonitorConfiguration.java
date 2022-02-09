/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.monitors.JmxMonitor;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link JmxMonitorConfiguration} is a configuration for JMX monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmxMonitorConfiguration extends MonitorConfiguration {
    private final String componentType;
    private final String object;
    private final List<JmxAttributeConfiguration> attributes;

    public JmxMonitorConfiguration(String name, String scope, long period, String measurementStrategy, String componentType,
                                   String object, List<JmxAttributeConfiguration> attributes) {
        super(name, scope, period, measurementStrategy);

        Assert.notNull(componentType);
        Assert.notNull(object);
        Assert.notNull(attributes);

        this.componentType = componentType;
        this.object = getCanonicalName(object);
        this.attributes = Immutables.wrap(attributes);
    }

    public String getComponentType() {
        return componentType;
    }

    public String getObject() {
        return object;
    }

    public List<JmxAttributeConfiguration> getAttributes() {
        return attributes;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new JmxMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        List<MetricValueSchemaConfiguration> metrics = new ArrayList<MetricValueSchemaConfiguration>();
        for (JmxAttributeConfiguration attribute : attributes) {
            if (attribute.getMeter() instanceof LogConfiguration) {
                LogConfiguration log = (LogConfiguration) attribute.getMeter();
                log.buildComponentSchemas(attribute.getMetricType(), components);

                metrics.addAll(log.getMetricSchemas());
            } else
                metrics.add(attribute.getMeter().getSchema(attribute.getMetricType()));
        }
        components.add(new ComponentValueSchemaConfiguration(componentType, metrics));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JmxMonitorConfiguration))
            return false;

        JmxMonitorConfiguration configuration = (JmxMonitorConfiguration) o;
        return super.equals(configuration) && componentType.equals(configuration.componentType) &&
                object.equals(configuration.object) && attributes.equals(configuration.attributes);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentType, object, attributes);
    }

    private String getCanonicalName(String name) {
        try {
            ObjectName objectName = ObjectName.getInstance(name);
            return objectName.getCanonicalName();
        } catch (MalformedObjectNameException e) {
            throw new InvalidConfigurationException(e);
        }
    }
}
