/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.time.impl;

import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Times;


/**
 * The {@link SystemTimeService} is an implementation of {@link ITimeService} interface based on system time.
 *
 * @author andreym
 * @threadsafety This class and its methods are thread safe.
 */
public class SystemTimeService implements ITimeService {
    @Override
    public long getCurrentTime() {
        return Times.getCurrentTime();
    }
}
