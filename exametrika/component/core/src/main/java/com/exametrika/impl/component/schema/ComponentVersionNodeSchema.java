/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.List;

import com.exametrika.api.component.config.schema.ComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;


/**
 * The {@link ComponentVersionNodeSchema} represents a schema of component version node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentVersionNodeSchema extends ObjectNodeSchema {
    private ComponentNodeSchema component;

    public ComponentVersionNodeSchema(ComponentVersionNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                      IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public ComponentVersionNodeSchemaConfiguration getConfiguration() {
        return (ComponentVersionNodeSchemaConfiguration) super.getConfiguration();
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IObjectSpaceSchema spaceSchema = (IObjectSpaceSchema) getParent();
        String name = getConfiguration().getName();
        int pos = name.lastIndexOf("Version");
        Assert.isTrue(pos != -1);
        name = name.substring(0, pos);
        component = spaceSchema.findNode(name);
        Assert.notNull(component);
    }

    public ComponentNodeSchema getComponent() {
        return component;
    }
}
