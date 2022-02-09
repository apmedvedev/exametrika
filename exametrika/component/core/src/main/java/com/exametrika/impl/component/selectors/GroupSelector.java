/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.selectors;

import java.util.Map;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link GroupSelector} is a group selector.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupSelector implements ISelector {
    private GroupComponentNode group;
    private final String name;
    private final IDatabaseContext context;

    public GroupSelector(IGroupComponent group, String name, IDatabaseContext context) {
        Assert.notNull(group);
        Assert.notNull(name);
        Assert.notNull(context);

        this.group = (GroupComponentNode) group;
        this.name = name;
        this.context = context;
    }

    @Override
    public IGroupComponent getComponent() {
        if (!group.isStale())
            return group;
        else
            return refreshComponent();
    }

    @Override
    public ISelectorSchema getSchema() {
        return null;
    }

    @Override
    public Object select(Map<String, ?> parameters) {
        JsonArrayBuilder builder = new JsonArrayBuilder();
        IGroupComponentVersion groupVersion = (IGroupComponentVersion) group.get();
        if (groupVersion != null) {
            for (IComponent component : groupVersion.getComponents()) {
                if (!((ComponentNode) component).getSchema().getConfiguration().getComponent().getSelectors().containsKey(name))
                    continue;

                ISelector selector = component.createSelector(name);
                builder.add(selector.select(parameters));
            }
        }

        return builder.toJson();
    }

    private IGroupComponent refreshComponent() {
        IObjectSpaceSchema spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
        group = spaceSchema.getSpace().findNodeById(group.getId());
        return group;
    }
}
