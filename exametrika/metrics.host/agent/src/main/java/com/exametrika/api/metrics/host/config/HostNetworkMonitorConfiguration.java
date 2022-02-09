/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.config;

import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.host.monitors.HostNetworkMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostNetworkMonitorConfiguration} is a configuration for host network monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostNetworkMonitorConfiguration extends MonitorConfiguration {
    private final String filter;
    private final boolean extendedStatistics;
    private final boolean tcpStatistics;
    private final Map<String, Long> networkInterfaceSpeed;

    public HostNetworkMonitorConfiguration(String name, String scope, long period, String measurementStrategy, String filter,
                                           boolean extendedStatistics, boolean tcpStatictics, Map<String, Long> networkInterfaceSpeed) {
        super(name, scope, period, measurementStrategy);

        this.filter = filter;
        this.extendedStatistics = extendedStatistics;
        this.tcpStatistics = tcpStatictics;
        this.networkInterfaceSpeed = Immutables.wrap(networkInterfaceSpeed);
    }

    public String getFilter() {
        return filter;
    }

    public boolean isExtendedStatistics() {
        return extendedStatistics;
    }

    public boolean isTcpStatistics() {
        return tcpStatistics;
    }

    public Map<String, Long> getNetworkInterfaceSpeed() {
        return networkInterfaceSpeed;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new HostNetworkMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        components.add(ValueSchemas.component("host.nets")
                .name("host.nets.dummy")
                .toConfiguration());
        components.add(ValueSchemas.component("host.net")
                .name("host.net.received")
                .name("host.net.sent")
                .name("host.net.rx.errors")
                .name("host.net.tx.errors")
                .name("host.net.rx.dropped")
                .name("host.net.tx.dropped")
                .name("host.net.rx.overruns")
                .name("host.net.tx.overruns")
                .name("host.net.rx.frame")
                .name("host.net.tx.collisions")
                .name("host.net.tx.carrier")
                .toConfiguration());
        components.add(ValueSchemas.component("host.tcp")
                .name("host.tcp.actives")
                .name("host.tcp.passives")
                .name("host.tcp.fails")
                .name("host.tcp.resets")
                .name("host.tcp.connections")
                .name("host.tcp.in")
                .name("host.tcp.out")
                .name("host.tcp.retransmits")
                .name("host.tcp.inErrors")
                .name("host.tcp.outErrors")
                .toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostNetworkMonitorConfiguration))
            return false;

        HostNetworkMonitorConfiguration configuration = (HostNetworkMonitorConfiguration) o;
        return super.equals(configuration) && Objects.equals(filter, configuration.filter) &&
                extendedStatistics == configuration.extendedStatistics &&
                tcpStatistics == configuration.tcpStatistics &&
                Objects.equals(networkInterfaceSpeed, configuration.networkInterfaceSpeed);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(filter, extendedStatistics, tcpStatistics, networkInterfaceSpeed);
    }
}
