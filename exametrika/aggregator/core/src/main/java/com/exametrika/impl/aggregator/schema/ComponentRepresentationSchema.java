/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IComponentRepresentationSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComponentComputer;

/**
 * The {@link ComponentRepresentationSchema} is a component representation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ComponentRepresentationSchema implements IComponentRepresentationSchema {
    private final ComponentRepresentationSchemaConfiguration configuration;
    private final int index;
    private final IComponentAccessorFactory accessorFactory;
    private final IComponentComputer computer;

    public ComponentRepresentationSchema(ComponentValueSchemaConfiguration componentSchema,
                                         ComponentRepresentationSchemaConfiguration configuration, int index) {
        Assert.notNull(componentSchema);
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.index = index;
        this.accessorFactory = configuration.createAccessorFactory(componentSchema);
        this.computer = configuration.createComputer(componentSchema);
    }

    @Override
    public ComponentRepresentationSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public IComponentAccessorFactory getAccessorFactory() {
        return accessorFactory;
    }

    @Override
    public IComponentComputer getComputer() {
        return computer;
    }
}
