/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSpaceSchemaConfiguration;


/**
 * The {@link ObjectSpaceSchemaConfiguration} represents a configuration of schema of object node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectSpaceSchemaConfiguration extends NodeSpaceSchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.objectdb-1.0";

    private final int pathIndex;
    private final int fullTextPathIndex;

    public ObjectSpaceSchemaConfiguration(String name, Set<? extends NodeSchemaConfiguration> nodes, String rootNodeType) {
        this(name, name, null, nodes, rootNodeType, 0, 0);
    }

    public ObjectSpaceSchemaConfiguration(String name, String alias, String description, Set<? extends NodeSchemaConfiguration> nodes,
                                          String rootNodeType, int pathIndex, int fullTextPathIndex) {
        this(name, alias, description, nodes, rootNodeType, pathIndex, fullTextPathIndex, true);
    }

    public ObjectSpaceSchemaConfiguration(String name, String alias, String description, Set<? extends NodeSchemaConfiguration> nodes,
                                          String rootNodeType, int pathIndex, int fullTextPathIndex, boolean freezed) {
        super(name, alias, description, nodes, rootNodeType);

        Assert.isTrue(nodes.size() <= Constants.MAX_SPACE_NODE_SCHEMA_COUNT);

        if (rootNodeType != null) {
            NodeSchemaConfiguration rootSchema = findNode(rootNodeType);
            Assert.notNull(rootSchema);

            for (FieldSchemaConfiguration field : rootSchema.getFields())
                Assert.isTrue(!field.isPrimary());
        }

        this.pathIndex = pathIndex;
        this.fullTextPathIndex = fullTextPathIndex;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public int getFullTextPathIndex() {
        return fullTextPathIndex;
    }

    @Override
    public ISpaceSchema createSchema(IDatabaseContext context, int version) {
        return new ObjectSpaceSchema(this, context, version);
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        ObjectSpaceSchemaConfiguration objectSpaceSchema = (ObjectSpaceSchemaConfiguration) schema;
        Set<NodeSchemaConfiguration> nodes = new HashSet<NodeSchemaConfiguration>();
        Map<String, NodeSchemaConfiguration> nodesMap = new HashMap<String, NodeSchemaConfiguration>(this.nodesMap);
        for (NodeSchemaConfiguration node : objectSpaceSchema.getNodes())
            nodes.add(combine(node, nodesMap));
        nodes.addAll(nodesMap.values());

        return (T) new ObjectSpaceSchemaConfiguration(combine(getName(), schema.getName()), combine(getAlias(), schema.getAlias()),
                combine(getDescription(), schema.getDescription()), nodes, combine(getRootNodeType(),
                objectSpaceSchema.getRootNodeType()), combine(pathIndex, objectSpaceSchema.getPathIndex()),
                combine(fullTextPathIndex, objectSpaceSchema.getFullTextPathIndex()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ObjectSpaceSchemaConfiguration))
            return false;

        ObjectSpaceSchemaConfiguration configuration = (ObjectSpaceSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex && fullTextPathIndex == configuration.fullTextPathIndex;
    }

    @Override
    public boolean equalsStructured(SpaceSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ObjectSpaceSchemaConfiguration))
            return false;

        ObjectSpaceSchemaConfiguration configuration = (ObjectSpaceSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex && fullTextPathIndex == configuration.fullTextPathIndex;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex, fullTextPathIndex);
    }
}
