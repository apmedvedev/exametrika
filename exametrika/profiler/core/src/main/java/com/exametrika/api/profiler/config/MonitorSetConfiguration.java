/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.monitors.MonitorSet;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link MonitorSetConfiguration} is a configuration for monitor sets.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonitorSetConfiguration extends MonitorConfiguration {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<MonitorConfiguration> monitors;

    public MonitorSetConfiguration(String name, long period, String measurementStrategy, Set<? extends MonitorConfiguration> monitors) {
        super(name, "", period, measurementStrategy);

        Assert.notNull(monitors);
        Assert.isTrue(!monitors.isEmpty());

        for (MonitorConfiguration monitor : monitors) {
            if (monitor.getMeasurementStrategy() != null)
                throw new InvalidArgumentException(messages.strategyNotAllowed(monitor.getName()));
        }

        this.monitors = Immutables.wrap(monitors);
    }

    public Set<MonitorConfiguration> getMonitors() {
        return monitors;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        List<IMonitor> monitors = new ArrayList<IMonitor>(this.monitors.size());
        for (MonitorConfiguration monitor : this.monitors)
            monitors.add(monitor.createMonitor(context));

        return new MonitorSet(this, monitors);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        for (MonitorConfiguration monitor : monitors)
            monitor.buildComponentSchemas(components);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MonitorSetConfiguration))
            return false;

        MonitorSetConfiguration configuration = (MonitorSetConfiguration) o;
        return super.equals(configuration) && monitors.equals(configuration.monitors);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(monitors);
    }

    private interface IMessages {
        @DefaultMessage("Measurement strategy is not allowed in child monitor ''{0}''.")
        ILocalizedMessage strategyNotAllowed(String monitor);
    }
}
