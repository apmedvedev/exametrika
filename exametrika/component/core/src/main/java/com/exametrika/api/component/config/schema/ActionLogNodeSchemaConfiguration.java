/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.component.nodes.ActionLogNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;

/**
 * The {@link ActionLogNodeSchemaConfiguration} represents a configuration of schema of action log node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionLogNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public static final String NAME = "ActionLog";

    public ActionLogNodeSchemaConfiguration(String alias, String description,
                                            List<? extends FieldSchemaConfiguration> fields) {
        super(NAME, alias, description, fields, null);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new ActionLogNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ActionLogNodeSchemaConfiguration))
            return false;

        ActionLogNodeSchemaConfiguration configuration = (ActionLogNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ActionLogNodeSchemaConfiguration))
            return false;

        ActionLogNodeSchemaConfiguration configuration = (ActionLogNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return ActionLogNode.class;
    }
}
