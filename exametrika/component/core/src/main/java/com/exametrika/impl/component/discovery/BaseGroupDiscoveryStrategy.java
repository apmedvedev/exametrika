/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.schema.GroupComponentNodeSchema;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link BaseGroupDiscoveryStrategy} is a base group discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BaseGroupDiscoveryStrategy implements IGroupDiscoveryStrategy {
    private final IDatabaseContext context;
    private IObjectSpaceSchema spaceSchema;
    private IPeriodNameManager nameManager;

    public BaseGroupDiscoveryStrategy(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public List<IGroupComponent> getGroups(IComponent initialComponent, IComponent childComponent, int level) {
        ensureSpace();

        IObjectSpace space = spaceSchema.getSpace();

        List<IGroupComponent> groups = new ArrayList<IGroupComponent>();
        for (GroupInfo info : getGroupInfos(initialComponent, childComponent, level)) {
            long scopeId = nameManager.addName(Names.getScope(info.scope)).getId();
            GroupComponentNodeSchema componentSchema = spaceSchema.findNode(info.nodeType);

            INodeIndex<Long, ComponentNode> index = space.getIndex(componentSchema.getIndexField());
            ComponentNode node = index.find(scopeId);
            if (node != null && !(node instanceof GroupComponentNode))
                continue;

            GroupComponentNode group = (GroupComponentNode) node;
            if (group == null || group.getCurrentVersion().isDeleted()) {
                if (group == null) {
                    group = space.createNode(scopeId, componentSchema);
                    if (info.tags != null)
                        group.setTags(info.tags);
                }

                if (info.dynamic)
                    group.setDynamic();

                group.setProperties(info.metadata);

                for (IGroupDiscoveryStrategy strategy : componentSchema.getGroupDiscoveryStrategies()) {
                    List<IGroupComponent> parentGroups = strategy.getGroups(initialComponent, group, level + 1);
                    if (!parentGroups.isEmpty()) {
                        parentGroups.get(0).addChild(group);
                        break;
                    }
                }
            }

            groups.add(group);
        }

        if (!groups.isEmpty() || getDefaultGroup() == null)
            return groups;
        else {
            IGroupComponent group = getDefaultGroup(initialComponent, childComponent);
            if (group != null)
                return Collections.singletonList(group);
            else
                return Collections.emptyList();
        }
    }

    protected abstract List<GroupInfo> getGroupInfos(IComponent initialComponent, IComponent childComponent, int level);

    protected abstract String getDefaultGroup();

    private IGroupComponent getDefaultGroup(IComponent initialComponent, IComponent childComponent) {
        IObjectSpace space = spaceSchema.getSpace();
        INodeIndex<Long, IGroupComponent> index = space.findIndex("componentIndex");

        IPeriodName name = nameManager.findByName(Names.getScope(getDefaultGroup()));
        if (name == null)
            return null;

        return index.find(name.getId());
    }

    private void ensureSpace() {
        if (spaceSchema == null) {
            spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
            nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
        }
    }

    protected static class GroupInfo {
        public final String scope;
        public final String nodeType;
        public final boolean dynamic;
        public final JsonObject metadata;
        public final List<String> tags;

        public GroupInfo(String scope, String nodeType, boolean dynamic, JsonObject metadata, List<String> tags) {
            Assert.notNull(scope);
            Assert.notNull(nodeType);

            this.scope = scope;
            this.nodeType = nodeType;
            this.dynamic = dynamic;
            this.metadata = metadata;
            this.tags = tags;
        }
    }
}
