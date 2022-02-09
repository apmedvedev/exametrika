/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.common.time.ITimeService;

public class TestTimeService implements ITimeService {
    public long time;

    @Override
    public long getCurrentTime() {
        return time;
    }
}