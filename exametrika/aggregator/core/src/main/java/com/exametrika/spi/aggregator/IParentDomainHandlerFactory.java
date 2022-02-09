/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link IParentDomainHandlerFactory} represents a factpry of parent domain measurement handler.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IParentDomainHandlerFactory {
    /**
     * Creates measurement handler, which can send measurements to parent domain
     *
     * @param context context
     * @return parent domain measurement handler
     */
    IMeasurementHandler createHander(IDatabaseContext context);
}
