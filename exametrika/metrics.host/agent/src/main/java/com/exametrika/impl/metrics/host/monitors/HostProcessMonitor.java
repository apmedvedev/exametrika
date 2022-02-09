/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.SigarException;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.metrics.host.config.HostProcessMonitorConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.metrics.host.IProcessNamingStrategy;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link HostProcessMonitor} is a monitor of host processes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostProcessMonitor extends AbstractMonitor {
    private final List<IExpression> filters;
    private final ProcessContext processContext;
    private final IProcessNamingStrategy namingStrategy;
    private final Map<String, Object> runtimeContext;
    private final TLongObjectMap<IMeterContainer> processesMeters = new TLongObjectHashMap<IMeterContainer>();

    public HostProcessMonitor(HostProcessMonitorConfiguration configuration, IMonitorContext context) {
        super("host.processes", configuration, context, false);

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        List<IExpression> compiledFilters = new ArrayList<IExpression>();
        if (configuration.getFilters() != null) {
            for (String filter : configuration.getFilters())
                compiledFilters.add(Expressions.compile(filter, compileContext));
        }

        this.filters = compiledFilters;
        this.processContext = new ProcessContext();
        if (configuration.getNamingStrategy() != null)
            namingStrategy = configuration.getNamingStrategy().createStrategy();
        else
            namingStrategy = null;
    }

    @Override
    protected void createMeters() {
        meters.addGauge("host.processes.total", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getTotal();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        meters.addGauge("host.processes.idle", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getIdle();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        meters.addGauge("host.processes.running", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getRunning();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        meters.addGauge("host.processes.sleeping", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getSleeping();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        meters.addGauge("host.processes.stopped", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getStopped();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        meters.addGauge("host.processes.threads", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getThreads();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        meters.addGauge("host.processes.zombie", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = SigarHolder.instance.getProcStat().getZombie();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Override
    protected void updateMetersContainers() {
        try {
            long[] ids = SigarHolder.instance.getProcList();
            for (int i = 0; i < ids.length; i++) {
                final long id = ids[i];
                if (allow(id)) {
                    IMeterContainer meters = processesMeters.get(id);
                    if (meters == null) {
                        meters = createMeterContainer(id);
                        processesMeters.put(id, meters);
                    }

                    meters.setProcessed(true);
                } else {
                    IMeterContainer meters = processesMeters.remove(id);
                    if (meters != null)
                        meters.delete();
                }
            }

            for (TLongObjectIterator<IMeterContainer> it = processesMeters.iterator(); it.hasNext(); ) {
                it.advance();
                IMeterContainer meters = it.value();
                if (!meters.isProcessed()) {
                    it.remove();
                    meters.delete();
                } else
                    meters.setProcessed(false);
            }
        } catch (SigarException e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    private IMeterContainer createMeterContainer(long id) {
        processContext.setId(id);
        String subScope = Names.escape(getProcessName(id));
        processContext.setId(-1);

        ProcessMeterContainer meterContainer = new HostProcessMeterContainer(getMeasurementId(subScope, MetricName.root(), "host.process"),
                context, (IInstanceContextProvider) context, id);
        addMeters(meterContainer);

        return meterContainer;
    }

    private boolean allow(long id) {
        try {
            SigarHolder.instance.getProcExe(id);
        } catch (SigarException e) {
            return false;
        }

        if (!filters.isEmpty()) {
            processContext.setId(id);

            boolean allow = false;
            for (IExpression filter : filters) {
                if (filter.execute(processContext, runtimeContext)) {
                    allow = true;
                    break;
                }
            }

            processContext.setId(-1);

            if (!allow)
                return false;
        }

        return true;
    }

    private String getProcessName(long id) {
        String name = (namingStrategy != null ? namingStrategy.getName(processContext) : Long.toString(id));
        return name.replace('.', '_');
    }

    public class HostProcessMeterContainer extends ProcessMeterContainer {
        public HostProcessMeterContainer(NameMeasurementId id, IMonitorContext context,
                                         IInstanceContextProvider contextProvider, long processId) {
            super(id, context, contextProvider, processId);
        }

        @Override
        public void delete() {
            super.delete();

            processesMeters.remove(processId);
        }
    }

}
