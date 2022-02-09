/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.Iterator;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IPeriodNodeSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.impl.aggregator.cache.PeriodNodeCache;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.INodeLoader;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.cache.NodeObjectCache;
import com.exametrika.impl.exadb.objectdb.fields.BodyField;
import com.exametrika.impl.exadb.objectdb.fields.ComplexField;
import com.exametrika.impl.exadb.objectdb.fields.SimpleField;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.NodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.INodeBody;


/**
 * The {@link PeriodNode} is a periodic node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNode extends Node implements IPeriodNode {
    private static final int NODE_CACHE_SIZE = getNodeCacheSize();
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int MAGIC = 0x17 << 24;
    private static final int MAGIC_MASK = 0xFF << 24;
    private static final int SCHEMA_INDEX_MASK = 0xFFFFFF;
    private static final int SCOPE_ID_OFFSET = 4;
    private static final int METRIC_ID_OFFSET = 12;
    private static final int NEXT_NODE_BLOCK_INDEX_OFFSET = 20;
    private final Period period;
    private final Location location;// magic(byte) + schemaIndex(3 byte) + scopeId(long) + metricId(long) + nextNodeBlockIndex(long)
    private IPeriodName scope;
    private IPeriodName metric;
    private INodeObject prevPeriodNode;

    public static PeriodNode create(Period period, int schemaIndex, Location location, long prevNodeBlockIndex, Object[] args) {
        Assert.notNull(location);

        PeriodSpace space = period.getSpace();
        INodeSchema nodeSchema = space.getSchema().getNodes().get(schemaIndex);
        long nodeBlockIndex = period.allocateBlocks(Constants.blockCount(nodeSchema.getConfiguration().getSize()));
        IRawPage headerPage = space.getRawTransaction().getPage(period.getFileIndex(), Constants.pageIndexByBlockIndex(nodeBlockIndex));
        PeriodNode node = new PeriodNode(period, nodeBlockIndex, headerPage, nodeSchema, location);

        node.flags |= FLAG_NEW | FLAG_MODIFIED;

        node.writeHeader(prevNodeBlockIndex, location, args);

        return node;
    }

    public static PeriodNode open(Period period, long nodeBlockIndex, NodeObject object) {
        PeriodSpace space = period.getSpace();

        return readInstance(space, period, nodeBlockIndex, object);
    }

    @Override
    public int getFileIndex() {
        return period.getFileIndex();
    }

    @Override
    public String toString() {
        String location = "scope:" + getScope().toString() + ", location:" + getMetric().toString();
        return location + " (" + getId() + "@" + period.toString() + ")";
    }

    @Override
    public IPeriodNodeSchema getSchema() {
        return (IPeriodNodeSchema) super.getSchema();
    }

    public long getNextNodeBlockIndex() {
        return getHeaderPage().getReadRegion().readLong(getHeaderOffset() + NEXT_NODE_BLOCK_INDEX_OFFSET);
    }

    @Override
    public int getDeletionCount() {
        return 0;
    }

    @Override
    public boolean allowDeletion() {
        return false;
    }

    @Override
    public boolean allowFieldDeletion() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return super.isReadOnly() || period.isClosed();
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public void delete() {
        Assert.supports(false);
    }

    @Override
    public PeriodSpace getSpace() {
        return (PeriodSpace) space;
    }

    @Override
    public Period getPeriod() {
        return period;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public IScopeName getScope() {
        if (scope != null && !scope.isStale())
            return scope.getName();
        else
            return refreshScope();
    }

    @Override
    public IMetricLocation getMetric() {
        if (metric != null && !metric.isStale())
            return metric.getName();
        else
            return refreshMetric();
    }

    @Override
    public <T> T getPreviousPeriodNode() {
        if (prevPeriodNode != null && !prevPeriodNode.isStale())
            return (T) prevPeriodNode;
        else {
            Iterator<Pair<IPeriod, Object>> it = getPeriodNodes().iterator();
            it.next();
            if (it.hasNext())
                return (T) it.next().getValue();
            else
                return null;
        }
    }

    @Override
    public Iterable<Pair<IPeriod, Object>> getPeriodNodes() {
        return new PeriodNodeIterable();
    }

    @Override
    public long allocateArea(IRawPage preferredPage) {
        return period.allocateBlocks(Constants.COMPLEX_FIELD_AREA_BLOCK_COUNT + 1);
    }

    @Override
    public void freeArea(IRawPage page, int pageOffset) {
        Assert.supports(false);
    }

    @Override
    public int allocateFile() {
        return period.getSpace().getSchema().getContext().getSchemaSpace().allocateFile(space.getRawTransaction());
    }

    @Override
    public long getRefId() {
        if (period.getPeriodIndex() == CyclePeriod.PERIOD_INDEX)
            return -nodeBlockIndex;
        else
            return nodeBlockIndex;
    }

    @Override
    public PeriodNode open(long refId) {
        PeriodNodeCache nodeCache = period.getSpace().getNodeCache();
        long nodeBlockIndex;
        Period period;
        if (refId > 0) {
            nodeBlockIndex = refId;
            period = this.period;
        } else {
            nodeBlockIndex = -refId;
            period = this.period.getSpace().getCyclePeriod();
        }

        PeriodNode node = nodeCache.findById(period.getFileIndex(), nodeBlockIndex);
        if (node == null) {
            node = PeriodNode.open(period, nodeBlockIndex, null);
            nodeCache.addNode(node, false);
        }

        return node;
    }

    @Override
    public boolean canReference(IReferenceFieldSchema fieldSchema, Node node) {
        if (fieldSchema.getExternalSpaceSchema() == null)
            return ((PeriodNode) node).getPeriod() == period || ((PeriodNode) node).getPeriod() == getSpace().getCyclePeriod();
        else
            return node.getSchema().getParent() == fieldSchema.getExternalSpaceSchema();
    }

    @Override
    public <T> T getRootNode() {
        return period.getRootNode();
    }

    @Override
    public INodeLoader getNodeLoader() {
        return period;
    }

    private PeriodNode(Period period, long nodeBlockIndex, IRawPage headerPage, INodeSchema schema, Location location) {
        super(period.getSpace(), schema, nodeBlockIndex, headerPage, NODE_CACHE_SIZE);

        this.period = period;
        this.location = location;
    }

    private static PeriodNode readInstance(PeriodSpace space, Period period, long nodeBlockIndex, NodeObject object) {
        IRawPage headerPage = space.getRawTransaction().getPage(period.getFileIndex(), Constants.pageIndexByBlockIndex(nodeBlockIndex));
        IRawReadRegion region = headerPage.getReadRegion();
        int pageOffset = Constants.pageOffsetByBlockIndex(nodeBlockIndex);

        int value = region.readInt(pageOffset);
        if ((value & MAGIC_MASK) != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(period.getFileIndex()));

        int schemaIndex = value & SCHEMA_INDEX_MASK;
        long scopeId = region.readLong(pageOffset + SCOPE_ID_OFFSET);
        long metricId = region.readLong(pageOffset + METRIC_ID_OFFSET);
        // skip prevNodeBlockIndex
        INodeSchema schema = space.getSchema().getNodes().get(schemaIndex);

        PeriodNode node = new PeriodNode(period, nodeBlockIndex, headerPage, schema, new Location(scopeId, metricId));
        if (schema.getBodyField() != null) {
            BodyField field = node.getField(schema.getBodyField().getIndex());
            INodeBody body = field.readValue();
            body.setField(field);
            node.object = body;
        } else if (object != null)
            node.object = object;
        else
            node.object = node.createNodeObject(schema);

        node.object.onOpened();

        return node;
    }

    private INodeObject createNodeObject(INodeSchema schema) {
        PeriodSpace space = period.getSpace();
        INodeObject object;
        if (space.getNodeManager().isCachingEnabled()) {
            NodeObjectCache objectCache = space.getNodeObjectCache();

            long id = getRefId();
            object = objectCache.get(id);
            if (object != null) {
                ((NodeObject) object).init(this);
                return object;
            }

            object = schema.getConfiguration().createNode(this);
            if (object instanceof NodeObject)
                objectCache.put(id, (NodeObject) object);
        } else {
            setNonCached();
            object = schema.getConfiguration().createNode(this);
        }

        return object;
    }

    private void writeHeader(long prevNodeBlockIndex, Location location, Object[] args) {
        IRawWriteRegion region = getHeaderPage().getWriteRegion();

        int headerOffset = getHeaderOffset();
        region.writeInt(headerOffset, MAGIC | schema.getIndex());
        region.writeLong(headerOffset + SCOPE_ID_OFFSET, location.getScopeId());
        region.writeLong(headerOffset + METRIC_ID_OFFSET, location.getMetricId());
        region.writeLong(headerOffset + NEXT_NODE_BLOCK_INDEX_OFFSET, 0);

        if (prevNodeBlockIndex != 0) {
            IRawPage prevNodePage = space.getRawTransaction().getPage(getFileIndex(), Constants.pageIndexByBlockIndex(prevNodeBlockIndex));
            int prevNodeHeaderOffset = Constants.pageOffsetByBlockIndex(prevNodeBlockIndex);
            IRawWriteRegion prevNodeRegion = prevNodePage.getWriteRegion();
            prevNodeRegion.writeLong(prevNodeHeaderOffset + NEXT_NODE_BLOCK_INDEX_OFFSET, nodeBlockIndex);
        }

        this.fields = new IField[schema.getFields().size()];

        Object[] fieldInitializers = new Object[fields.length];
        for (int i = 0; i < fields.length; i++)
            fieldInitializers[i] = schema.getFields().get(i).getConfiguration().createInitializer();

        object = schema.getConfiguration().createNode(this);
        if (object instanceof NodeObject) {
            NodeObjectCache objectCache = period.getSpace().getNodeObjectCache();
            objectCache.put(getRefId(), (NodeObject) object);
        }

        object.onBeforeCreated(null, args, fieldInitializers);

        for (int i = 0; i < fields.length; i++) {
            FieldSchemaConfiguration fieldConfiguration = schema.getFields().get(i).getConfiguration();
            if (fieldConfiguration instanceof IndexedLocationFieldSchemaConfiguration)
                fields[i] = SimpleField.create(this, i, location, fieldInitializers[i]);
            else if (fieldConfiguration instanceof SimpleFieldSchemaConfiguration)
                fields[i] = SimpleField.create(this, i, null, fieldInitializers[i]);
            else if (fieldConfiguration instanceof ComplexFieldSchemaConfiguration)
                fields[i] = ComplexField.create(this, i, null, fieldInitializers[i]);
            else
                Assert.error();
        }

        for (int i = 0; i < fields.length; i++)
            ((IFieldObject) fields[i]).onAfterCreated(location, fieldInitializers[i]);

        if (schema.getBodyField() != null) {
            BodyField field = getField(schema.getBodyField().getIndex());
            ((INodeBody) object).setField(field);
        }
    }

    private IScopeName refreshScope() {
        if (location.getScopeId() == 0)
            return ScopeName.root();

        IPeriodNameManager nameManager = getTransaction().findExtension(IPeriodNameManager.NAME);
        scope = nameManager.findById(location.getScopeId());
        if (scope != null)
            return scope.getName();
        else
            return null;
    }

    private IMetricLocation refreshMetric() {
        if (location.getMetricId() == 0) {
            if (((PeriodNodeSchemaConfiguration) schema.getConfiguration()).isStack())
                return CallPath.root();
            else
                return MetricName.root();
        }

        IPeriodNameManager nameManager = getTransaction().findExtension(IPeriodNameManager.NAME);
        metric = nameManager.findById(location.getMetricId());
        if (metric != null)
            return metric.getName();
        else
            return null;
    }

    private static int getNodeCacheSize() {
        return Memory.getShallowSize(PeriodNode.class) + 3 * Memory.getShallowSize(SimpleList.Element.class) +
                Memory.getShallowSize(Location.class);
    }

    private class PeriodNodeIterable implements Iterable<Pair<IPeriod, Object>> {
        @Override
        public Iterator<Pair<IPeriod, Object>> iterator() {
            return new PeriodNodeIterator(new Pair<IPeriod, PeriodNode>(period, PeriodNode.this));
        }
    }

    private class PeriodNodeIterator implements Iterator<Pair<IPeriod, Object>> {
        private Pair<IPeriod, PeriodNode> current;

        public PeriodNodeIterator(Pair<IPeriod, PeriodNode> current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Pair<IPeriod, Object> next() {
            Assert.notNull(current);

            Pair<IPeriod, PeriodNode> res = current;
            if (current.getValue() != null) {
                INodeObject nodeObject = current.getValue().prevPeriodNode;

                if (nodeObject != null && !nodeObject.isStale()) {
                    PeriodNode prevPeriodNode = (PeriodNode) nodeObject.getNode();
                    current = new Pair<IPeriod, PeriodNode>(prevPeriodNode.getPeriod(), prevPeriodNode);
                    return new Pair<IPeriod, Object>(res.getKey(), res.getValue() != null ? res.getValue().getObject() : null);
                }
            }

            Period period = (Period) current.getKey();
            int periodIndex = period.getPeriodIndex();
            if (periodIndex > 0) {
                period = period.getSpace().getPeriod(periodIndex - 1);

                INodeObject nodeObject = period.findNode(PeriodNode.this.object);
                PeriodNode prevNode;
                if (nodeObject != null)
                    prevNode = (PeriodNode) nodeObject.getNode();
                else
                    prevNode = null;

                if (current.getValue() != null)
                    current.getValue().prevPeriodNode = nodeObject;

                current = new Pair<IPeriod, PeriodNode>(period, prevNode);
            } else {
                PeriodCycle cycle = period.getSpace().getPreviousCycle();
                PeriodSpace space = cycle != null ? cycle.getSpace() : null;

                if (space != null) {
                    period = null;
                    if (periodIndex == CyclePeriod.PERIOD_INDEX)
                        period = space.getPeriod(periodIndex);
                    else if (space.getPeriodsCount() > 0)
                        period = space.getPeriod(space.getPeriodsCount() - 1);

                    if (period != null) {
                        INodeObject nodeObject = period.findNode(PeriodNode.this.object);
                        PeriodNode prevNode;
                        if (nodeObject != null)
                            prevNode = (PeriodNode) nodeObject.getNode();
                        else
                            prevNode = null;

                        if (current.getValue() != null)
                            current.getValue().prevPeriodNode = nodeObject;

                        current = new Pair<IPeriod, PeriodNode>(period, prevNode);
                    } else
                        current = null;
                } else
                    current = null;
            }

            return new Pair<IPeriod, Object>(res.getKey(), res.getValue() != null ? res.getValue().getObject() : null);
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
