/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.monitors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.SigarException;

import com.exametrika.impl.profiler.SigarHolder;
import com.exametrika.spi.metrics.host.IProcessContext;


/**
 * The {@link ProcessContext} is a filter context of host process.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ProcessContext implements IProcessContext {
    private long id;

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getParentId() {
        try {
            return SigarHolder.instance.getProcState(id).getPpid();
        } catch (SigarException e) {
            return -1;
        }
    }

    @Override
    public String getName() {
        try {
            return SigarHolder.instance.getProcState(id).getName();
        } catch (SigarException e) {
            return "";
        }
    }

    @Override
    public String getCommand() {
        try {
            return SigarHolder.instance.getProcExe(id).getName();
        } catch (SigarException e) {
            return "";
        }
    }

    @Override
    public String getWorkingDir() {
        try {
            return SigarHolder.instance.getProcExe(id).getCwd();
        } catch (SigarException e) {
            return "";
        }
    }

    @Override
    public String[] getArgs() {
        try {
            return SigarHolder.instance.getProcArgs(id);
        } catch (SigarException e) {
            return new String[0];
        }
    }

    @Override
    public Map<String, String> getEnvironment() {
        try {
            return SigarHolder.instance.getProcEnv(id);
        } catch (SigarException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public String getUser() {
        try {
            return SigarHolder.instance.getProcCredName(id).getUser();
        } catch (SigarException e) {
            return "";
        }
    }

    @Override
    public String getGroup() {
        try {
            return SigarHolder.instance.getProcCredName(id).getGroup();
        } catch (SigarException e) {
            return "";
        }
    }

    @Override
    public List<String> getModules() {
        try {
            return SigarHolder.instance.getProcModules(id);
        } catch (SigarException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public int getPriority() {
        try {
            return SigarHolder.instance.getProcState(id).getPriority();
        } catch (SigarException e) {
            return -1;
        }
    }

    @Override
    public long getStartTime() {
        try {
            return SigarHolder.instance.getProcCpu(id).getStartTime();
        } catch (SigarException e) {
            return 0;
        }
    }
}
