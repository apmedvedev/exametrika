/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import java.util.Map;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;

import com.exametrika.api.metrics.host.config.HostKpiMonitorConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IMeterContainer;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link HostKpiMonitor} is a monitor of host runtime.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostKpiMonitor extends AbstractMonitor {
    private final ICondition<String> fileSystemFilter;
    private final ICondition<String> networkInterfaceFilter;

    public HostKpiMonitor(HostKpiMonitorConfiguration configuration, IMonitorContext context) {
        super(configuration.getComponentType(), configuration, context, false);

        if (configuration.getFileSystemFilter() != null)
            fileSystemFilter = Strings.createFilterCondition(configuration.getFileSystemFilter(), true);
        else
            fileSystemFilter = null;

        if (configuration.getNetworkInterfaceFilter() != null)
            networkInterfaceFilter = Strings.createFilterCondition(configuration.getNetworkInterfaceFilter(), true);
        else
            networkInterfaceFilter = null;
    }

    @Override
    protected void createMeters() {
        meters.addCounter("host.cpu.total", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                CpuInfo[] cpuInfos = SigarHolder.instance.getCpuInfoList();
                Cpu[] cpus = SigarHolder.instance.getCpuList();
                long sum = 0;
                for (int i = 0; i < cpuInfos.length; i++) {
                    long value = cpus[i].getTotal();
                    if (value != -1)
                        sum += value;
                }

                return sum;
            }
        });

        meters.addCounter("host.cpu.idle", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                CpuInfo[] cpuInfos = SigarHolder.instance.getCpuInfoList();
                Cpu[] cpus = SigarHolder.instance.getCpuList();
                long sum = 0;
                for (int i = 0; i < cpuInfos.length; i++) {
                    long value = cpus[i].getIdle();
                    if (value != -1)
                        sum += value;
                }

                return sum;
            }
        });

        meters.addCounter("host.cpu.used", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                CpuInfo[] cpuInfos = SigarHolder.instance.getCpuInfoList();
                Cpu[] cpus = SigarHolder.instance.getCpuList();
                long sum = 0;
                for (int i = 0; i < cpuInfos.length; i++) {
                    long value = cpus[i].getSys();
                    if (value != -1)
                        sum += value;
                    value = cpus[i].getUser();
                    if (value != -1)
                        sum += value;
                }

                return sum;
            }
        });

        meters.addCounter("host.cpu.io", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                CpuInfo[] cpuInfos = SigarHolder.instance.getCpuInfoList();
                Cpu[] cpus = SigarHolder.instance.getCpuList();
                long sum = 0;
                for (int i = 0; i < cpuInfos.length; i++) {
                    long value = cpus[i].getIrq();
                    if (value != -1)
                        sum += value;
                    value = cpus[i].getSoftIrq();
                    if (value != -1)
                        sum += value;
                    value = cpus[i].getNice();
                    if (value != -1)
                        sum += value;
                    value = cpus[i].getStolen();
                    if (value != -1)
                        sum += value;
                    value = cpus[i].getWait();
                    if (value != -1)
                        sum += value;
                }

                return sum;
            }
        });

        meters.addGauge("host.memory.total", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getTotal();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.memory.used", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getActualUsed();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.memory.free", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getMem().getActualFree();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addCounter("host.disk.read", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                FileSystem[] fileSystems = SigarHolder.instance.getFileSystemList();
                long count = 0, bytes = 0;
                for (int i = 0; i < fileSystems.length; i++) {
                    FileSystem fileSystem = fileSystems[i];
                    if (fileSystem.getType() != FileSystem.TYPE_LOCAL_DISK)
                        continue;

                    final String name = fileSystem.getDirName();
                    if (fileSystemFilter != null && !fileSystemFilter.evaluate(name))
                        continue;

                    FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                    if (usage.getDiskReads() != -1 && usage.getDiskReadBytes() != -1) {
                        count += usage.getDiskReads();
                        bytes += usage.getDiskReadBytes();
                    }
                }

                return new Pair(count, bytes);
            }
        });

        meters.addCounter("host.disk.write", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                FileSystem[] fileSystems = SigarHolder.instance.getFileSystemList();
                long count = 0, bytes = 0;
                for (int i = 0; i < fileSystems.length; i++) {
                    FileSystem fileSystem = fileSystems[i];
                    if (fileSystem.getType() != FileSystem.TYPE_LOCAL_DISK)
                        continue;

                    final String name = fileSystem.getDirName();
                    if (fileSystemFilter != null && !fileSystemFilter.evaluate(name))
                        continue;

                    FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(name);
                    if (usage.getDiskWrites() != -1 && usage.getDiskWriteBytes() != -1) {
                        count += usage.getDiskWrites();
                        bytes += usage.getDiskWriteBytes();
                    }
                }

                return new Pair(count, bytes);
            }
        });

        meters.addCounter("host.net.received", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                String[] networkInterfaces = SigarHolder.instance.getNetInterfaceList();
                long packets = 0, bytes = 0;
                for (int i = 0; i < networkInterfaces.length; i++) {
                    final String name = networkInterfaces[i];
                    if (networkInterfaceFilter != null && !networkInterfaceFilter.evaluate(name))
                        continue;

                    try {
                        NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                        if (stat.getRxPackets() != -1 && stat.getRxBytes() != -1) {
                            packets += stat.getRxPackets();
                            bytes += stat.getRxBytes();
                        }
                    } catch (SigarException e) {
                    }
                }

                return new Pair(packets, bytes);
            }
        });

        meters.addCounter("host.net.sent", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                String[] networkInterfaces = SigarHolder.instance.getNetInterfaceList();
                long packets = 0, bytes = 0;
                for (int i = 0; i < networkInterfaces.length; i++) {
                    final String name = networkInterfaces[i];
                    if (networkInterfaceFilter != null && !networkInterfaceFilter.evaluate(name))
                        continue;

                    NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                    if (stat.getTxPackets() != -1 && stat.getTxBytes() != -1) {
                        packets += stat.getTxPackets();
                        bytes += stat.getTxBytes();
                    }
                }

                return new Pair(packets, bytes);
            }
        });

        meters.addGauge("host.swap.total", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getTotal();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.swap.used", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getUsed();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addGauge("host.swap.free", new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getFree();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });

        meters.addCounter("host.swap.pagesIn", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                long value = SigarHolder.instance.getSwap().getPageIn();
                if (value != -1)
                    return value;
                else
                    return null;
            }
        });
    }

    @Override
    protected void initMetadata(IMeterContainer meters) {
        OperatingSystem os = OperatingSystem.getInstance();

        String cpu = "";
        long memory = 0;
        long disk = 0;
        long network = 0;
        long swap = 0;
        try {
            CpuInfo[] cpuInfos = SigarHolder.instance.getCpuInfoList();
            if (cpuInfos.length > 0)
                cpu = cpuInfos[0].getTotalCores() + " x " + cpuInfos[0].getModel();

            memory = SigarHolder.instance.getMem().getTotal();

            FileSystem[] fileSystems = SigarHolder.instance.getFileSystemList();
            for (int i = 0; i < fileSystems.length; i++) {
                FileSystem fileSystem = fileSystems[i];
                if (fileSystem.getType() != FileSystem.TYPE_LOCAL_DISK)
                    continue;

                FileSystemUsage usage = SigarHolder.instance.getFileSystemUsage(fileSystem.getDirName());
                disk += usage.getTotal() * 1024;
            }

            Map<String, Long> netSpeeds = HostNetworkMonitor.getNetworkInterfaceSpeed();
            for (long speed : netSpeeds.values()) {
                if (network < speed)
                    network = speed;
            }

            swap = SigarHolder.instance.getSwap().getTotal();
        } catch (SigarException e) {

        }

        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .put("host", System.getProperty("com.exametrika.hostName"))
                .put("name", os.getName())
                .put("version", os.getVersion())
                .put("arch", os.getArch())
                .put("machine", os.getMachine())
                .put("description", os.getDescription())
                .put("patchLevel", os.getPatchLevel())
                .put("vendor", os.getVendor())
                .put("vendorVersion", os.getVendorVersion())
                .put("vendorName", os.getVendorName())
                .put("vendorCode", os.getVendorCodeName())
                .put("dataModel", os.getDataModel())
                .put("cpuEndian", os.getCpuEndian())
                .put("cpu", cpu)
                .put("memory", memory)
                .put("disk", disk)
                .put("network", network)
                .put("swap", swap)
                .put("nodeProperties", context.getConfiguration().getNodeProperties())
                .toObject();
        meters.setMetadata(metadata);
    }
}
