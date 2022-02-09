/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.instance;

import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.InstanceSerializer;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link InstanceFieldFactory} is an implementation of {@link IFieldFactory} for instance fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceFieldFactory implements IFieldFactory {
    private final InstanceFieldConfiguration configuration;
    private final IMeasurementIdProvider idProvider;
    private final IInstanceContextProvider contextProvider;

    public InstanceFieldFactory(InstanceFieldConfiguration configuration, IMeasurementIdProvider idProvider,
                                IInstanceContextProvider contextProvider) {
        Assert.notNull(configuration);
        Assert.notNull(idProvider);
        Assert.notNull(contextProvider);

        this.configuration = configuration;
        this.idProvider = idProvider;
        this.contextProvider = contextProvider;
    }

    @Override
    public IFieldCollector createCollector() {
        return new InstanceCollector(configuration, idProvider, contextProvider);
    }

    @Override
    public IFieldValueSerializer createValueSerializer() {
        return new InstanceSerializer(false, configuration.isMax());
    }
}