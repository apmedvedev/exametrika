/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.SigarException;

import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.metrics.host.config.HostFileSystemMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link HostFileSystemMonitor} is a monitor of host file systems.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostFileSystemMonitor extends AbstractMonitor {
    private ICondition<String> filter;

    public HostFileSystemMonitor(HostFileSystemMonitorConfiguration configuration, IMonitorContext context) {
        super(null, configuration, context, false);

        if (configuration.getFilter() != null)
            filter = Strings.createFilterCondition(configuration.getFilter(), true);
        else
            filter = null;
    }

    @Override
    protected void createMeters() {
        try {
            FileSystem[] fileSystems = SigarHolder.instance.getFileSystemList();
            for (int i = 0; i < fileSystems.length; i++) {
                FileSystem fileSystem = fileSystems[i];
                if (fileSystem.getType() != FileSystem.TYPE_LOCAL_DISK)
                    continue;

                final String name = fileSystem.getDirName();

                if (filter != null && !filter.evaluate(name))
                    continue;

                String subScope = Names.escape(name);
                IMeterContainer meters = createMeterContainer(subScope, MetricName.root(), "host.fs");
                initMetadata(meters, fileSystem);

                meters.addCounter("host.disk.read", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        if (usage.getDiskReads() != -1 && usage.getDiskReadBytes() != -1)
                            return new Pair(usage.getDiskReads(), usage.getDiskReadBytes());
                        else
                            return null;
                    }
                });

                meters.addCounter("host.disk.write", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        if (usage.getDiskWrites() != -1 && usage.getDiskWriteBytes() != -1)
                            return new Pair(usage.getDiskWrites(), usage.getDiskWriteBytes());
                        else
                            return null;
                    }
                });

                meters.addGauge("host.disk.serviceTime", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        double value = usage.getDiskServiceTime();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.disk.queue", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        double value = usage.getDiskQueue();
                        if (value >= 0)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.disk.total", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        long value = usage.getTotal() * 1024;
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.disk.used", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        long value = usage.getUsed() * 1024;
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.disk.free", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        long value = usage.getFree() * 1024;
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.disk.available", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        long value = usage.getAvail() * 1024;
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.fs.files.total", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        long value = usage.getFiles();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addGauge("host.fs.files.used", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                        long value = usage.getFiles();
                        if (value != -1)
                            return value - usage.getFreeFiles();
                        else
                            return null;
                    }
                });
            }
        } catch (Exception e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    private void initMetadata(IMeterContainer meters, FileSystem fileSystem) throws SigarException {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .putIf("dir", fileSystem.getDirName(), !"-1".equals(fileSystem.getDirName()))
                .putIf("dev", fileSystem.getDevName(), !"-1".equals(fileSystem.getDevName()))
                .putIf("fsType", fileSystem.getTypeName(), !"-1".equals(fileSystem.getTypeName()))
                .putIf("sysType", fileSystem.getSysTypeName(), !"-1".equals(fileSystem.getSysTypeName()))
                .putIf("mountOptions", fileSystem.getOptions(), !"-1".equals(fileSystem.getOptions()))
                .putIf("flags", fileSystem.getFlags(), fileSystem.getFlags() != -1)
                .toObject();
        meters.setMetadata(metadata);
    }
}
