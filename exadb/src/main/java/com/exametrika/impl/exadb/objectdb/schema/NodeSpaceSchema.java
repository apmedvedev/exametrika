/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.exadb.core.schema.SchemaObject;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSpaceSchemaConfiguration;


/**
 * The {@link NodeSpaceSchema} represents an abstract schema of node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeSpaceSchema extends SchemaObject implements INodeSpaceSchema {
    public static final String DOCUMENT_TYPE_FIELD_NAME = "exaDocType";
    public static final String NODE_ID_FIELD_NAME = "exaNodeId";
    protected final IDatabaseContext context;
    protected final NodeSpaceSchemaConfiguration configuration;
    private final INodeSchema rootNode;
    private final List<INodeSchema> nodes;
    private final Map<String, INodeSchema> nodesMap;
    private final Map<String, INodeSchema> nodesByAliasMap;
    private final int version;
    private ISchemaObject parent;

    public NodeSpaceSchema(IDatabaseContext context, NodeSpaceSchemaConfiguration configuration, int version, String type) {
        super(type);

        Assert.notNull(context);
        Assert.notNull(configuration);

        List<INodeSchema> nodes = createNodes(configuration);
        INodeSchema rootNode = findRootNode(configuration, nodes);

        Assert.notNull(nodes);
        Assert.isTrue(nodes.size() == configuration.getNodes().size());

        Map<String, INodeSchema> nodesMap = new HashMap<String, INodeSchema>();
        Map<String, INodeSchema> nodesByAliasMap = new HashMap<String, INodeSchema>();
        for (INodeSchema node : nodes) {
            Assert.isNull(nodesMap.put(node.getConfiguration().getName(), node));
            Assert.isNull(nodesByAliasMap.put(node.getConfiguration().getAlias(), node));
        }

        this.context = context;
        this.configuration = configuration;
        this.rootNode = rootNode;
        this.nodes = Immutables.wrap(nodes);
        this.nodesMap = nodesMap;
        this.nodesByAliasMap = nodesByAliasMap;
        this.version = version;
    }

    public IDatabaseContext getContext() {
        return context;
    }

    @Override
    public void setParent(ISchemaObject parent, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(parent);

        this.parent = parent;
        super.setParent(parent, schemaObjects);

        for (INodeSchema node : nodes)
            ((NodeSchema) node).setParent(this, schemaObjects);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        for (INodeSchema node : nodes)
            ((NodeSchema) node).resolveDependencies();
    }

    @Override
    public NodeSpaceSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ISchemaObject getParent() {
        return parent;
    }

    @Override
    public final int getVersion() {
        return version;
    }

    @Override
    public INodeSchema getRootNode() {
        return rootNode;
    }

    @Override
    public final List<INodeSchema> getNodes() {
        return nodes;
    }

    @Override
    public final INodeSchema findNode(String name) {
        Assert.notNull(name);

        return nodesMap.get(name);
    }

    @Override
    public final INodeSchema findNodeByAlias(String alias) {
        Assert.notNull(alias);

        return nodesByAliasMap.get(alias);
    }

    @Override
    public Iterable<ISchemaObject> getChildren() {
        return (Iterable) nodes;
    }

    @Override
    public Iterable<ISchemaObject> getChildren(String type) {
        Assert.notNull(type);

        if (type.equals(INodeSchema.TYPE))
            return (Iterable) nodes;
        else
            return Collections.emptyList();
    }

    @Override
    public <T extends ISchemaObject> T findChild(String type, String name) {
        Assert.notNull(type);
        Assert.notNull(name);

        if (type.equals(INodeSchema.TYPE))
            return (T) nodesMap.get(name);
        else
            return null;
    }

    @Override
    public <T extends ISchemaObject> T findChildByAlias(String type, String alias) {
        Assert.notNull(type);
        Assert.notNull(alias);

        if (type.equals(INodeSchema.TYPE))
            return (T) nodesByAliasMap.get(alias);
        else
            return null;
    }

    protected final List<INodeSchema> createNodes(NodeSpaceSchemaConfiguration configuration) {
        List<INodeSchema> nodes = new ArrayList<INodeSchema>();
        int m = 0, n = 0;
        Map<String, Integer> indexes = new HashMap<String, Integer>();
        Map<String, Integer> blobIndexes = new HashMap<String, Integer>();
        for (int i = 0; i < configuration.getNodes().size(); i++) {
            NodeSchemaConfiguration node = configuration.getNodes().get(i);
            List<IFieldSchema> fields = new ArrayList<IFieldSchema>();
            int offset = Node.HEADER_SIZE;
            for (int k = 0; k < node.getFields().size(); k++) {
                FieldSchemaConfiguration field = node.getFields().get(k);
                int indexTotalIndex;
                if (field.isIndexed()) {
                    String indexName = field.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName();

                    Integer value = indexes.get(indexName);
                    if (value == null) {
                        indexTotalIndex = m;
                        indexes.put(indexName, indexTotalIndex);
                        m++;
                    } else
                        indexTotalIndex = value;
                } else
                    indexTotalIndex = -1;

                IFieldSchema fieldSchema = field.createSchema(k, offset, indexTotalIndex);
                fields.add(fieldSchema);

                if (fieldSchema instanceof StructuredBlobFieldSchema) {
                    int blobIndexTotalIndex = -1;
                    if (field instanceof StructuredBlobFieldSchemaConfiguration && !((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty()) {
                        for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                            String indexName = blobIndex.getIndexName();
                            if (indexName == null)
                                indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                            Integer value = blobIndexes.get(indexName);
                            if (value == null) {
                                blobIndexTotalIndex = n;
                                blobIndexes.put(indexName, blobIndexTotalIndex);
                                n++;
                            } else
                                blobIndexTotalIndex = value;

                            ((StructuredBlobFieldSchema) fieldSchema).addBlobIndexTotalIndex(blobIndexTotalIndex);
                        }
                    }
                }

                offset += field.getSize();
            }

            nodes.add(node.createSchema(i, fields, createDocumentSchema(node, fields)));
        }

        return nodes;
    }

    protected final INodeSchema findRootNode(NodeSpaceSchemaConfiguration configuration, List<INodeSchema> nodes) {
        INodeSchema rootNode = null;
        if (configuration.getRootNodeType() != null) {
            for (INodeSchema node : nodes) {
                if (node.getConfiguration().getName().equals(configuration.getRootNodeType())) {
                    rootNode = node;
                    break;
                }
            }
        }

        return rootNode;
    }

    protected abstract IDocumentSchema createDocumentSchema(NodeSchemaConfiguration configuration, List<IFieldSchema> fields);
}
