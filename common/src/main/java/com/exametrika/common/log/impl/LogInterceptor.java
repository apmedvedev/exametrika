/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log.impl;


/**
 * The {@link LogInterceptor} is a log interceptor.
 *
 * @author AndreyM
 * @threadsafety Implementations of this class and its methods are thread safe.
 */
public class LogInterceptor {
    public static LogInterceptor INSTANCE = new LogInterceptor();

    public void onLog(LogEvent event) {
    }
}
