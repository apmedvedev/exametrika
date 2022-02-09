/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.actions;

import java.util.Map;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.schema.ActionSchema;


/**
 * The {@link Action} is a action.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Action implements IAction {
    private ComponentNode component;
    protected final ActionSchema schema;

    public Action(IComponent component, IActionSchema schema) {
        Assert.notNull(component);
        Assert.notNull(schema);

        this.component = (ComponentNode) component;
        this.schema = (ActionSchema) schema;
    }

    @Override
    public ActionSchema getSchema() {
        return schema;
    }

    @Override
    public final IComponent getComponent() {
        if (!component.isStale())
            return component;
        else
            return refreshComponent();
    }

    @Override
    public final void execute(Map<String, ?> parameters) {
        IPermission permission = schema.getExecutePermission();
        permission.beginCheck(this);

        doExecute(parameters);

        permission.endCheck();
    }

    protected abstract void doExecute(Map<String, ?> parameters);

    private IComponent refreshComponent() {
        IObjectSpaceSchema spaceSchema = schema.getContext().getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
        component = spaceSchema.getSpace().findNodeById(component.getId());
        return component;
    }
}
