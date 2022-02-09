/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.component.config.model.TransactionGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.ITransactionComponentVersion;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link TransactionGroupDiscoveryStrategy} is a transaction group discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class TransactionGroupDiscoveryStrategy extends BaseGroupDiscoveryStrategy {
    private final TransactionGroupDiscoveryStrategySchemaConfiguration configuration;

    public TransactionGroupDiscoveryStrategy(TransactionGroupDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(context);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    protected List<GroupInfo> getGroupInfos(IComponent initialComponent, IComponent childComponent, int level) {
        String componentGroup = null;
        boolean dynamic = true;
        ITransactionComponentVersion version = (ITransactionComponentVersion) initialComponent.getCurrentVersion();
        JsonObject properties = version.getProperties();
        if (properties != null) {
            componentGroup = properties.get("group");
            dynamic = !(Boolean) properties.get("static", false);
        }

        JsonObject nodeProperties = null;
        if (version.getPrimaryNode() != null)
            nodeProperties = version.getPrimaryNode().getCurrentVersion().getProperties();

        List<GroupInfo> infos = new ArrayList<GroupInfo>();
        if (!(childComponent instanceof IGroupComponent)) {
            JsonObject metadata = getGroupMetadata(true, componentGroup, nodeProperties);
            infos.add(new GroupInfo(componentGroup, configuration.getComponent(), dynamic,
                    metadata, metadata != null ? (List<String>) metadata.get("tags", null) : null));
        } else {
            String scopeName = getGroupScope(childComponent);
            if (scopeName != null) {
                JsonObject metadata = getGroupMetadata(false, scopeName, nodeProperties);
                infos.add(new GroupInfo(scopeName, configuration.getComponent(), dynamic, metadata,
                        metadata != null ? (List<String>) metadata.get("tags", null) : null));
            }
        }

        return infos;
    }

    @Override
    protected String getDefaultGroup() {
        return configuration.getGroup();
    }

    private String getGroupScope(IComponent childComponent) {
        String childScope = childComponent.getScope().toString();
        int pos = childScope.lastIndexOf('.');
        if (pos == -1)
            return null;

        return childScope.substring(0, pos);
    }

    private JsonObject getGroupMetadata(boolean firstLevel, String groupName, JsonObject nodeProperties) {
        if (nodeProperties != null) {
            Object groupsMetadata = nodeProperties.select("nodeProperties?.groupsMetadata?", null);
            if (groupsMetadata instanceof JsonObject) {
                JsonObject metadata = ((JsonObject) groupsMetadata).get(groupName, null);
                if (metadata != null)
                    return metadata;
            }
        }

        String title = groupName;
        if (title.startsWith("transactions."))
            title = title.substring("transactions.".length());

        if (firstLevel) {
            int pos = title.indexOf(".");
            if (pos != -1)
                title = title.substring(pos + 1);
        }

        return Json.object().put("title", title).toObject();
    }
}
