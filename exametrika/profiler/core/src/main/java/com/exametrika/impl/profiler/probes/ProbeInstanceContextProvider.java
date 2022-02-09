/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;

/**
 * The {@link ProbeInstanceContextProvider} is a probe instance context provider.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ProbeInstanceContextProvider implements IInstanceContextProvider {
    private final ITimeService timeService;
    private JsonObject context;

    public ProbeInstanceContextProvider(ITimeService timeService) {
        Assert.notNull(timeService);

        this.timeService = timeService;
    }

    @Override
    public JsonObject getContext() {
        return context;
    }

    @Override
    public void setContext(JsonObject value) {
        context = value;
    }

    @Override
    public long getExtractionTime() {
        return timeService.getCurrentTime();
    }
}