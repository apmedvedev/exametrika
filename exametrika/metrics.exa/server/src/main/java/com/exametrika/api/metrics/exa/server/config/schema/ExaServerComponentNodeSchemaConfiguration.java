/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.schema.HealthComponentNodeSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.metrics.exa.server.nodes.ExaServerComponentNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link ExaServerComponentNodeSchemaConfiguration} represents a configuration of schema of exa server component node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerComponentNodeSchemaConfiguration extends HealthComponentNodeSchemaConfiguration {
    public ExaServerComponentNodeSchemaConfiguration(String name, String alias, String description,
                                                     List<? extends FieldSchemaConfiguration> fields, ComponentSchemaConfiguration component) {
        super(name, alias, description, fields, component);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new ExaServerComponentNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaServerComponentNodeSchemaConfiguration))
            return false;

        ExaServerComponentNodeSchemaConfiguration configuration = (ExaServerComponentNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ExaServerComponentNodeSchemaConfiguration))
            return false;

        ExaServerComponentNodeSchemaConfiguration configuration = (ExaServerComponentNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return ExaServerComponentNode.class;
    }
}
