/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.selectors;

import java.util.Map;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.schema.SelectorSchema;


/**
 * The {@link Selector} is a selector.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Selector implements ISelector {
    protected ComponentNode component;
    protected final SelectorSchema schema;

    public Selector(IComponent component, ISelectorSchema schema) {
        Assert.notNull(component);
        Assert.notNull(schema);

        this.component = (ComponentNode) component;
        this.schema = (SelectorSchema) schema;
    }

    @Override
    public SelectorSchema getSchema() {
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
    public final Object select(Map<String, ?> parameters) {
        IPermission permission = schema.getExecutePermission();
        permission.beginCheck(this);

        Object result = doSelect(parameters);

        permission.endCheck();

        return result;
    }

    protected abstract Object doSelect(Map<String, ?> parameters);

    private IComponent refreshComponent() {
        IObjectSpaceSchema spaceSchema = schema.getContext().getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
        component = spaceSchema.getSpace().findNodeById(component.getId());
        return component;
    }
}
