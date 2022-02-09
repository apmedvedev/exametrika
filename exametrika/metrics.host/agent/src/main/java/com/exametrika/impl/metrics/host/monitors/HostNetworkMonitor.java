/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Tcp;

import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.metrics.host.config.HostNetworkMonitorConfiguration;
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
 * The {@link HostNetworkMonitor} is a monitor of host network interfaces.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostNetworkMonitor extends AbstractMonitor {
    private ICondition<String> filter;
    private final HostNetworkMonitorConfiguration configuration;
    private final Map<String, Long> networkInterfaceSpeed;

    public HostNetworkMonitor(HostNetworkMonitorConfiguration configuration, IMonitorContext context) {
        super("host.nets", configuration, context, false);

        this.configuration = configuration;

        if (configuration.getFilter() != null)
            filter = Strings.createFilterCondition(configuration.getFilter(), true);
        else
            filter = null;

        if (configuration.getNetworkInterfaceSpeed() != null)
            networkInterfaceSpeed = configuration.getNetworkInterfaceSpeed();
        else
            networkInterfaceSpeed = getNetworkInterfaceSpeed();
    }

    public static Map<String, Long> getNetworkInterfaceSpeed() {
        try {
            Map<String, Long> networkInterfaceSpeed = new HashMap<String, Long>();

            String[] networkInterfaces = SigarHolder.instance.getNetInterfaceList();
            for (int i = 0; i < networkInterfaces.length; i++) {
                final String name = networkInterfaces[i];
                try {
                    NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                    if (stat.getSpeed() > 0)
                        networkInterfaceSpeed.put(name, stat.getSpeed() / 8);
                    else {
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader("/sys/class/net/" + name + "/speed"));
                            long speed = Long.parseLong(reader.readLine()) * 125000;
                            if (speed > 0)
                                networkInterfaceSpeed.put(name, speed);
                        } catch (Exception e) {
                        }
                    }
                } catch (SigarException e) {
                }
            }
            return networkInterfaceSpeed;
        } catch (Exception e) {
            return Exceptions.wrapAndThrow(e);
        }
    }

    @Override
    protected void createMeters() {
        try {
            meters.addCounter("host.nets.dummy", false, 0, new IMeasurementProvider() {
                @Override
                public Object getValue() throws Exception {
                    return 1;
                }
            });

            initMetadata(SigarHolder.instance.getNetInfo());

            String[] networkInterfaces = SigarHolder.instance.getNetInterfaceList();
            for (int i = 0; i < networkInterfaces.length; i++) {
                final String name = networkInterfaces[i];
                if (filter != null && !filter.evaluate(name))
                    continue;

                String subScope = Names.escape(name);
                IMeterContainer meters = createMeterContainer(subScope, MetricName.root(), "host.net");
                initMetadata(name, meters, SigarHolder.instance.getNetInterfaceConfig(name));

                meters.addCounter("host.net.received", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                            if (stat.getRxPackets() != -1 && stat.getRxBytes() != -1)
                                return new Pair(stat.getRxPackets(), stat.getRxBytes());
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.net.sent", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                            if (stat.getTxPackets() != -1 && stat.getTxBytes() != -1)
                                return new Pair(stat.getTxPackets(), stat.getTxBytes());
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.net.rx.errors", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                            long value = stat.getRxErrors();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.net.tx.errors", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                            long value = stat.getTxErrors();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                if (configuration.isExtendedStatistics()) {
                    meters.addCounter("host.net.rx.dropped", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getRxDropped();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });

                    meters.addCounter("host.net.tx.dropped", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getTxDropped();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });

                    meters.addCounter("host.net.rx.overruns", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getRxOverruns();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });

                    meters.addCounter("host.net.tx.overruns", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getTxOverruns();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });

                    meters.addCounter("host.net.rx.frame", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getRxFrame();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });

                    meters.addCounter("host.net.tx.collisions", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getTxCollisions();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });

                    meters.addCounter("host.net.tx.carrier", false, 0, new IMeasurementProvider() {
                        @Override
                        public Object getValue() throws Exception {
                            try {
                                NetInterfaceStat stat = SigarHolder.instance.getNetInterfaceStat(name);
                                long value = stat.getTxCarrier();
                                if (value != -1)
                                    return value;
                                else
                                    return null;
                            } catch (SigarException e) {
                                return null;
                            }
                        }
                    });
                }
            }

            if (configuration.isTcpStatistics()) {
                String subScope = "tcp";
                IMeterContainer meters = createMeterContainer(subScope, MetricName.root(), "host.tcp");
                initMetadata(meters);

                meters.addCounter("host.tcp.actives", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getActiveOpens();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.passives", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getPassiveOpens();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.fails", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getAttemptFails();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.resets", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getEstabResets();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addGauge("host.tcp.connections", new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getCurrEstab();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.in", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        Tcp tcp = SigarHolder.instance.getTcp();
                        long value = tcp.getInSegs();
                        if (value != -1)
                            return value;
                        else
                            return null;
                    }
                });

                meters.addCounter("host.tcp.out", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getOutSegs();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.retransmits", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getRetransSegs();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.inErrors", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getInErrs();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });

                meters.addCounter("host.tcp.outErrors", false, 0, new IMeasurementProvider() {
                    @Override
                    public Object getValue() throws Exception {
                        try {
                            Tcp tcp = SigarHolder.instance.getTcp();
                            long value = tcp.getOutRsts();
                            if (value != -1)
                                return value;
                            else
                                return null;
                        } catch (SigarException e) {
                            return null;
                        }
                    }
                });
            }
        } catch (Exception e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    private void initMetadata(NetInfo info) throws SigarException {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .putIf("hostName", info.getHostName(), !"-1".equals(info.getHostName()))
                .putIf("domainName", info.getDomainName(), !"-1".equals(info.getDomainName()))
                .putIf("defaultGateway", info.getDefaultGateway(), !"-1".equals(info.getDefaultGateway()))
                .putIf("primaryDns", info.getPrimaryDns(), !"-1".equals(info.getPrimaryDns()))
                .putIf("secondaryDns", info.getSecondaryDns(), !"-1".equals(info.getSecondaryDns()))
                .putIf("fqdn", SigarHolder.instance.getFQDN(), !"-1".equals(SigarHolder.instance.getFQDN()))
                .toObject();
        meters.setMetadata(metadata);
    }

    private void initMetadata(String name, IMeterContainer meters, NetInterfaceConfig config) throws SigarException {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .putIf("name", config.getName(), !"-1".equals(config.getName()))
                .putIf("netType", config.getType(), !"-1".equals(config.getType()))
                .putIf("description", config.getDescription(), !"-1".equals(config.getDescription()))
                .putIf("address", config.getAddress(), !"-1".equals(config.getAddress()))
                .putIf("destination", config.getDestination(), !"-1".equals(config.getDestination()))
                .putIf("broadcast", config.getBroadcast(), !"-1".equals(config.getBroadcast()))
                .putIf("hardwareAddress", config.getHwaddr(), !"-1".equals(config.getHwaddr()))
                .putIf("netMask", config.getNetmask(), !"-1".equals(config.getNetmask()))
                .putIf("flags", config.getFlags() != -1 ? NetFlags.getIfFlagsString(config.getFlags()).trim().toLowerCase() : "", config.getFlags() != -1)
                .putIf("mtu", config.getMtu(), config.getMtu() != -1)
                .putIf("metric", config.getMetric(), config.getMetric() != -1)
                .putIf("speed", networkInterfaceSpeed.get(name), networkInterfaceSpeed.get(name) != null)
                .toObject();
        meters.setMetadata(metadata);
    }
}
