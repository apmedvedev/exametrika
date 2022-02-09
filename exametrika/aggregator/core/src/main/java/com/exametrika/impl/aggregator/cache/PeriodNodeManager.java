/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.cache;

import com.exametrika.common.time.ITimeService;
import com.exametrika.impl.exadb.objectdb.cache.NodeManager;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link PeriodNodeManager} is a manager of period nodes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNodeManager extends NodeManager {
    public PeriodNodeManager(IDatabaseContext context, ITimeService timeService) {
        super(context, timeService);
    }
}
