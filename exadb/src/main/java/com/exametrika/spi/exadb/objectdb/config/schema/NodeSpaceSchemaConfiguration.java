/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;


/**
 * The {@link NodeSpaceSchemaConfiguration} represents an abstract configuration of schema of node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeSpaceSchemaConfiguration extends SpaceSchemaConfiguration {
    private List<NodeSchemaConfiguration> nodes;
    protected final Map<String, NodeSchemaConfiguration> nodesMap;
    private final Map<String, NodeSchemaConfiguration> nodesByAliasMap;
    private final String rootNodeType;
    private final int totalIndexCount;
    private final int totalBlobIndexCount;
    private final boolean hasFullTextIndex;
    protected boolean freezed;

    public NodeSpaceSchemaConfiguration(String name, String alias, String description, Set<? extends NodeSchemaConfiguration> nodes,
                                        String rootNodeType) {
        this(name, alias, description, nodes, rootNodeType, true);
    }

    public NodeSpaceSchemaConfiguration(String name, String alias, String description, Set<? extends NodeSchemaConfiguration> nodes,
                                        String rootNodeType, boolean freezed) {
        super(name, alias, description);

        Assert.notNull(nodes);

        Map<String, NodeSchemaConfiguration> nodesMap = new HashMap<String, NodeSchemaConfiguration>();
        Map<String, NodeSchemaConfiguration> nodesByAliasMap = new HashMap<String, NodeSchemaConfiguration>();
        Set<String> indexes = new HashSet<String>();
        Set<String> blobIndexes = new HashSet<String>();
        boolean hasFullTextIndex = false;
        for (NodeSchemaConfiguration node : nodes) {
            Assert.isNull(nodesMap.put(node.getName(), node));
            Assert.isNull(nodesByAliasMap.put(node.getAlias(), node));

            for (FieldSchemaConfiguration field : node.getFields()) {
                if (field.isIndexed()) {
                    String indexName = field.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName();

                    indexes.add(indexName);
                } else if (field instanceof StructuredBlobFieldSchemaConfiguration && !((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty()) {
                    for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                        String indexName = blobIndex.getIndexName();
                        if (indexName == null)
                            indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                        blobIndexes.add(indexName);
                    }
                }
            }

            if (node.hasFullTextIndex())
                hasFullTextIndex = true;
        }

        this.hasFullTextIndex = hasFullTextIndex;

        if (rootNodeType != null) {
            NodeSchemaConfiguration rootSchema = nodesMap.get(rootNodeType);
            Assert.notNull(rootSchema);
        }

        List<NodeSchemaConfiguration> nodesList = new ArrayList(nodes);
        Collections.sort(nodesList, new Comparator<NodeSchemaConfiguration>() {
            @Override
            public int compare(NodeSchemaConfiguration o1, NodeSchemaConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.freezed = freezed;
        if (freezed)
            this.nodes = Immutables.wrap(nodesList);
        else
            this.nodes = nodesList;
        this.nodesMap = nodesMap;
        this.nodesByAliasMap = nodesByAliasMap;
        this.rootNodeType = rootNodeType;
        this.totalIndexCount = indexes.size();
        this.totalBlobIndexCount = blobIndexes.size();
    }

    public final List<NodeSchemaConfiguration> getNodes() {
        return nodes;
    }

    public final NodeSchemaConfiguration findNode(String name) {
        Assert.notNull(name);

        return nodesMap.get(name);
    }

    public final NodeSchemaConfiguration findNodeByAlias(String alias) {
        Assert.notNull(alias);

        return nodesByAliasMap.get(alias);
    }

    public final String getRootNodeType() {
        return rootNodeType;
    }

    public final int getTotalIndexCount() {
        return totalIndexCount;
    }

    public int getTotalBlobIndexCount() {
        return totalBlobIndexCount;
    }

    public final boolean hasFullTextIndex() {
        return hasFullTextIndex;
    }

    public void addNode(NodeSchemaConfiguration node) {
        Assert.notNull(node);
        Assert.checkState(!freezed);
        Assert.isTrue(findNode(node.getName()) == null);
        Assert.isTrue(findNodeByAlias(node.getAlias()) == null);

        nodes.add(node);
        nodesMap.put(node.getName(), node);
        nodesByAliasMap.put(node.getAlias(), node);
    }

    @Override
    public void orderNodes(SpaceSchemaConfiguration oldSchema) {
        NodeSpaceSchemaConfiguration oldNodeSpaceSchema = (NodeSpaceSchemaConfiguration) oldSchema;
        List<NodeSchemaConfiguration> nodes = Immutables.unwrap(this.nodes);
        List<NodeSchemaConfiguration> sortedNodes = new ArrayList<NodeSchemaConfiguration>(this.nodes);
        nodes.clear();

        for (NodeSchemaConfiguration node : oldNodeSpaceSchema.getNodes())
            nodes.add(nodesMap.get(node.getName()));

        for (NodeSchemaConfiguration node : sortedNodes) {
            if (oldNodeSpaceSchema.findNode(node.getName()) == null)
                nodes.add(node);
        }
    }

    @Override
    public void freeze() {
        if (freezed)
            return;

        freezed = true;
        nodes = Immutables.wrap(nodes);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeSpaceSchemaConfiguration))
            return false;

        NodeSpaceSchemaConfiguration configuration = (NodeSpaceSchemaConfiguration) o;
        return super.equals(configuration) && nodesMap.equals(configuration.nodesMap) && Objects.equals(rootNodeType, configuration.rootNodeType);
    }

    @Override
    public boolean equalsStructured(SpaceSchemaConfiguration newSchema) {
        if (!(newSchema instanceof NodeSpaceSchemaConfiguration))
            return false;

        NodeSpaceSchemaConfiguration configuration = (NodeSpaceSchemaConfiguration) newSchema;

        for (NodeSchemaConfiguration node : nodes) {
            NodeSchemaConfiguration otherNode = configuration.nodesMap.get(node.getName());
            if (otherNode == null || !otherNode.equalsStructured(node))
                return false;
        }

        return super.equalsStructured(configuration) && Objects.equals(rootNodeType, configuration.rootNodeType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(nodesMap, rootNodeType);
    }
}
