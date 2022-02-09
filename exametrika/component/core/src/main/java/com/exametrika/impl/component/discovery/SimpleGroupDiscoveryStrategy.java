/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.component.config.model.SimpleGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleGroupDiscoveryStrategy} is a simple group discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleGroupDiscoveryStrategy implements IGroupDiscoveryStrategy {
    private final SimpleGroupDiscoveryStrategySchemaConfiguration configuration;
    private final IDatabaseContext context;
    private IObjectSpaceSchema spaceSchema;
    private IPeriodNameManager nameManager;

    public SimpleGroupDiscoveryStrategy(SimpleGroupDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public List<IGroupComponent> getGroups(IComponent initialComponent, IComponent childComponent, int level) {
        if (spaceSchema == null) {
            spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
            nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
        }

        IObjectSpace space = spaceSchema.getSpace();
        INodeIndex<Long, IGroupComponent> index = space.findIndex("componentIndex");

        IPeriodName name = nameManager.findByName(Names.getScope(configuration.getGroup()));
        if (name == null)
            return Collections.emptyList();

        IGroupComponent group = index.find(name.getId());
        if (group != null)
            return Collections.singletonList(group);
        else
            return Collections.emptyList();
    }
}
