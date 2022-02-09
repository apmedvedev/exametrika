/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.component.nodes.IncidentNode;
import com.exametrika.impl.component.schema.IncidentNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;

/**
 * The {@link IncidentNodeSchemaConfiguration} represents a configuration of schema of incident node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class IncidentNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public static final String NAME = "Incident";

    public IncidentNodeSchemaConfiguration(String alias, String description,
                                           List<? extends FieldSchemaConfiguration> fields) {
        this(NAME, alias, description, fields);
    }

    public IncidentNodeSchemaConfiguration(String name, String alias, String description,
                                           List<? extends FieldSchemaConfiguration> fields) {
        super(name, alias, description, fields, null);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new IncidentNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new IncidentNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IncidentNodeSchemaConfiguration))
            return false;

        IncidentNodeSchemaConfiguration configuration = (IncidentNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IncidentNodeSchemaConfiguration))
            return false;

        IncidentNodeSchemaConfiguration configuration = (IncidentNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return IncidentNode.class;
    }
}
