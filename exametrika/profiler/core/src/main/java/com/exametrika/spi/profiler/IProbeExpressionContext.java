/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link IProbeExpressionContext} represents a probe expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IProbeExpressionContext extends IExpressionContext {
    int wallTime = 0;
    int sysTime = 1;
    int userTime = 2;
    int waitTime = 3;
    int waitCount = 4;
    int blockTime = 5;
    int blockCount = 6;
    int gcCount = 7;
    int gcTime = 8;
    int allocationBytes = 9;
    int allocationCount = 10;
    int errorsCount = 11;
    int threadsCount = 12;
    int classesCount = 13;
    int ioCount = 14;
    int ioTime = 15;
    int ioBytes = 16;
    int fileCount = 17;
    int fileTime = 18;
    int fileBytes = 19;
    int fileReadCount = 20;
    int fileReadTime = 21;
    int fileReadBytes = 22;
    int fileWriteCount = 23;
    int fileWriteTime = 24;
    int fileWriteBytes = 25;
    int netCount = 26;
    int netTime = 27;
    int netBytes = 28;
    int netConnectCount = 29;
    int netConnectTime = 30;
    int netReceiveCount = 31;
    int netReceiveTime = 32;
    int netReceiveBytes = 33;
    int netSendCount = 34;
    int netSendTime = 35;
    int netSendBytes = 36;
    int dbTime = 37;
    int dbConnectCount = 38;
    int dbConnectTime = 39;
    int dbQueryCount = 40;
    int dbQueryTime = 41;

    /**
     * Returns current thread cpu time.
     *
     * @return current thread cpu time
     */
    long getThreadCpuTime();

    /**
     * Returns counter value.
     *
     * @param type counter type
     * @return counter value
     */
    Object counter(int type);
}
