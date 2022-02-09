/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.IPeriodNodeSchema;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSpaceSchema;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.impl.aggregator.PeriodSpace.NodeIndexInfo;
import com.exametrika.impl.aggregator.cache.PeriodNodeCache;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.index.LocationNodeIndex;
import com.exametrika.impl.aggregator.index.PeriodNodeIndex;
import com.exametrika.impl.aggregator.index.PeriodNodeNonUniqueSortedIndex;
import com.exametrika.impl.aggregator.index.PeriodNodeSortedIndex;
import com.exametrika.impl.aggregator.schema.LocationFieldSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.objectdb.INodeLoader;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.index.NodeIndex;
import com.exametrika.spi.aggregator.IAggregationService;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.NodeObject;


/**
 * The {@link Period} is a period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class Period implements IPeriod, INodeLoader {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1706;
    public static final int HEADER_BLOCK_COUNT = 2;// magic(short) + padding(byte) + closed(byte) + firstNodeBlockIndex(long) + 
    private static final int CLOSED_OFFSET = 3;    //  + lastNodeBlockIndex(long) + rootNodeBlockIndex(long) + startTime(long) + endTime(long)
    private static final int FIRST_NODE_BLOCK_INDEX_OFFSET = 4;
    private static final int LAST_NODE_BLOCK_INDEX_OFFSET = 12;
    private static final int ROOT_NODE_BLOCK_INDEX_OFFSET = 20;
    private static final int END_TIME_OFFSET = 36;
    private final PeriodSpace space;
    private final int periodIndex;
    private final int fileIndex;
    private IRawPage headerPage;
    private final int headerOffset;
    private long startTime;
    private long endTime;
    private NodeIndex[] indexes;
    private LocationNodeIndex[] locationIndexes;
    private int locationIndex = -1;
    private boolean closed;
    private PeriodNode rootNode;

    public Period(PeriodSpace space, int periodIndex, int fileIndex, long periodBlockIndex, boolean create, long startTime) {
        Assert.notNull(space);

        this.space = space;
        this.periodIndex = periodIndex;
        this.fileIndex = fileIndex;
        this.indexes = new NodeIndex[space.getSchema().getConfiguration().getTotalIndexCount()];
        this.locationIndexes = new LocationNodeIndex[space.getSchema().getConfiguration().getTotalLocationIndexCount()];

        if (periodBlockIndex != 0) {
            headerPage = space.getRawTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(periodBlockIndex));
            headerOffset = Constants.pageOffsetByBlockIndex(periodBlockIndex);
        } else {
            headerPage = null;
            headerOffset = 0;
        }

        if (create) {
            this.startTime = startTime;
            writeHeader();
        } else
            readHeader();
    }

    public int getPeriodIndex() {
        return periodIndex;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public long allocateBlocks(int blockCount) {
        return space.allocateBlocks(blockCount);
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean close(ClosePeriodBatchOperation batch, IBatchControl batchControl, long currentTime, boolean schemaChange) {
        Assert.checkState(!closed);

        endTime = currentTime;
        if (endTime < startTime)
            endTime = startTime;

        if (!doClose(batch, batchControl, schemaChange))
            return false;

        space.getNodeManager().flush();

        closed = true;
        headerPage.getWriteRegion().writeByte(headerOffset + CLOSED_OFFSET, (byte) 1);
        headerPage.getWriteRegion().writeLong(headerOffset + END_TIME_OFFSET, endTime);
        return true;
    }

    public void updateNonAggregatingPeriod() {
        AggregationService aggregationService = getSpace().getTransaction().findDomainService(IAggregationService.NAME);
        if (aggregationService == null)
            return;

        aggregationService.updateNonAggregatingPeriod(this);
    }

    public <T> T addNode(Location location, int index) {
        return findOrCreateNode(location, space.getSchema().getNodes().get(index));
    }

    @Override
    public String getType() {
        return getSpace().getSchema().getConfiguration().getName();
    }

    @Override
    public PeriodSpace getSpace() {
        return space;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public <T> T getRootNode() {
        if (rootNode != null && !rootNode.isStale())
            return rootNode.getObject();

        return readRootNode();
    }

    @Override
    public <T> T findNode(T node) {
        Assert.notNull(node);

        INodeObject object = (INodeObject) node;
        PeriodNode periodNode = (PeriodNode) object.getNode();
        IPeriodNodeSchema schema = (IPeriodNodeSchema) getSpace().getSchema().findNode(periodNode.getSchema().getConfiguration().getName());
        if (schema == null)
            return null;

        INodeIndex index = getIndex(schema.getPrimaryField());
        return (T) index.find(periodNode.getLocation());
    }

    @Override
    public <T> T findNodeById(long id) {
        Node node = loadNode(id, null);
        if (node != null)
            return node.getObject();
        else
            return null;
    }

    @Override
    public <T extends INodeIndex> T getIndex(IFieldSchema field) {
        Assert.notNull(field);
        Assert.isTrue(field.getConfiguration().isIndexed());
        Assert.isTrue(field.getParent().getParent() == space.getSchema());
        if (field instanceof LocationFieldSchema)
            return (T) getLocationIndex(((LocationFieldSchema) field).getLocationTotalIndex(), field.getParent().getIndex());
        else
            return (T) getIndex(field.getIndexTotalIndex());
    }

    @Override
    public <T extends INodeIndex> T findIndex(String indexName) {
        Integer value = space.findIndex(indexName);
        if (value == null || value == getLocationIndex())
            return null;

        return (T) getIndex(value);
    }

    @Override
    public <T> T findOrCreateNode(Location location, INodeSchema schema, Object... args) {
        Assert.notNull(location);
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == space.getSchema());
        Assert.checkState(!closed);

        INodeIndex index = getIndex(schema.getPrimaryField());
        T res = (T) index.find(location);
        if (res != null)
            return res;

        return createNode(location, schema.getIndex(), false, args);
    }

    @Override
    public boolean containsNode(Location location, INodeSchema schema) {
        Assert.notNull(location);
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == space.getSchema());

        INodeIndex index = getIndex(schema.getPrimaryField());
        return index.contains(location);
    }

    @Override
    public <T> T findNode(Location location, INodeSchema schema) {
        Assert.notNull(location);
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == space.getSchema());

        INodeIndex index = getIndex(schema.getPrimaryField());
        return (T) index.find(location);
    }

    @Override
    public <T> T createNode(Location location, INodeSchema schema, Object... args) {
        Assert.notNull(location);
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == space.getSchema());
        Assert.checkState(!closed);

        if (containsNode(location, schema)) {
            IPeriodNameManager nameManager = space.getTransaction().findExtension(IPeriodNameManager.NAME);
            IScopeName scope;
            if (location.getScopeId() != 0)
                scope = nameManager.findById(location.getScopeId()).getName();
            else
                scope = ScopeName.root();

            IMetricLocation metric;
            if (location.getMetricId() != 0)
                metric = nameManager.findById(location.getMetricId()).getName();
            else
                metric = MetricName.root();

            Assert.isTrue(false, "Node ''{0}:{1}:{2}'' already exists.", scope, metric, schema.getConfiguration().getName());
        }

        return createNode(location, schema.getIndex(), false, args);
    }

    private <T> T createNode(Location location, int schemaIndex, boolean root, Object... args) {
        long lastNodeBlockIndex = headerPage.getWriteRegion().readLong(headerOffset + LAST_NODE_BLOCK_INDEX_OFFSET);
        PeriodNode node = PeriodNode.create(this, schemaIndex, location, lastNodeBlockIndex, args);
        long nodeBlockIndex = node.getNodeBlockIndex();

        if (lastNodeBlockIndex == 0)
            headerPage.getWriteRegion().writeLong(headerOffset + FIRST_NODE_BLOCK_INDEX_OFFSET, nodeBlockIndex);

        headerPage.getWriteRegion().writeLong(headerOffset + LAST_NODE_BLOCK_INDEX_OFFSET, nodeBlockIndex);

        space.getNodeCache().addNode(node, true);

        INodeObject object = node.getObject();
        object.onCreated(location, args);

        return (T) object;
    }

    @Override
    public <T> Iterable<T> getNodes() {
        return new NodeIterable<T>(0);
    }

    public <T> Iterable<T> getNodes(long startId) {
        return new NodeIterable<T>(startId);
    }

    @Override
    public Node loadNode(long id, NodeObject object) {
        PeriodNodeCache nodeCache = space.getNodeCache();
        PeriodNode node = nodeCache.findById(getFileIndex(), id);
        if (node == null) {
            node = PeriodNode.open(this, id, object);
            nodeCache.addNode(node, false);
        }

        return node;
    }

    public void dump(IJsonHandler json, DumpContext context) {
        context.reset();

        if (context.getQuery() != null && (context.getQuery().contains("componentTypes") ||
                context.getQuery().contains("scopes") || context.getQuery().contains("metrics") ||
                context.getQuery().contains("scopeFilters") || context.getQuery().contains("metricFilters"))) {
            IPeriodNameManager nameManager = space.getTransaction().findExtension(IPeriodNameManager.NAME);

            Set<String> componentTypes = JsonUtils.toSet((JsonArray) context.getQuery().get("componentTypes", null));
            Set<IAggregationNodeSchema> nodeSchemas = null;
            if (componentTypes != null) {
                nodeSchemas = new HashSet<IAggregationNodeSchema>();
                for (String componentType : componentTypes)
                    nodeSchemas.add(getSpace().getSchema().findAggregationNode(componentType));
            }

            Set<Long> scopeIds = null;
            if (context.getQuery().contains("scopes")) {
                scopeIds = new LinkedHashSet<Long>();
                List<String> scopes = JsonUtils.toList((JsonArray) context.getQuery().get("scopes"));
                for (String scopeName : scopes) {
                    IScopeName scope = Names.getScope(scopeName);
                    if (scope.isEmpty())
                        scopeIds.add(0l);
                    else {
                        IPeriodName name = nameManager.findByName(scope);
                        if (name == null)
                            continue;

                        scopeIds.add(name.getId());
                    }
                }
            }

            NameFilter scopeFilter = null;
            if (context.getQuery().contains("scopeFilters"))
                scopeFilter = NameFilter.toFilter(JsonUtils.<String>toList((JsonArray) context.getQuery().get("scopeFilters")), null);

            Set<Long> metricIds = null;
            if (context.getQuery().contains("metrics")) {
                metricIds = new LinkedHashSet<Long>();
                List<String> metrics = JsonUtils.toList((JsonArray) context.getQuery().get("metrics"));
                for (String metricName : metrics) {
                    IMetricName metric = Names.getMetric(metricName);
                    if (metric.isEmpty())
                        metricIds.add(0l);
                    else {
                        IPeriodName name = nameManager.findByName(metric);
                        if (name == null)
                            continue;

                        metricIds.add(name.getId());
                    }
                }
            }

            NameFilter metricFilter = null;
            if (context.getQuery().contains("metricFilters"))
                metricFilter = NameFilter.toFilter(JsonUtils.<String>toList((JsonArray) context.getQuery().get("metricFilters")), null);

            if (nodeSchemas != null && scopeIds != null && metricIds != null) {
                for (IAggregationNodeSchema nodeSchema : nodeSchemas) {
                    for (long scopeId : scopeIds) {
                        for (long metricId : metricIds) {
                            INodeObject node = (INodeObject) getIndex(nodeSchema.getPrimaryField()).find(new Location(scopeId, metricId));
                            if (node != null) {
                                json.key("<node>");
                                json.startObject();
                                node.dump(json, context);
                                json.endObject();
                            }
                        }
                    }
                }
            } else {
                json.key("<nodes>");
                json.startArray();
                for (INodeObject node : this.<INodeObject>getNodes()) {
                    IPeriodNode periodNode = (IPeriodNode) node.getNode();
                    if ((nodeSchemas == null || nodeSchemas.contains(periodNode.getSchema())) &&
                            (scopeIds == null || scopeIds.contains(periodNode.getLocation().getScopeId())) &&
                            (scopeFilter == null || scopeFilter.match(periodNode.getScope().toString())) &&
                            (metricIds == null || metricIds.contains(periodNode.getLocation().getMetricId())) &&
                            (metricFilter == null || metricFilter.match(periodNode.getMetric().toString())) &&
                            !context.isNodeTraversed(node.getNode().getId())) {
                        json.startObject();
                        node.dump(json, context);
                        json.endObject();
                    }
                }
                json.endArray();
            }
        } else {
            INodeObject root = getRootNode();
            if (root != null) {
                json.key("<root>");
                json.startObject();
                root.dump(json, context);
                json.endObject();
            }

            if ((context.getFlags() & IDumpContext.DUMP_ORPHANED) != 0) {
                json.key("<orphaned>");
                json.startArray();
                for (INodeObject node : this.<INodeObject>getNodes()) {
                    if (!context.isNodeTraversed(node.getNode().getId())) {
                        json.startObject();
                        node.dump(json, context);
                        json.endObject();
                    }
                }
                json.endArray();
            }
        }
    }

    public void createRootNode() {
        INodeSchema rootNodeSchema = getRootNodeSchema();
        if (rootNodeSchema != null) {
            PeriodNode rootNode = (PeriodNode) ((INodeObject) createNode(new Location(0, 0), rootNodeSchema.getIndex(), true)).getNode();
            headerPage.getWriteRegion().writeLong(headerOffset + ROOT_NODE_BLOCK_INDEX_OFFSET, rootNode.getNodeBlockIndex());

            if (rootNode.isCached())
                this.rootNode = rootNode;
        }
    }

    @Override
    public String toString() {
        return getSpace().toString() + "[" + periodIndex + "]";
    }

    protected INodeSchema getRootNodeSchema() {
        INodeSpaceSchema schema = space.getSchema();
        return schema.getRootNode();
    }

    protected boolean doClose(ClosePeriodBatchOperation batch, IBatchControl batchControl, boolean schemaChange) {
        if (space.getSchema().getConfiguration().isNonAggregating())
            return true;

        AggregationService aggregationService = space.getTransaction().findDomainService(IAggregationService.NAME);
        if (aggregationService == null)
            return true;

        return aggregationService.closePeriod(batch, batchControl, this, schemaChange);
    }

    private void readHeader() {
        if (headerPage == null) {
            closed = true;
            return;
        }

        RawPageDeserialization deserialization = new RawPageDeserialization(space.getRawTransaction(), getFileIndex(),
                headerPage, headerOffset);

        short magic = deserialization.readShort();
        deserialization.readByte();
        closed = deserialization.readBoolean();
        deserialization.readLong(); // firstNodeBlockIndex
        deserialization.readLong(); // lastNodeBlockIndex
        deserialization.readLong(); // rootNodeBlockIndex
        startTime = deserialization.readLong();
        endTime = deserialization.readLong();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
    }

    private void writeHeader() {
        closed = false;
        endTime = 0;

        RawPageSerialization serialization = new RawPageSerialization(space.getRawTransaction(), getFileIndex(),
                headerPage, headerOffset);

        serialization.writeShort(MAGIC);
        serialization.writeByte((byte) 0);
        serialization.writeBoolean(closed);
        serialization.writeLong(0); // firstNodeBlockIndex
        serialization.writeLong(0); // lastNodeBlockIndex 
        serialization.writeLong(0); // rootNodeBlockIndex
        serialization.writeLong(startTime);
        serialization.writeLong(endTime);
    }

    private <T> T readRootNode() {
        long rootNodeBlockIndex = headerPage.getReadRegion().readLong(headerOffset + ROOT_NODE_BLOCK_INDEX_OFFSET);
        if (rootNodeBlockIndex == 0)
            return null;

        PeriodNodeCache nodeCache = space.getNodeCache();
        rootNode = nodeCache.findById(getFileIndex(), rootNodeBlockIndex);
        if (rootNode == null) {
            PeriodNode rootNode = PeriodNode.open(this, rootNodeBlockIndex, null);
            nodeCache.addNode(rootNode, false);

            if (rootNode.isCached())
                this.rootNode = rootNode;
            else
                return rootNode.getObject();
        }
        return rootNode.getObject();
    }

    private NodeIndex createNodeIndex(IUniqueIndex index, boolean sorted, boolean cached, int i) {
        if (sorted) {
            if (index instanceof INonUniqueSortedIndex)
                return new PeriodNodeNonUniqueSortedIndex(space.getSchema().getContext(), (INonUniqueSortedIndex) index, this, i);
            else if (index instanceof ISortedIndex)
                return new PeriodNodeSortedIndex(space.getSchema().getContext(), (ISortedIndex) index, this, i);
            else
                return Assert.error();
        } else
            return new PeriodNodeIndex(space.getSchema().getContext(), index, cached, this, i);
    }

    private NodeIndex getIndex(int i) {
        if (indexes[i] == null) {
            NodeIndexInfo info = space.getIndex(i);
            indexes[i] = createNodeIndex(info.index, info.sorted, info.cached, i);
        }

        return indexes[i];
    }

    private LocationNodeIndex getLocationIndex(int locationTotalIndex, int nodeSchemaIndex) {
        if (locationIndexes[locationTotalIndex] == null) {
            int locationIndex = getLocationIndex();
            NodeIndexInfo info = space.getIndex(locationIndex);
            locationIndexes[locationTotalIndex] = new LocationNodeIndex(space.getSchema().getContext(), info.index, this, locationIndex, nodeSchemaIndex);
        }

        return locationIndexes[locationTotalIndex];
    }

    private int getLocationIndex() {
        if (locationIndex == -1)
            locationIndex = space.findIndex("locationIndex");
        return locationIndex;
    }

    private class NodeIterable<T> implements Iterable<T> {
        private final long nodeBlockIndex;

        public NodeIterable(long nodeBlockIndex) {
            this.nodeBlockIndex = nodeBlockIndex;
        }

        @Override
        public Iterator<T> iterator() {
            if (headerPage != null) {
                long lastNodeBlockIndex;
                if (nodeBlockIndex == 0)
                    lastNodeBlockIndex = headerPage.getReadRegion().readLong(headerOffset + FIRST_NODE_BLOCK_INDEX_OFFSET);
                else
                    lastNodeBlockIndex = nodeBlockIndex;

                if (lastNodeBlockIndex != 0) {
                    PeriodNodeCache nodeCache = space.getNodeCache();
                    PeriodNode node = nodeCache.findById(getFileIndex(), lastNodeBlockIndex);
                    if (node == null) {
                        node = PeriodNode.open(Period.this, lastNodeBlockIndex, null);
                        nodeCache.addNode(node, false);
                    }

                    return new NodeIterator<T>(node);
                }
            }

            return new NodeIterator<T>(null);
        }
    }

    private class NodeIterator<T> implements Iterator<T> {
        private PeriodNode currentNode;

        public NodeIterator(PeriodNode currentNode) {
            this.currentNode = currentNode;
        }

        @Override
        public boolean hasNext() {
            return currentNode != null;
        }

        @Override
        public T next() {
            Assert.notNull(currentNode);

            T res = currentNode.getObject();

            if (currentNode.getNextNodeBlockIndex() != 0) {
                long nodeBlockIndex = currentNode.getNextNodeBlockIndex();
                PeriodNodeCache nodeCache = space.getNodeCache();
                currentNode = nodeCache.findById(getFileIndex(), nodeBlockIndex);
                if (currentNode == null) {
                    currentNode = PeriodNode.open(Period.this, nodeBlockIndex, null);
                    nodeCache.addNode(currentNode, false);
                }
            } else
                currentNode = null;

            return res;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
