/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.SigarException;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.IMonitorContext;

public class ProcessMeterContainer extends MeterContainer {
    protected final long processId;
    private Cpu cpu;
    private ProcState state;
    private ProcTime time;
    private ProcFd fd;
    private ProcMem mem;
    private Mem hostMem;

    public ProcessMeterContainer(NameMeasurementId id, IMonitorContext context,
                                 IInstanceContextProvider contextProvider, long processId) {
        super(id, context, contextProvider);

        this.processId = processId;

        createMeters();
    }

    @Override
    public void measure() {
        try {
            boolean first = state == null;
            cpu = SigarHolder.instance.getCpu();
            state = SigarHolder.instance.getProcState(processId);
            time = SigarHolder.instance.getProcTime(processId);
            fd = SigarHolder.instance.getProcFd(processId);
            mem = SigarHolder.instance.getProcMem(processId);
            hostMem = SigarHolder.instance.getMem();

            if (first)
                initMetadata();

            super.measure();
        } catch (SigarException e) {
            delete();
        }
    }

    protected void createMeters() {
        addGauge("host.process.threads", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = state.getThreads();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addInfo("host.process.state", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    return getProcessState(state.getState());
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addCounter("host.process.time", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return System.nanoTime() / 1000000;
            }
        });

        addCounter("host.process.cpu.max", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = cpu.getTotal();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addCounter("host.process.cpu.total", false, 10, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = time.getTotal();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addCounter("host.process.cpu.user", false, 10, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = time.getUser();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addCounter("host.process.cpu.sys", false, 10, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = time.getSys();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addGauge("host.process.fd", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = fd.getTotal();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addGauge("host.process.memory.max", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = hostMem.getTotal();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addGauge("host.process.memory.total", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = mem.getSize();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addGauge("host.process.memory.shared", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = mem.getShare();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addGauge("host.process.memory.resident", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = mem.getResident();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addCounter("host.process.memory.majorFaults", false, 10, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = mem.getMajorFaults();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addCounter("host.process.memory.minorFaults", false, 10, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = mem.getMinorFaults();
                    if (value != -1)
                        return value;
                    else
                        return null;
                } catch (Exception e) {
                    return Exceptions.wrapAndThrow(e);
                }
            }
        });

        addInfo("host.process.processor", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                try {
                    long value = state.getProcessor();
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

    protected void initMetadata(Json json) {
    }

    private void initMetadata() throws SigarException {
        ProcState state = SigarHolder.instance.getProcState(processId);
        ProcExe exe = SigarHolder.instance.getProcExe(processId);
        ProcCredName cred = SigarHolder.instance.getProcCredName(processId);
        Json metadata = Json.object()
                .put("node", ((IMonitorContext) context).getConfiguration().getNodeName())
                .put("name", state.getName())
                .put("id", processId)
                .put("parentId", state.getPpid())
                .put("command", exe.getName())
                .put("workingDir", exe.getCwd())
                .put("args", JsonUtils.toJson(SigarHolder.instance.getProcArgs(processId)))
                .put("environment", JsonUtils.toJson(SigarHolder.instance.getProcEnv(processId)))
                .put("user", cred.getUser())
                .put("group", cred.getGroup())
                .put("modules", JsonUtils.toJson(SigarHolder.instance.getProcModules(processId)))
                .put("priority", state.getPriority())
                .put("nice", state.getNice())
                .put("startTime", SigarHolder.instance.getProcCpu(processId).getStartTime());

        initMetadata(metadata);
        setMetadata(metadata.toObject());
    }

    private String getProcessState(char c) {
        if (c == ProcState.IDLE)
            return "idle";
        else if (c == ProcState.RUN)
            return "running";
        else if (c == ProcState.SLEEP)
            return "sleeping";
        else if (c == ProcState.STOP)
            return "stopped";
        else if (c == ProcState.ZOMBIE)
            return "zombie";
        else
            return "unknown";
    }
}