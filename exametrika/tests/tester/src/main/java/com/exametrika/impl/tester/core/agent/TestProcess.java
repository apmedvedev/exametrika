/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.SigarException;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;


/**
 * The {@link TestProcess} represents an OS process under test.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestProcess {
    protected static final ILogger logger = Loggers.get(TestProcess.class);
    private final ProcessBuilder processBuilder;
    private final String id = UUID.randomUUID().toString();
    private Process process;
    private long pid = -1;

    public TestProcess(ProcessBuilder processBuilder) {
        Assert.notNull(processBuilder);

        this.processBuilder = processBuilder;
        configureProcessBuilder(processBuilder);
    }

    public long getPid() {
        return pid;
    }

    public synchronized void start() {
        try {
            process = processBuilder.start();
            pid = findPid();
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    public synchronized boolean isAlive() {
        if (process != null && pid != -1)
            return isAlive(pid);

        return false;
    }

    public synchronized int getExitValue() {
        if (process != null) {
            try {
                return process.exitValue();
            } catch (IllegalThreadStateException e) {
                Exceptions.wrapAndThrow(e);
            } catch (Exception e) {
            }
        }
        return -1;
    }

    public void waitFor() {
        Process process = this.process;
        if (process != null) {
            while (true) {
                if (!isAlive())
                    break;

                Threads.sleep(1000);
            }

            stop(0);
        }
    }

    public void stop(long timeout) {
        List<Long> pids = buildPids();
        for (Long pid : pids)
            kill(pid, "SIGTERM");

        long startTime = Times.getCurrentTime();
        while (true) {
            for (Iterator<Long> it = pids.iterator(); it.hasNext(); ) {
                long pid = it.next();
                if (!isAlive(pid))
                    it.remove();
            }

            if (pids.isEmpty() || Times.getCurrentTime() > startTime + timeout)
                break;

            Threads.sleep(1000);
        }

        for (Long pid : pids)
            kill(pid, "SIGKILL");
    }

    public void destroy() {
        List<Long> pids = buildPids();
        for (Long pid : pids)
            kill(pid, "SIGKILL");
    }

    private void configureProcessBuilder(ProcessBuilder processBuilder) {
        Map<String, String> env = processBuilder.environment();
        for (Iterator<Map.Entry<String, String>> it = env.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey().startsWith("EXA_"))
                it.remove();
        }

        env.put("EXA_ID", id);
    }

    private long findPid() {
        try {
            synchronized (SigarHolder.instance) {
                long[] pids = SigarHolder.instance.getProcList();
                for (int i = 0; i < pids.length; i++) {
                    long pid = pids[i];
                    try {
                        String id = SigarHolder.instance.getProcEnv(pid, "EXA_ID");
                        if (this.id.equals(id))
                            return pid;
                    } catch (SigarException e) {
                    }
                }
            }
        } catch (SigarException e) {
            Exceptions.wrapAndThrow(e);
        }

        return -1;
    }

    private synchronized List<Long> buildPids() {
        if (pid != -1) {
            try {
                synchronized (SigarHolder.instance) {
                    long[] allPids = SigarHolder.instance.getProcList();
                    List<Pair<Long, Long>> list = new ArrayList<Pair<Long, Long>>();
                    for (int i = 0; i < allPids.length; i++) {
                        long pid = allPids[i];
                        try {
                            ProcState state = SigarHolder.instance.getProcState(pid);
                            long ppid = state.getPpid();
                            list.add(new Pair<Long, Long>(pid, ppid));
                        } catch (SigarException e) {
                        }
                    }
                    List<Long> pids = new ArrayList<Long>();
                    buildPids(pid, list, pids);
                    return pids;
                }
            } catch (Exception e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }

        return java.util.Collections.emptyList();
    }

    private void buildPids(long pid, List<Pair<Long, Long>> list, List<Long> pids) {
        pids.add(pid);
        for (Pair<Long, Long> p : list) {
            if (p.getValue() == pid)
                buildPids(p.getKey(), list, pids);
        }
    }

    private synchronized boolean isAlive(long pid) {
        if (this.pid == pid) {
            try {
                process.exitValue();
            } catch (IllegalThreadStateException e) {
                return true;
            } catch (Exception e) {
            }

            return false;
        }

        try {
            synchronized (SigarHolder.instance) {
                ProcState state = SigarHolder.instance.getProcState(pid);
                if (state.getState() != ProcState.STOP && state.getState() != ProcState.ZOMBIE)
                    return true;
            }
        } catch (SigarException e) {
        }

        return false;
    }

    private void kill(long pid, String sigName) {
        try {
            synchronized (SigarHolder.instance) {
                SigarHolder.instance.kill(pid, sigName);
            }
        } catch (SigarException e) {
        }
    }
}
    
