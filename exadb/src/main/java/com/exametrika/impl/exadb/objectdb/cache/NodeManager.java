/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import java.util.Iterator;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link NodeManager} is a manager of nodes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class NodeManager implements ICacheControl {
    private final IDatabaseContext context;
    protected NodeCacheManager nodeCacheManager;
    protected final ITransactionProvider transactionProvider;
    protected final ITimeService timeService;
    private final SimpleList<Node> writeNodes = new SimpleList<Node>();
    private final SimpleList<Node> committedNodes = new SimpleList<Node>();
    protected boolean bigTransaction;
    private boolean cachingEnabled = true;

    public NodeManager(IDatabaseContext context, ITimeService timeService) {
        Assert.notNull(context);
        Assert.notNull(timeService);

        this.context = context;
        this.transactionProvider = context.getTransactionProvider();
        this.timeService = timeService;
    }

    public IDatabaseContext getContext() {
        return context;
    }

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public void setNodeCacheManager(NodeCacheManager nodeCacheManager) {
        Assert.notNull(nodeCacheManager);
        Assert.checkState(this.nodeCacheManager == null);

        this.nodeCacheManager = nodeCacheManager;
    }

    public void onNodeModified(Node node) {
        Assert.isTrue(!node.isModified());

        writeNodes.addLast(node.getWriteElement());
    }

    public void onNodeNew(Node node) {
        writeNodes.addLast(node.getWriteElement());
    }

    public boolean hasCommitted() {
        return !committedNodes.isEmpty();
    }

    public void flush() {
        if (!writeNodes.isEmpty())
            bigTransaction = true;

        flush(true);
    }

    @Override
    public void validate() {
        for (Node node : writeNodes.values())
            node.validate();
    }

    @Override
    public void onTransactionStarted() {
        int options = transactionProvider.getTransaction().getOptions();
        if ((options & IOperation.FLUSH) != 0 || (options & IOperation.DURABLE) != 0)
            flush(true);
    }

    @Override
    public void onTransactionCommitted() {
        int options = transactionProvider.getTransaction().getOptions();
        if ((options & IOperation.FLUSH) != 0 || (options & IOperation.DURABLE) != 0 ||
                (options & IOperation.DELAYED_FLUSH) == 0 || bigTransaction) {
            if (!writeNodes.isEmpty())
                flush(true);
        } else {
            for (Node node : writeNodes.values()) {
                node.clearModified();
                if (!node.isUncommitted())
                    committedNodes.addLast(node.getCommittedElement());
            }

            writeNodes.clear();
        }

        bigTransaction = false;
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        return false;
    }

    @Override
    public void onTransactionRolledBack() {
        if (!committedNodes.isEmpty()) {
            for (Iterator<Element<Node>> it = committedNodes.iterator(); it.hasNext(); ) {
                Node node = it.next().getValue();
                if (!node.isModified()) {
                    it.remove();
                    node.getElement().remove();

                    node.getSpace().getNodeCache().removeNode(node);
                    node.setStale();
                }
            }
        }

        if (!writeNodes.isEmpty()) {
            for (Iterator<Element<Node>> it = writeNodes.iterator(); it.hasNext(); ) {
                Node node = it.next().getValue();
                it.remove();
                node.getElement().remove();

                node.getSpace().getNodeCache().removeNode(node);
                node.setStale();
            }
        }

        if (bigTransaction) {
            nodeCacheManager.unloadNodes(true);
            bigTransaction = false;
        }
    }

    @Override
    public void flush(boolean full) {
        flushCommitted();

        if (!writeNodes.isEmpty()) {
            for (Node node : writeNodes.values())
                node.flush();

            writeNodes.clear();
        }
    }

    @Override
    public void clear(boolean full) {
        if (full)
            nodeCacheManager.close();
        else
            nodeCacheManager.unloadNodes(false);
    }

    @Override
    public void unloadExcessive() {
        nodeCacheManager.unloadExcessive();
    }

    @Override
    public void setCachingEnabled(boolean value) {
        cachingEnabled = value;
    }

    @Override
    public void setMaxCacheSize(String category, long value) {
        NodeCache nodeCache = nodeCacheManager.getExistingNodeCache(category);
        if (nodeCache != null)
            nodeCache.setBatchMaxCacheSize(value);
    }

    public void flushCommitted() {
        if (!committedNodes.isEmpty()) {
            for (Node node : committedNodes.values()) {
                if (!node.isModified())
                    node.flush();
            }

            committedNodes.clear();
        }
    }

    public void clearCommitted() {
        committedNodes.clear();
    }

    public void setBigTransaction() {
        bigTransaction = true;
    }
}
