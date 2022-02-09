/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.jobs;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IJobContext;


/**
 * The {@link ComponentJobOperation} is a base component job operation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComponentJobOperation extends CompletionHandler implements IAsynchronousJobOperation {
    private final IJobContext context;
    protected boolean async;

    public ComponentJobOperation(IJobContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public void run() {
        context.getDatabaseContext().getDatabase().transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema componentSpaceSchema = context.getDatabaseContext().getSchemaSpace().getCurrentSchema(
                ).findSchemaById("space:component.component");
                IObjectSpace componentSpace = componentSpaceSchema.getSpace();
                INodeIndex<Long, ComponentNode> index = componentSpace.findIndex("componentIndex");

                long scopeId = (Long) context.getSchema().getParameters().get("component.scopeId");
                ComponentNode component = index.find(scopeId);
                if (component.allowExecution()) {
                    execute(context, component);
                    if (!async)
                        context.onSucceeded(null);
                } else
                    context.onSucceeded(null);
            }

            @Override
            public void onRolledBack() {
                context.onFailed(null);
            }
        });
    }

    @Override
    public void onSucceeded(Object result) {
        context.onSucceeded(result);
    }

    @Override
    public void onFailed(Throwable error) {
        context.onFailed(error);
    }

    protected abstract void execute(IJobContext jobContext, IComponent component);
}
