/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.impl.component.nodes.IncidentGroupNode;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link IncidentGroupNodeSchemaConfiguration} represents a configuration of schema of incident group node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IncidentGroupNodeSchemaConfiguration extends IncidentNodeSchemaConfiguration {
    public static final String NAME = "IncidentGroup";

    public IncidentGroupNodeSchemaConfiguration(String alias, String description,
                                                List<? extends FieldSchemaConfiguration> fields) {
        super(NAME, alias, description, fields);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new IncidentGroupNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IncidentGroupNodeSchemaConfiguration))
            return false;

        IncidentGroupNodeSchemaConfiguration configuration = (IncidentGroupNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IncidentGroupNodeSchemaConfiguration))
            return false;

        IncidentGroupNodeSchemaConfiguration configuration = (IncidentGroupNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return IncidentGroupNode.class;
    }
}
