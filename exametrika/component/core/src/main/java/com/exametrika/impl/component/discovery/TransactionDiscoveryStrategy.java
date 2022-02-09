/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.component.config.model.TransactionDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.ITransactionComponentVersion;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.NodeComponentNode;
import com.exametrika.impl.component.nodes.TransactionComponentNode;
import com.exametrika.impl.component.nodes.TransactionComponentVersionNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link TransactionDiscoveryStrategy} is a transaction component discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class TransactionDiscoveryStrategy extends BaseComponentDiscoveryStrategy {
    public TransactionDiscoveryStrategy(TransactionDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(configuration, context);
    }

    @Override
    protected void setProperties(ComponentNode component, JsonObject metadata) {
        if (component.getCurrentVersion().getProperties() != null)
            return;

        if (!metadata.contains("title") && metadata.contains("type")) {
            JsonObjectBuilder builder = new JsonObjectBuilder(metadata);
            String type = builder.get("type");
            String title = null;
            if (type.contains("http")) {
                String app = builder.get("app", null);
                if (app != null) {
                    title = app + ":";
                    String url = builder.get("url", null);
                    if (url != null)
                        title += url;
                }
            } else if (type.contains("jms")) {
                String destination = builder.get("destination", null);
                if (destination != null)
                    title = builder.get("destinationType") + ":" + destination;
            } else if (type.contains("method")) {
                String scope = component.getScope().toString();
                int pos = scope.indexOf("method:");
                if (pos != -1)
                    title = scope.substring(pos);
            }
            if (title != null)
                builder.put("title", title);

            metadata = builder.toJson();
        }
        super.setProperties(component, metadata);
    }

    @Override
    protected boolean isDynamic(JsonObject metadata) {
        if (metadata != null)
            return !(Boolean) metadata.get("static", false);
        else
            return true;
    }

    @Override
    protected boolean areReferencesResolved(ComponentNode component) {
        return ((TransactionComponentVersionNode) ((TransactionComponentNode) component).getCurrentVersion()).getPrimaryNode() != null;
    }

    @Override
    protected void resolveReferences(List<ComponentNode> components) {
        IPeriodNameManager nameManager = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
        IObjectSpace space = spaceSchema.getSpace();

        for (ComponentNode component : components) {
            IScopeName scope = component.getScope();
            if (scope.getSegments().size() <= 1)
                continue;

            scope = Names.getScope(scope.getSegments().subList(0, scope.getSegments().size() - 1));

            IPeriodName name = nameManager.findByName(scope);
            if (name == null)
                continue;

            INodeIndex<Long, NodeComponentNode> index = space.getIndex(component.getSchema().getIndexField());
            NodeComponentNode node = index.find(name.getId());
            if (node == null)
                continue;

            node.addTransaction((TransactionComponentNode) component);
            updateMetadata((TransactionComponentNode) component, node);
        }
    }

    private void updateMetadata(TransactionComponentNode transaction, NodeComponentNode node) {
        JsonObject transactionProperties = transaction.getCurrentVersion().getProperties();
        if (transactionProperties == null)
            return;

        String title = transactionProperties.get("title", null);
        if (title == null)
            return;

        JsonObject transactionMetadata = null;
        JsonObject properties = node.getCurrentVersion().getProperties();
        if (properties != null) {
            JsonObject transactionsMetadata = properties.select("nodeProperties?.transactionsMetadata?", null);
            if (transactionsMetadata != null) {
                transactionMetadata = transactionsMetadata.get(title, null);
                if (transactionMetadata == null) {
                    for (Map.Entry<String, Object> entry : transactionsMetadata) {
                        JsonObject metadata = (JsonObject) entry.getValue();
                        String pattern = metadata.get("pattern", null);
                        if (pattern == null)
                            continue;

                        if (Strings.createFilterCondition(pattern, false).evaluate(title)) {
                            transactionMetadata = metadata;
                            break;
                        }
                    }
                }
            }
        }

        JsonObjectBuilder builder = new JsonObjectBuilder(transactionProperties);
        ITransactionComponentVersion version = (ITransactionComponentVersion) transaction.getCurrentVersion();
        INodeComponent transactionNode = version.getPrimaryNode();
        String nodeTitle = transactionNode.getTitle();

        if (transactionMetadata != null) {
            builder.putAll(transactionMetadata);
            builder.remove("pattern");

            String newTitle = builder.get("title", "") + " (" + nodeTitle + ")";
            builder.put("title", newTitle);

            String description = builder.get("description", "") + " (" + title + ")";
            builder.put("description", description);

            transaction.setProperties(builder.toJson());
            if (isDynamic(builder))
                transaction.setDynamic();
            else
                transaction.clearDynamic();

            List<String> tags = builder.get("tags", null);
            if (tags != null)
                transaction.setTags(tags);
        } else {
            String newTitle = builder.get("title", "") + " (" + nodeTitle + ")";
            builder.put("title", newTitle);

            transaction.setProperties(builder.toJson());
        }
    }
}
