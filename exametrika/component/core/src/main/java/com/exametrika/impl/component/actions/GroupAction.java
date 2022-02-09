/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.actions;

import java.util.Map;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.IAsyncAction;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link GroupAction} is a group action.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupAction implements IAsyncAction {
    private GroupComponentNode group;
    private final String name;
    private final boolean recursive;
    private final IDatabaseContext context;

    public GroupAction(IGroupComponent group, String name, boolean recursive, IDatabaseContext context) {
        Assert.notNull(group);
        Assert.notNull(name);
        Assert.notNull(context);

        this.group = (GroupComponentNode) group;
        this.name = name;
        this.recursive = recursive;
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
    public IActionSchema getSchema() {
        return null;
    }

    @Override
    public void execute(Map<String, ?> parameters) {
        execute(getComponent(), parameters, null);
    }

    @Override
    public <T> void execute(Map<String, ?> parameters, ICompletionHandler<T> completionHandler) {
        execute(getComponent(), parameters, completionHandler);
    }

    private <T> void execute(IGroupComponent group, Map<String, ?> parameters, ICompletionHandler<T> completionHandler) {
        for (IComponent component : ((IGroupComponentVersion) group.getCurrentVersion()).getComponents()) {
            if (!((ComponentNode) component).getSchema().getConfiguration().getComponent().getActions().containsKey(name))
                continue;

            IAction action = component.createAction(name);
            if (action instanceof IAsyncAction)
                ((IAsyncAction) action).execute(parameters, completionHandler);
            else {
                Assert.isNull(completionHandler);
                action.execute(parameters);
            }
        }

        if (recursive) {
            for (IGroupComponent child : ((IGroupComponentVersion) group.getCurrentVersion()).getChildren())
                execute(child, parameters, completionHandler);
        }
    }

    private IGroupComponent refreshComponent() {
        IObjectSpaceSchema spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
        group = spaceSchema.getSpace().findNodeById(group.getId());
        return group;
    }
}
