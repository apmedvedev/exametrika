/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Assert;


/**
 * The {@link AppStackCounterType} is a type of application stack counter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public enum AppStackCounterType {
    WALL_TIME("stack.wall.time", true),
    SYS_TIME("stack.sys.time", true),
    USER_TIME("stack.user.time", true),
    WAIT_TIME("stack.wait.time", true),
    WAIT_COUNT("stack.wait.count", true),
    BLOCK_TIME("stack.block.time", true),
    BLOCK_COUNT("stack.block.count", true),
    GARBAGE_COLLECTION_COUNT("stack.gc.count", true),
    GARBAGE_COLLECTION_TIME("stack.gc.time", true),
    ALLOCATION_BYTES("stack.alloc.bytes", true),
    ALLOCATION_COUNT("stack.alloc.count", false),
    ERRORS_COUNT("stack.errors.count", false),
    THREADS_COUNT("stack.threads.count", false),
    CLASSES_COUNT("stack.classes.count", false),
    IO_COUNT("stack.io.count", false),
    IO_TIME("stack.io.time", false),
    IO_BYTES("stack.io.bytes", false),
    FILE_COUNT("stack.file.count", false),
    FILE_TIME("stack.file.time", false),
    FILE_BYTES("stack.file.bytes", false),
    FILE_READ_COUNT("stack.file.read.count", false),
    FILE_READ_TIME("stack.file.read.time", false),
    FILE_READ_BYTES("stack.file.read.bytes", false),
    FILE_WRITE_COUNT("stack.file.write.count", false),
    FILE_WRITE_TIME("stack.file.write.time", false),
    FILE_WRITE_BYTES("stack.file.write.bytes", false),
    NET_COUNT("stack.net.count", false),
    NET_TIME("stack.net.time", false),
    NET_BYTES("stack.net.bytes", false),
    NET_CONNECT_COUNT("stack.net.connect.count", false),
    NET_CONNECT_TIME("stack.net.connect.time", false),
    NET_RECEIVE_COUNT("stack.net.receive.count", false),
    NET_RECEIVE_TIME("stack.net.receive.time", false),
    NET_RECEIVE_BYTES("stack.net.receive.bytes", false),
    NET_SEND_COUNT("stack.net.send.count", false),
    NET_SEND_TIME("stack.net.send.time", false),
    NET_SEND_BYTES("stack.net.send.bytes", false),
    DB_TIME("stack.db.time", false),
    DB_CONNECT_COUNT("stack.db.connect.count", false),
    DB_CONNECT_TIME("stack.db.connect.time", false),
    DB_QUERY_COUNT("stack.db.query.count", false),
    DB_QUERY_TIME("stack.db.query.time", false),
    ;

    private final String metricType;
    private final boolean system;

    private AppStackCounterType(String metricType, boolean system) {
        Assert.notNull(metricType);

        this.metricType = metricType;
        this.system = system;
    }

    public String getMetricType() {
        return metricType;
    }

    public boolean isSystem() {
        return system;
    }
}
