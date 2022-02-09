/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;


/**
 * The {@link TimeSource} is a type of time source.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public enum TimeSource {
    WALL_TIME,

    THREAD_CPU_TIME
}
