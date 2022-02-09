/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.SigarException;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link HostCpuMonitor} is a monitor of host CPUs.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostCpuMonitor extends AbstractMonitor {
    public HostCpuMonitor(MonitorConfiguration configuration, IMonitorContext context) {
        super(null, configuration, context, false);
    }

    @Override
    protected void createMeters() {
        try {
            CpuInfo[] cpuInfos = SigarHolder.instance.getCpuInfoList();
            for (int i = 0; i < cpuInfos.length; i++) {
                String subScope = Integer.toString(i);
                IMeterContainer meters = createMeterContainer(subScope, MetricName.root(), "host.cpu");
                initMetadata(meters, cpuInfos[i]);

                final int index = i;
                meters.addCounter("host.cpu.total", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getTotal();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.idle", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getIdle();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.irq", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getIrq();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.softIrq", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getSoftIrq();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.nice", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getNice();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.stolen", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getStolen();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.sys", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getSys();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.user", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getUser();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.cpu.iowait", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        long value = SigarHolder.instance.getCpuList()[index].getWait();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });
            }
        } catch (Exception e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    private void initMetadata(IMeterContainer meters, CpuInfo info) throws SigarException {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .putIf("vendor", info.getVendor(), !"-1".equals(info.getVendor()))
                .putIf("model", info.getModel(), !"-1".equals(info.getModel()))
                .putIf("frequency", info.getMhz(), info.getMhz() != -1)
                .putIf("cacheSize", info.getCacheSize(), info.getCacheSize() != -1)
                .putIf("totalCores", info.getTotalCores(), info.getTotalCores() != -1)
                .putIf("totalSockets", info.getTotalSockets(), info.getTotalSockets() != -1)
                .toObject();
        meters.setMetadata(metadata);
    }
}
