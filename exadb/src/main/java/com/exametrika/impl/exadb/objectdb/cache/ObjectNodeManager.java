/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ObjectNodeManager} is a manager of object nodes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectNodeManager extends NodeManager {
    private Map<FreeKey, SimpleList<ObjectNode>> freeNodeMap = new HashMap<FreeKey, SimpleList<ObjectNode>>();
    private int freeNodeCount;
    private final SimpleList<ObjectNode> freeNodes = new SimpleList<ObjectNode>();
    private volatile ObjectDatabaseExtensionConfiguration configuration;

    public ObjectNodeManager(IDatabaseContext context,
                             ITimeService timeService, ObjectDatabaseExtensionConfiguration configuration) {
        super(context, timeService);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public void setConfiguration(ObjectDatabaseExtensionConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public ObjectNode findFreeNode(int fileIndex, int schemaIndex) {
        SimpleList<ObjectNode> list = freeNodeMap.get(new FreeKey(fileIndex, schemaIndex));
        if (list == null || list.isEmpty())
            return null;

        ObjectNode node = list.getFirst().getValue();
        node.getElement().remove();
        node.getElement().reset();
        freeNodeCount--;

        node.removeDeletedElement();

        return node;
    }

    public void onNodeDeleted(ObjectNode node) {
        node.getSpace().getNodeCache().removeNode(node);
        node.setLastAccessTime((int) (timeService.getCurrentTime() >>> 13));

        Element element = node.getElement();
        Assert.isTrue(element.isAttached() && !element.isRemoved());
        element.remove();
        element.reset();

        freeNodeCount++;
        freeNodes.addLast(element);

        if (freeNodeCount > configuration.getMaxFreeNodeCacheSize())
            unloadFreeNodes(false);

        FreeKey key = new FreeKey(node.getFileIndex(), node.getSchema().getIndex());
        SimpleList<ObjectNode> list = freeNodeMap.get(key);
        if (list == null) {
            list = new SimpleList<ObjectNode>();
            freeNodeMap.put(key, list);
        }

        list.addFirst(node.getDeletedElement());
    }

    @Override
    public void onTransactionRolledBack() {
        if (!freeNodes.isEmpty()) {
            for (Iterator<Element<ObjectNode>> it = freeNodes.iterator(); it.hasNext(); ) {
                ObjectNode node = it.next().getValue();
                it.remove();

                node.setStale();
            }

            freeNodeCount = 0;
            freeNodeMap.clear();
        }

        super.onTransactionRolledBack();
    }

    @Override
    public void clear(boolean full) {
        super.clear(full);

        unloadFreeNodes(full);
    }

    public void unloadFreeNodesOfDeletedSpaces(Set<? extends NodeSpace> spaces) {
        for (Iterator<Element<ObjectNode>> it = freeNodes.iterator(); it.hasNext(); ) {
            ObjectNode node = it.next().getValue();
            if (spaces.contains(node.getSpace())) {
                freeNodeCount--;
                it.remove();

                node.setStale();
            }
        }
    }

    private void unloadFreeNodes(boolean removeAll) {
        if (freeNodes.isEmpty())
            return;

        int currentTime = (int) (timeService.getCurrentTime() >>> 13);
        int maxNodeIdlePeriod = (int) (configuration.getMaxFreeNodeIdlePeriod() >>> 13);

        ObjectNode node = freeNodes.getFirst().getValue();
        if (!removeAll && currentTime - node.getLastAccessTime() <= maxNodeIdlePeriod &&
                freeNodeCount <= configuration.getMaxFreeNodeCacheSize())
            return;

        for (Iterator<Element<ObjectNode>> it = freeNodes.iterator(); it.hasNext(); ) {
            node = it.next().getValue();
            if (removeAll || currentTime - node.getLastAccessTime() > maxNodeIdlePeriod ||
                    freeNodeCount > configuration.getMaxFreeNodeCacheSize()) {
                freeNodeCount--;
                it.remove();

                node.setStale();
            } else
                break;
        }

        if (removeAll)
            freeNodeMap = new HashMap<FreeKey, SimpleList<ObjectNode>>();
    }

    private static class FreeKey {
        private final int fileIndex;
        private final int nodeSchemaIndex;
        private final int hashCode;

        public FreeKey(int fileIndex, int nodeSchemaIndex) {
            this.fileIndex = fileIndex;
            this.nodeSchemaIndex = nodeSchemaIndex;
            this.hashCode = 31 * fileIndex + nodeSchemaIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof FreeKey))
                return false;

            FreeKey k = (FreeKey) o;
            return fileIndex == k.fileIndex && nodeSchemaIndex == k.nodeSchemaIndex;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
