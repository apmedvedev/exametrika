/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.component.config.model.PatternGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Strings;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link PatternGroupDiscoveryStrategy} is a pattern group discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class PatternGroupDiscoveryStrategy extends BaseGroupDiscoveryStrategy {
    private final PatternGroupDiscoveryStrategySchemaConfiguration configuration;
    private final ICondition<String> condition;

    public PatternGroupDiscoveryStrategy(PatternGroupDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(context);

        Assert.notNull(configuration);

        this.configuration = configuration;
        this.condition = Strings.createFilterCondition(configuration.getPattern(), true);
    }

    @Override
    protected List<GroupInfo> getGroupInfos(IComponent initialComponent, IComponent childComponent, int level) {
        boolean dynamic = false;
        if (childComponent instanceof IHealthComponent)
            dynamic = ((IHealthComponentVersion) childComponent.getCurrentVersion()).isDynamic();
        JsonObject properties = initialComponent.getCurrentVersion().getProperties();

        List<GroupInfo> infos = new ArrayList<GroupInfo>();
        if (!(childComponent instanceof IGroupComponent)) {
            if (properties != null) {
                Object componentGroups = properties.select("nodeProperties?.groups?", null);
                if (componentGroups instanceof JsonArray) {
                    for (Object object : (JsonArray) componentGroups) {
                        String scopeName = (String) object;
                        if (!condition.evaluate(scopeName))
                            continue;

                        JsonObject metadata = getGroupMetadata(scopeName, properties);
                        infos.add(new GroupInfo(scopeName, configuration.getComponent(),
                                dynamic, metadata, metadata != null ? (List<String>) metadata.get("tags", null) : null));
                    }
                }
            }
        } else {
            String scopeName = getGroupScope(childComponent);
            if (scopeName != null) {
                JsonObject metadata = getGroupMetadata(scopeName, properties);
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

        String scopeName = childScope.substring(0, pos);
        if (condition.evaluate(scopeName))
            return scopeName;
        else
            return null;
    }

    private JsonObject getGroupMetadata(String scopeName, JsonObject properties) {
        if (properties == null)
            return null;

        Object groupsMetadata = properties.select("nodeProperties?.groupsMetadata?", null);
        if (!(groupsMetadata instanceof JsonObject))
            return null;

        return ((JsonObject) groupsMetadata).get(scopeName, null);
    }
}
