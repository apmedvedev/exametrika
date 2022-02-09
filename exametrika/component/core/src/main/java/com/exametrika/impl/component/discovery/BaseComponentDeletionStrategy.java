/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.HealthComponentNode;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link BaseComponentDeletionStrategy} is a base component deletion strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BaseComponentDeletionStrategy implements IComponentDeletionStrategy {
    protected final IDatabaseContext context;
    private IObjectSpaceSchema spaceSchema;

    public BaseComponentDeletionStrategy(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public void processDeleted(Set<Long> existingComponents) {
        if (spaceSchema == null)
            spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");

        IObjectSpace space = spaceSchema.getSpace();
        ComponentRootNode root = space.getRootNode();

        List<IHealthComponent> deletingComponents = new ArrayList<IHealthComponent>();
        for (IHealthComponent component : getComponents(root)) {
            IHealthComponentVersion version = (IHealthComponentVersion) component.getCurrentVersion();
            if (version.isDynamic()) {
                if (version.getState() != State.MAINTENANCE && !existingComponents.contains(component.getScopeId())) {
                    if (!isActive(component))
                        deletingComponents.add(component);
                } else
                    onActivate(component);
            } else if (version.getState() != State.MAINTENANCE && version.getState() != State.UNAVAILABLE && !existingComponents.contains(component.getScopeId()))
                ((HealthComponentNode) component).setUnavailableState();
        }

        for (IHealthComponent agent : deletingComponents)
            agent.delete();
    }

    protected abstract Iterable<? extends IHealthComponent> getComponents(ComponentRootNode root);

    protected abstract boolean isActive(IHealthComponent component);

    protected void onActivate(IHealthComponent component) {
    }
}
