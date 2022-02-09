/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.impl.component.nodes.BehaviorTypeNode;
import com.exametrika.impl.component.schema.BehaviorTypeNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;

/**
 * The {@link BehaviorTypeNodeSchemaConfiguration} represents a configuration of schema of behavior type node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BehaviorTypeNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
    public static final String NAME = "BehaviorType";

    public BehaviorTypeNodeSchemaConfiguration(String alias, String description,
                                               List<? extends FieldSchemaConfiguration> fields) {
        super(NAME, alias, description, fields, null);
    }

    @Override
    public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
        return new BehaviorTypeNodeSchema(this, index, fields, documentSchema);
    }

    @Override
    public INodeObject createNode(INode node) {
        return new BehaviorTypeNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BehaviorTypeNodeSchemaConfiguration))
            return false;

        BehaviorTypeNodeSchemaConfiguration configuration = (BehaviorTypeNodeSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof BehaviorTypeNodeSchemaConfiguration))
            return false;

        BehaviorTypeNodeSchemaConfiguration configuration = (BehaviorTypeNodeSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected Class getNodeClass() {
        return BehaviorTypeNode.class;
    }
}
