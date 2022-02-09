/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.standard;

import com.exametrika.impl.aggregator.common.values.StandardSerializer;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link StandardFieldFactory} is an implementation of {@link IFieldFactory} for standard fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardFieldFactory implements IFieldFactory {
    @Override
    public IFieldCollector createCollector() {
        return new StandardCollector();
    }

    @Override
    public IFieldValueSerializer createValueSerializer() {
        return new StandardSerializer(false);
    }
}
