/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.objectdb.cache.NodeObjectCache;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCache;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCacheManager;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeManager;
import com.exametrika.impl.exadb.objectdb.index.NodeIndex;
import com.exametrika.impl.exadb.objectdb.index.ObjectNodeFullTextIndex;
import com.exametrika.impl.exadb.objectdb.index.ObjectNodeIndex;
import com.exametrika.impl.exadb.objectdb.index.ObjectNodeNonUniqueSortedIndex;
import com.exametrika.impl.exadb.objectdb.index.ObjectNodeSortedIndex;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.NodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link ObjectSpace} is a object space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectSpace extends NodeSpace implements IObjectSpace, IFullTextDocumentSpace, INodeLoader {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int HEADER_SIZE = 36;
    private static final short MAGIC = 0x1707;// magic(short) + version(byte) + closed(byte) + nextBlockIndex(long) + lastNodeBlockIndex(long) +
    private static final int CLOSED_OFFSET = 3;// + lastFreeAreaPageIndex(long) + rootNodeBlockIndex(long) + freeNodeBlockIndex[long * nodeSchemaCount]
    private static final int NEXT_BLOCK_INDEX_OFFSET = 4;
    private static final int LAST_NODE_BLOCK_INDEX_OFFSET = 12;
    private static final int LAST_FREE_AREA_PAGE_INDEX_OFFSET = 20;
    private static final int ROOT_NODE_BLOCK_INDEX_OFFSET = 28;
    private static final int PAGE_HEADER_SIZE = Constants.BLOCK_SIZE;
    private static final int PAGE_HEADER_BLOCK_COUNT = 1;
    private static final short PAGE_MAGIC = 0x1708;// pageMagic(short) + pageNextFreeAreaPageIndex(long) + pageLastFreeAreaPageOffset(int)
    private static final int PAGE_MAGIC_OFFSET = 0;
    private static final int PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET = 2;
    private static final int PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET = 10;
    public static final byte FREE_AREA_MAGIC = 0x1A;// freeAreaMagic(byte) + nextFreeAreaPageOffset(int)
    private static final int FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET = 1;
    private final ObjectSpaceSchema schema;
    private final ObjectNodeManager nodeManager;
    private final ObjectNodeCacheManager nodeCacheManager;
    private final ObjectNodeCache nodeCache;
    private NodeIndexInfo[] indexes;
    private final Map<String, Integer> indexesMap = new HashMap<String, Integer>();
    private ObjectNodeFullTextIndex fullTextIndex;
    private BlobIndexInfo[] blobIndexes;
    private IRawPage headerPage;
    private boolean closed;
    private ObjectNode rootNode;
    private final NodeObjectCache nodeObjectCache = new NodeObjectCache();

    public static ObjectSpace create(ITransactionProvider transactionProvider, int fileIndex,
                                     ObjectSpaceSchema schema, ObjectNodeManager nodeManager, ObjectNodeCacheManager nodeCacheManager, long indexFileIndexesFileOffset) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        String filePrefix = getFilePrefix(schema, fileIndex);
        bindFile(schema, transaction, fileIndex, filePrefix, schema.getConfiguration().getPathIndex());

        ObjectSpace space = new ObjectSpace(transactionProvider, fileIndex, schema, filePrefix, nodeManager, nodeCacheManager);
        space.onCreated(indexFileIndexesFileOffset);

        return space;
    }

    public static ObjectSpace open(ITransactionProvider transactionProvider, int fileIndex, ObjectSpaceSchema schema,
                                   ObjectNodeManager nodeManager, ObjectNodeCacheManager nodeCacheManager, long indexFileIndexesFileOffset) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        String filePrefix = getFilePrefix(schema, fileIndex);
        bindFile(schema, transaction, fileIndex, filePrefix, schema.getConfiguration().getPathIndex());

        ObjectSpace space = new ObjectSpace(transactionProvider, fileIndex, schema, filePrefix, nodeManager, nodeCacheManager);
        space.onOpened(indexFileIndexesFileOffset);
        return space;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public NodeObjectCache getNodeObjectCache() {
        return nodeObjectCache;
    }

    @Override
    public ObjectNodeManager getNodeManager() {
        return nodeManager;
    }

    @Override
    public ObjectNodeCacheManager getNodeCacheManager() {
        return nodeCacheManager;
    }

    @Override
    public ObjectNodeCache getNodeCache() {
        return nodeCache;
    }

    public long allocateArea(IRawPage preferredPage) {
        Assert.checkState(!closed);

        long areaBlockIndex = allocateAreaFromPage(preferredPage, false);
        if (areaBlockIndex > 0)
            return areaBlockIndex;

        IRawWriteRegion region = headerPage.getWriteRegion();
        areaBlockIndex = region.readLong(NEXT_BLOCK_INDEX_OFFSET);

        if (Constants.pageIndexByBlockIndex(areaBlockIndex) != preferredPage.getIndex()) {
            long lastFreeAreaPageIndex = region.readLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET);
            while (lastFreeAreaPageIndex != 0) {
                IRawPage lastFreeAreaPage = getRawTransaction().getPage(fileIndex, lastFreeAreaPageIndex);
                areaBlockIndex = allocateAreaFromPage(lastFreeAreaPage, true);

                if (areaBlockIndex > 0)
                    return areaBlockIndex;

                lastFreeAreaPageIndex = -areaBlockIndex;
            }
        }

        return allocateBlocks(Constants.COMPLEX_FIELD_AREA_BLOCK_COUNT + 1);
    }

    public void freeArea(IRawPage page, int pageOffset) {
        Assert.checkState(!closed);
        Assert.isTrue((pageOffset & Constants.BLOCK_MASK) == pageOffset);
        Assert.isTrue(pageOffset > 0);

        IRawWriteRegion pageRegion = page.getWriteRegion();
        short value = pageRegion.readShort(PAGE_MAGIC_OFFSET);
        if (value != PAGE_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));
        int pageLastFreeAreaPageOffset = pageRegion.readInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET);
        pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, pageOffset);

        pageRegion.writeByte(pageOffset, FREE_AREA_MAGIC);
        pageRegion.writeInt(pageOffset + FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET,
                pageLastFreeAreaPageOffset != 0 ? pageLastFreeAreaPageOffset : -1);

        if (pageLastFreeAreaPageOffset == 0) {
            IRawWriteRegion region = headerPage.getWriteRegion();
            long lastFreeAreaPageIndex = region.readLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET);
            Assert.checkState(lastFreeAreaPageIndex != page.getIndex());
            region.writeLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET, page.getIndex());

            pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, lastFreeAreaPageIndex);
        }
    }

    public long getFreeNodeBlockIndex(int schemaIndex) {
        IRawReadRegion region = headerPage.getReadRegion();

        int offset = schemaIndex * 8 + HEADER_SIZE;
        return region.readLong(offset);
    }

    public long setFreeNodeBlockIndex(int schemaIndex, long nodeBlockIndex) {
        IRawWriteRegion region = headerPage.getWriteRegion();

        int offset = schemaIndex * 8 + HEADER_SIZE;
        long prevFreeNodeBlockIndex = region.readLong(offset);
        region.writeLong(offset, nodeBlockIndex);

        return prevFreeNodeBlockIndex;
    }

    public <T> T addNode(Object key, int index, Object... args) {
        return findOrCreateNode(key, schema.getNodes().get(index), args);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public long allocateBlocks(int blockCount) {
        Assert.isTrue(blockCount + PAGE_HEADER_BLOCK_COUNT <= Constants.BLOCKS_PER_PAGE_COUNT);
        Assert.checkState(!closed);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long nextBlockIndex = region.readLong(NEXT_BLOCK_INDEX_OFFSET);
        Assert.isTrue(nextBlockIndex != 0);
        int pageOffset = Constants.pageOffsetByBlockIndex(nextBlockIndex);
        int requiredSize = pageOffset + Constants.dataSize(blockCount);
        if (requiredSize > Constants.PAGE_SIZE) {
            IRawPage page = getRawTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex));
            while (pageOffset + Constants.COMPLEX_FIELD_AREA_SIZE <= Constants.PAGE_SIZE) {
                freeArea(page, pageOffset);
                pageOffset += Constants.COMPLEX_FIELD_AREA_SIZE;
            }
        }

        long res = nextBlockIndex;
        if (requiredSize >= Constants.PAGE_SIZE) {
            IRawPage page = getRawTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex) + 1);
            IRawWriteRegion pageRegion = page.getWriteRegion();
            pageRegion.writeShort(PAGE_MAGIC_OFFSET, PAGE_MAGIC);
            pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, 0);
            pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, 0);

            nextBlockIndex = Constants.blockIndex(page.getIndex(), PAGE_HEADER_SIZE);

            if (requiredSize > Constants.PAGE_SIZE)
                res = nextBlockIndex;
        }

        region.writeLong(NEXT_BLOCK_INDEX_OFFSET, nextBlockIndex + blockCount);

        return res;
    }

    public void delete() {
        Assert.checkState(!closed);

        nodeManager.flush();

        closed = true;

        headerPage.getWriteRegion().writeByte(CLOSED_OFFSET, (byte) 1);

        for (int i = 0; i < indexes.length; i++)
            getIndex(i).getIndex().delete();
        indexes = null;
        indexesMap.clear();

        for (int i = 0; i < blobIndexes.length; i++)
            getBlobIndex(i).delete();
        blobIndexes = null;

        if (fullTextIndex != null) {
            fullTextIndex.getIndex().delete();
            fullTextIndex = null;
        }
    }

    public void unload() {
        nodeCache.release();

        if (indexes != null) {
            for (int i = 0; i < indexes.length; i++) {
                if (indexes[i].index != null)
                    indexes[i].index.unload();
            }

            indexes = null;
            indexesMap.clear();
        }

        if (blobIndexes != null) {
            for (int i = 0; i < blobIndexes.length; i++) {
                if (blobIndexes[i].index != null)
                    blobIndexes[i].index.unload();
            }

            blobIndexes = null;
        }

        if (fullTextIndex != null) {
            fullTextIndex.unload();
            fullTextIndex = null;
        }

        nodeObjectCache.clear();
    }

    @Override
    public ObjectSpaceSchema getSchema() {
        return schema;
    }

    @Override
    public <T> T getRootNode() {
        if (rootNode != null && !rootNode.isStale())
            return rootNode.getObject();

        return readRootNode();
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
        Assert.isTrue(field.getParent().getParent() == schema);
        return (T) getIndex(field.getIndexTotalIndex());
    }

    @Override
    public <T extends INodeIndex> T findIndex(String indexName) {
        Integer value = indexesMap.get(indexName);
        if (value == null)
            return null;

        return (T) getIndex(value);
    }

    @Override
    public ObjectNodeFullTextIndex getFullTextIndex() {
        return fullTextIndex;
    }

    @Override
    public <T> T findOrCreateNode(Object key, INodeSchema schema, Object... args) {
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == this.schema);
        Assert.isTrue(schema.getPrimaryField() != null || key == null);
        Assert.checkState(!closed);

        if (key != null) {
            INodeIndex index = getIndex(schema.getPrimaryField());
            T res = (T) index.find(key);
            if (res != null)
                return res;
        }

        ObjectNode node = createNode(key, schema.getIndex(), false, false, args);
        return node.getObject();
    }

    public <T> T findOrCreateMigratedNode(Object key, INodeSchema schema) {
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == this.schema);
        Assert.isTrue(schema.getPrimaryField() != null || key == null);
        Assert.checkState(!closed);

        if (key != null) {
            INodeIndex index = getIndex(schema.getPrimaryField());
            T res = (T) index.find(key);
            if (res != null)
                return res;
        }

        ObjectNode node = createNode(key, schema.getIndex(), false, true, null);
        return node.getObject();
    }

    @Override
    public boolean containsNode(Object key, INodeSchema schema) {
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == this.schema);
        Assert.isTrue(schema.getPrimaryField() != null && key != null);
        Assert.checkState(!closed);

        INodeIndex index = getIndex(schema.getPrimaryField());
        return index.contains(key);
    }

    @Override
    public <T> T findNode(Object key, INodeSchema schema) {
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == this.schema);
        Assert.isTrue(schema.getPrimaryField() != null && key != null);
        Assert.checkState(!closed);

        INodeIndex index = getIndex(schema.getPrimaryField());
        return (T) index.find(key);
    }

    @Override
    public <T> T createNode(Object key, INodeSchema schema, Object... args) {
        Assert.notNull(schema);
        Assert.isTrue(schema.getParent() == this.schema);
        Assert.isTrue(schema.getPrimaryField() != null || key == null);
        Assert.checkState(!closed);

        if (key != null)
            Assert.isTrue(!containsNode(key, schema));

        ObjectNode node = createNode(key, schema.getIndex(), false, false, args);
        return node.getObject();
    }

    @Override
    public <T> Iterable<T> getNodes(INodeSchema schema) {
        return new NodeIterable<T>(schema);
    }

    @Override
    public <T> Iterable<T> getNodes() {
        return new NodeIterable<T>(null);
    }

    @Override
    public void addIndexValue(IFieldSchema field, Object key, INode node, boolean updateIndex, boolean updateCache) {
        NodeIndex<Object, Long> index = getIndex(field);
        index.add(key, node, updateIndex, updateCache);
    }

    @Override
    public void updateIndexValue(IFieldSchema field, Object oldKey, Object newKey, INode node) {
        NodeIndex<Object, Long> index = getIndex(field);
        index.update(oldKey, newKey, node);
    }

    @Override
    public void removeIndexValue(IFieldSchema field, Object key, INode node, boolean updateIndex, boolean updateCache) {
        if (indexes == null)
            return;

        NodeIndex<Object, Long> index = getIndex(field);
        index.remove(key, node, updateIndex, updateCache);
    }

    @Override
    public void write(IDataSerialization serialization, IDocument document) {
        if (!(document.getContext() instanceof IFullTextDocumentSpace)) {
            long nodeId = ((INumericField) document.getFields().get(0)).get().longValue();
            serialization.writeBoolean(true);
            serialization.writeLong(nodeId);
        } else {
            IFullTextDocumentSpace space = document.getContext();
            IField field = document.getContext();
            serialization.writeBoolean(false);
            serialization.writeLong(field.getNode().getId());
            serialization.writeInt(field.getSchema().getIndex());
            space.write(serialization, document);
        }
    }

    @Override
    public void readAndReindex(IDataDeserialization deserialization) {
        if (deserialization.readBoolean()) {
            long nodeId = deserialization.readLong();

            INode node = ((INodeObject) findNodeById(nodeId)).getNode();
            fullTextIndex.update(node);
        } else {
            long nodeId = deserialization.readLong();
            int fieldIndex = deserialization.readInt();

            INode node = ((INodeObject) findNodeById(nodeId)).getNode();
            IFullTextDocumentSpace space = node.getField(fieldIndex);
            space.readAndReindex(deserialization);
        }
    }

    public List<String> beginSnapshot() {
        if (fullTextIndex != null)
            return fullTextIndex.beginSnapshot();
        else
            return Collections.emptyList();
    }

    public void endSnapshot() {
        if (fullTextIndex != null)
            fullTextIndex.endSnapshot();
    }

    @Override
    public Node loadNode(long id, NodeObject object) {
        ObjectNode node = nodeCache.findById(fileIndex, id);
        if (node == null) {
            node = ObjectNode.open(this, id, object);
            if (node.isDeleted())
                return null;

            nodeCache.addNode(node, false);
        }

        return node;
    }

    @Override
    public Map<String, String> getProperties(NodeSchemaConfiguration configuration) {
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", schema.getParent().getConfiguration().getName())
                .put("nodeName", configuration.getName())
                .put("name", schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName() + "." + configuration.getName())
                .toMap();

        return properties;
    }

    @Override
    public String getFieldId(IField field) {
        return Long.toString(field.getNode().getId()) + ":" + field.getSchema().getIndex();
    }

    @Override
    public IUniqueIndex getBlobIndex(StructuredBlobFieldSchema field, int blobIndex) {
        Assert.notNull(field);
        Assert.isTrue(blobIndex < field.getConfiguration().getIndexes().size());
        Assert.isTrue(field.getParent().getParent() == schema);
        return getBlobIndex(field.getBlobIndexTotalIndex(blobIndex));
    }

    public void dump(IJsonHandler json, DumpContext context) {
        context.reset();

        if (context.getQuery() != null && context.getQuery().contains("keys")) {
            NameFilter filter = NameFilter.toFilter(JsonUtils.<String>toList((JsonArray) context.getQuery().get("keys")), null);
            json.key("<objects>");
            json.startArray();
            for (INodeObject node : this.<INodeObject>getNodes()) {
                String key = ((IObjectNode) node.getNode()).getKey().toString();
                if (filter.match(key)) {
                    json.startObject();
                    node.dump(json, context);
                    json.endObject();
                }
            }
            json.endArray();
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

    @Override
    public String toString() {
        return Spaces.getSpaceDataFileName(filePrefix, fileIndex);
    }

    private ObjectSpace(ITransactionProvider transactionProvider, int fileIndex, ObjectSpaceSchema schema,
                        String filePrefix, ObjectNodeManager nodeManager, ObjectNodeCacheManager nodeCacheManager) {
        super(transactionProvider, fileIndex, filePrefix);

        Assert.notNull(schema);
        Assert.notNull(nodeManager);
        Assert.notNull(nodeCacheManager);

        this.schema = schema;
        this.nodeManager = nodeManager;
        this.nodeCacheManager = nodeCacheManager;
        Pair<String, String> pair = schema.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "nodes.data.object")
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", schema.getParent().getConfiguration().getName())
                .put("name", schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName())
                .toMap());
        this.nodeCache = nodeCacheManager.getNodeCache(pair.getKey(), pair.getValue());
        this.nodeCache.addRef();
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
    }

    private void onCreated(long indexFileIndexesFileOffset) {
        closed = false;

        writeHeader();
        createIndexes(indexFileIndexesFileOffset);

        createRootNode();
    }

    private void onOpened(long indexFileIndexesFileOffset) {
        readHeader();
        openIndexes(indexFileIndexesFileOffset);
    }

    private void readHeader() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();
        closed = deserialization.readBoolean();
        deserialization.readLong(); // nextBlockIndex (long) 
        deserialization.readLong(); // lastNodeBlockIndex (long)
        deserialization.readLong(); // lastFreeAreaPageIndex(long)
        deserialization.readLong(); // rootNodeBlockIndex(long)

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);
        serialization.writeBoolean(closed);
        serialization.writeLong(Constants.BLOCKS_PER_PAGE_COUNT + PAGE_HEADER_BLOCK_COUNT);
        serialization.writeLong(0);
        serialization.writeLong(0);
        serialization.writeLong(0);

        IRawPage page = getRawTransaction().getPage(fileIndex, 1);
        IRawWriteRegion pageRegion = page.getWriteRegion();
        pageRegion.writeShort(PAGE_MAGIC_OFFSET, PAGE_MAGIC);
        pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, 0);
        pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, 0);
    }

    private void createRootNode() {
        if (schema.getRootNode() != null) {
            ObjectNode rootNode = createNode(null, schema.getRootNode().getIndex(), true, false, null);
            headerPage.getWriteRegion().writeLong(ROOT_NODE_BLOCK_INDEX_OFFSET, rootNode.getNodeBlockIndex());
            if (rootNode.isCached())
                this.rootNode = rootNode;
        }
    }

    private long allocateAreaFromPage(IRawPage page, boolean correct) {
        IRawWriteRegion pageRegion = page.getWriteRegion();
        short value = pageRegion.readShort(PAGE_MAGIC_OFFSET);
        if (value != PAGE_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));
        int pageLastFreeAreaPageOffset = pageRegion.readInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET);
        long res;
        int nextFreeAreaPageOffset = -1;
        long nextFreeAreaPageIndex = pageRegion.readLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET);
        if (pageLastFreeAreaPageOffset > 0) {
            if (pageRegion.readByte(pageLastFreeAreaPageOffset) != FREE_AREA_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));
            nextFreeAreaPageOffset = pageRegion.readInt(pageLastFreeAreaPageOffset + FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET);
            pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, nextFreeAreaPageOffset);
            res = Constants.blockIndex(page.getIndex(), pageLastFreeAreaPageOffset);
        } else
            res = -nextFreeAreaPageIndex;

        if (correct && nextFreeAreaPageOffset <= 0) {
            pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, 0);
            IRawWriteRegion region = headerPage.getWriteRegion();
            region.writeLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET, nextFreeAreaPageIndex);
        }
        return res;
    }

    private ObjectNode createNode(Object key, int schemaIndex, boolean root, boolean migrated, Object[] args) {
        long lastNodeBlockIndex = headerPage.getWriteRegion().readLong(LAST_NODE_BLOCK_INDEX_OFFSET);
        ObjectNode node = ObjectNode.create(this, schemaIndex, lastNodeBlockIndex, key, root, args);

        if (node.getPrevNodeBlockIndex() == lastNodeBlockIndex) {
            lastNodeBlockIndex = node.getNodeBlockIndex();
            headerPage.getWriteRegion().writeLong(LAST_NODE_BLOCK_INDEX_OFFSET, lastNodeBlockIndex);
        }

        nodeCache.addNode(node, true);

        INodeObject object = node.getObject();
        if (!migrated)
            object.onCreated(key, args);
        else
            object.onBeforeMigrated(key);

        return node;
    }

    private <T> T readRootNode() {
        long rootNodeBlockIndex = headerPage.getReadRegion().readLong(ROOT_NODE_BLOCK_INDEX_OFFSET);
        if (rootNodeBlockIndex == 0)
            return null;

        rootNode = nodeCache.findById(fileIndex, rootNodeBlockIndex);
        if (rootNode == null) {
            ObjectNode rootNode = ObjectNode.open(this, rootNodeBlockIndex, null);
            Assert.checkState(!rootNode.isDeleted());
            nodeCache.addNode(rootNode, false);

            if (rootNode.isCached())
                this.rootNode = rootNode;
            else
                return rootNode.getObject();
        }
        return rootNode.getObject();
    }

    private static void bindFile(ObjectSpaceSchema schema, IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceDataFileName(filePrefix, fileIndex));
        bindInfo.setFlags(RawBindInfo.DIRECTORY_OWNER);

        Pair<String, String> pair = schema.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.data.object")
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", schema.getParent().getConfiguration().getName())
                .put("name", schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName())
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    public static void createNewIndexes(IIndexManager indexManager, int fileIndex, ObjectSpaceSchema schema, int skipIndexCount,
                                        int skipBlobIndexCount, List<Integer> newIndexIds, List<Integer> newBlobIndexIds) {
        String filePrefix = getFilePrefix(schema, fileIndex);
        String indexesPath = Spaces.getSpaceIndexesDirName(filePrefix);
        int i = 0;
        Map<String, Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>> indexedFields = new HashMap<String,
                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>>();

        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!field.isIndexed())
                    continue;

                String indexName = field.getIndexName();
                if (indexName == null)
                    indexName = node.getName() + "." + field.getName();

                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration> indexedPair = indexedFields.get(indexName);
                if (indexedPair == null) {
                    indexedFields.put(indexName, new Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>(node, field));
                    if (i >= skipIndexCount) {
                        IndexSchemaConfiguration indexConfiguration = createIndexSchemaConfiguration(schema, node, field);
                        String indexFilePrefix = indexesPath + File.separator + indexConfiguration.getType();
                        IUniqueIndex index = indexManager.createIndex(indexFilePrefix, indexConfiguration);
                        newIndexIds.add(index.getId());
                    }

                    i++;
                } else {
                    IndexSchemaConfiguration indexConfiguration1 = createIndexSchemaConfiguration(schema, indexedPair.getKey(), field);
                    IndexSchemaConfiguration indexConfiguration2 = createIndexSchemaConfiguration(schema, indexedPair.getKey(),
                            indexedPair.getValue());
                    Assert.isTrue(indexConfiguration1.equals(indexConfiguration2));
                    Assert.isTrue(field.isSorted() == indexedPair.getValue().isSorted());
                    Assert.isTrue(field.isCached() == indexedPair.getValue().isCached());
                }
            }
        }

        i = 0;
        Map<String, StructuredBlobIndexSchemaConfiguration> indexedBlobs = new HashMap<String, StructuredBlobIndexSchemaConfiguration>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!(field instanceof StructuredBlobFieldSchemaConfiguration) || ((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty())
                    continue;

                for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                    String indexName = blobIndex.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                    StructuredBlobIndexSchemaConfiguration indexedBlob = indexedBlobs.get(indexName);
                    if (indexedBlob == null) {
                        indexedBlobs.put(indexName, blobIndex);
                        if (i >= skipBlobIndexCount) {
                            IndexSchemaConfiguration indexConfiguration = createBlobIndexSchemaConfiguration(schema, node, blobIndex);
                            String indexFilePrefix = indexesPath + File.separator + indexConfiguration.getType();
                            IUniqueIndex index = indexManager.createIndex(indexFilePrefix, indexConfiguration);
                            newBlobIndexIds.add(index.getId());
                        }
                        i++;
                    } else
                        Assert.isTrue(blobIndex.equals(indexedBlob));
                }
            }
        }
    }

    private void createIndexes(long indexFileIndexesFileOffset) {
        indexes = new NodeIndexInfo[schema.getConfiguration().getTotalIndexCount()];

        IRawTransaction transaction = schema.getContext().getTransactionProvider().getRawTransaction();
        IIndexManager indexManager = schema.getContext().findTransactionExtension(IIndexManager.NAME);

        int i = 0;
        Map<String, Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>> indexedFields = new HashMap<String,
                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>>();
        List<Integer> indexIds = new ArrayList<Integer>();

        if (schema.getConfiguration().hasFullTextIndex()) {
            FullTextIndexSchemaConfiguration indexConfiguration = new FullTextIndexSchemaConfiguration(schema.getConfiguration().getName(),
                    schema.getConfiguration().getAlias(), schema.getConfiguration().getDescription(), schema.getConfiguration().getFullTextPathIndex());
            String filePrefix = indexesPath + File.separator + indexConfiguration.getType();
            IFullTextIndex index = indexManager.createIndex(filePrefix, indexConfiguration);
            fullTextIndex = new ObjectNodeFullTextIndex(schema.getContext(), index, index.getId(), this);
            indexIds.add(index.getId());
        }

        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!field.isIndexed())
                    continue;

                String indexName = field.getIndexName();
                if (indexName == null)
                    indexName = node.getName() + "." + field.getName();

                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration> indexedPair = indexedFields.get(indexName);
                if (indexedPair == null) {
                    indexedFields.put(indexName, new Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>(node, field));
                    IndexSchemaConfiguration indexConfiguration = createIndexSchemaConfiguration(schema, node, field);
                    String filePrefix = indexesPath + File.separator + indexConfiguration.getType();
                    IUniqueIndex index = indexManager.createIndex(filePrefix, indexConfiguration);
                    indexes[i] = new NodeIndexInfo(index.getId(), field.isSorted(), field.isCached(),
                            createNodeIndex(index, field.isSorted(), field.isCached()));
                    indexesMap.put(indexName, i);
                    indexIds.add(index.getId());
                    i++;
                } else {
                    IndexSchemaConfiguration indexConfiguration1 = createIndexSchemaConfiguration(schema, indexedPair.getKey(), field);
                    IndexSchemaConfiguration indexConfiguration2 = createIndexSchemaConfiguration(schema, indexedPair.getKey(), indexedPair.getValue());
                    Assert.isTrue(indexConfiguration1.equals(indexConfiguration2));
                    Assert.isTrue(field.isSorted() == indexedPair.getValue().isSorted());
                    Assert.isTrue(field.isCached() == indexedPair.getValue().isCached());
                }
            }
        }

        blobIndexes = new BlobIndexInfo[schema.getConfiguration().getTotalBlobIndexCount()];
        i = 0;
        Map<String, StructuredBlobIndexSchemaConfiguration> indexedBlobs = new HashMap<String, StructuredBlobIndexSchemaConfiguration>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!(field instanceof StructuredBlobFieldSchemaConfiguration) || ((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty())
                    continue;

                for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                    String indexName = blobIndex.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                    StructuredBlobIndexSchemaConfiguration indexedBlob = indexedBlobs.get(indexName);
                    if (indexedBlob == null) {
                        indexedBlobs.put(indexName, blobIndex);
                        IndexSchemaConfiguration indexConfiguration = createBlobIndexSchemaConfiguration(schema, node, blobIndex);
                        String filePrefix = indexesPath + File.separator + indexConfiguration.getType();
                        IUniqueIndex index = indexManager.createIndex(filePrefix, indexConfiguration);
                        blobIndexes[i] = new BlobIndexInfo(index.getId(), index);
                        indexIds.add(index.getId());
                        i++;
                    } else
                        Assert.isTrue(blobIndex.equals(indexedBlob));
                }
            }
        }

        RawPageSerialization serialization = new RawPageSerialization(transaction,
                0, Constants.pageIndexByFileOffset(indexFileIndexesFileOffset), Constants.pageOffsetByFileOffset(indexFileIndexesFileOffset));
        for (Integer id : indexIds)
            serialization.writeInt(id);
    }

    private NodeIndex getIndex(int i) {
        NodeIndexInfo info = indexes[i];
        if (info.index == null) {
            IIndexManager indexManager = schema.getContext().findTransactionExtension(IIndexManager.NAME);
            IUniqueIndex index = indexManager.getIndex(info.id);
            info.index = createNodeIndex(index, info.sorted, info.cached);
        }

        return info.index;
    }

    private IUniqueIndex getBlobIndex(int i) {
        BlobIndexInfo info = blobIndexes[i];
        if (info.index == null || info.index.isStale()) {
            IIndexManager indexManager = schema.getContext().findTransactionExtension(IIndexManager.NAME);
            IUniqueIndex index = indexManager.getIndex(info.id);
            info.index = index;
        }

        return info.index;
    }

    private void openIndexes(long indexFileIndexesFileOffset) {
        indexes = new NodeIndexInfo[schema.getConfiguration().getTotalIndexCount()];

        IRawTransaction transaction = schema.getContext().getTransactionProvider().getRawTransaction();
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction,
                0, Constants.pageIndexByFileOffset(indexFileIndexesFileOffset), Constants.pageOffsetByFileOffset(indexFileIndexesFileOffset));

        int fullTextIndexId = 0;
        if (schema.getConfiguration().hasFullTextIndex())
            fullTextIndexId = deserialization.readInt();

        int i = 0;
        Set<String> indexNames = new HashSet<String>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!field.isIndexed())
                    continue;

                String indexName = field.getIndexName();
                if (indexName == null)
                    indexName = node.getName() + "." + field.getName();

                if (!indexNames.contains(indexName)) {
                    indexNames.add(indexName);
                    indexes[i] = new NodeIndexInfo(deserialization.readInt(), field.isSorted(), field.isCached(), null);
                    indexesMap.put(indexName, i);
                    i++;
                }
            }
        }

        blobIndexes = new BlobIndexInfo[schema.getConfiguration().getTotalBlobIndexCount()];
        i = 0;
        indexNames = new HashSet<String>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!(field instanceof StructuredBlobFieldSchemaConfiguration) || ((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty())
                    continue;

                for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                    String indexName = blobIndex.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                    if (!indexNames.contains(indexName)) {
                        indexNames.add(indexName);
                        blobIndexes[i] = new BlobIndexInfo(deserialization.readInt(), null);
                        i++;
                    }
                }
            }
        }

        if (schema.getConfiguration().hasFullTextIndex()) {
            fullTextIndex = new ObjectNodeFullTextIndex(schema.getContext(), null, fullTextIndexId, this);
            fullTextIndex.reindex();
        }
    }

    private NodeIndex createNodeIndex(IUniqueIndex index, boolean sorted, boolean cached) {
        if (sorted) {
            if (index instanceof INonUniqueSortedIndex)
                return new ObjectNodeNonUniqueSortedIndex(schema.getContext(), (INonUniqueSortedIndex) index, this);
            else if (index instanceof ISortedIndex)
                return new ObjectNodeSortedIndex(schema.getContext(), (ISortedIndex) index, this);
            else
                return Assert.error();
        } else
            return new ObjectNodeIndex(schema.getContext(), index, this, cached);
    }

    private static IndexSchemaConfiguration createIndexSchemaConfiguration(ObjectSpaceSchema schema,
                                                                           NodeSchemaConfiguration node, FieldSchemaConfiguration field) {
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", schema.getParent().getConfiguration().getName())
                .put("nodeName", node.getName())
                .put("name", schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName() + "." + node.getName())
                .toMap();

        String namePrefix = schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName() + "." + node.getName() + ".";
        String aliasPrefix = schema.getParent().getConfiguration().getAlias() + "." + schema.getConfiguration().getAlias() + "." + node.getAlias() + ".";

        return field.createIndexSchemaConfiguration(namePrefix, aliasPrefix, properties);
    }

    private static IndexSchemaConfiguration createBlobIndexSchemaConfiguration(ObjectSpaceSchema schema,
                                                                               NodeSchemaConfiguration node, StructuredBlobIndexSchemaConfiguration configuration) {
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", schema.getParent().getConfiguration().getName())
                .put("nodeName", node.getName())
                .put("name", schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName() + "." + node.getName())
                .toMap();

        String namePrefix = schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName() + "." + node.getName() + ".";
        String aliasPrefix = schema.getParent().getConfiguration().getAlias() + "." + schema.getConfiguration().getAlias() + "." + node.getAlias() + ".";

        int maxKeySize = configuration.getMaxKeySize() + 10;
        KeyNormalizerSchemaConfiguration keyNormalizer = new ByteArrayKeyNormalizerSchemaConfiguration();

        switch (configuration.getIndexType()) {
            case BTREE:
                return new BTreeIndexSchemaConfiguration(namePrefix + configuration.getName(), aliasPrefix + configuration.getAlias(), configuration.getDescription(),
                        configuration.getPathIndex(), configuration.isFixedKey(), maxKeySize,
                        true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), configuration.isSorted(), configuration.isUnique(), properties);
            case TREE:
                return new TreeIndexSchemaConfiguration(namePrefix + configuration.getName(), aliasPrefix + configuration.getAlias(), configuration.getDescription(),
                        configuration.getPathIndex(), configuration.isFixedKey(), maxKeySize,
                        true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), configuration.isSorted(),
                        configuration.isUnique(), properties);
            case HASH:
                return new HashIndexSchemaConfiguration(namePrefix + configuration.getName(), aliasPrefix + configuration.getAlias(), configuration.getDescription(),
                        configuration.getPathIndex(), configuration.isFixedKey(), maxKeySize,
                        true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), properties);
            default:
                return Assert.error();
        }
    }

    private static String getFilePrefix(ObjectSpaceSchema schema, int fileIndex) {
        return ObjectSpaces.getObjectSpacePrefix(schema.getParent().getConfiguration().getName(), schema.getConfiguration(), fileIndex);
    }

    private class NodeIterable<T> implements Iterable<T> {
        private final INodeSchema schema;

        public NodeIterable(INodeSchema schema) {
            this.schema = schema;
        }

        @Override
        public Iterator<T> iterator() {
            long lastNodeBlockIndex = 0;
            if (headerPage != null)
                lastNodeBlockIndex = headerPage.getReadRegion().readLong(LAST_NODE_BLOCK_INDEX_OFFSET);

            return new NodeIterator<T>(schema, lastNodeBlockIndex);
        }
    }

    private class NodeIterator<T> implements Iterator<T> {
        private final INodeSchema schema;
        private ObjectNode nextNode;

        public NodeIterator(INodeSchema schema, long nodeBlockIndex) {
            this.schema = schema;
            this.nextNode = findNext(nodeBlockIndex);
        }

        @Override
        public boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public T next() {
            Assert.notNull(nextNode);

            T res = nextNode.getObject();
            nextNode = findNext(nextNode.getPrevNodeBlockIndex());

            return res;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }

        private ObjectNode findNext(long nodeBlockIndex) {
            while (nodeBlockIndex != 0) {
                ObjectNode node = nodeCache.findById(fileIndex, nodeBlockIndex);
                if (node == null) {
                    node = ObjectNode.open(ObjectSpace.this, nodeBlockIndex, null);
                    if (!node.isDeleted())
                        nodeCache.addNode(node, false);
                }

                if (!node.isDeleted() && (schema == null || node.getSchema() == schema))
                    return node;

                nodeBlockIndex = node.getPrevNodeBlockIndex();
            }

            return null;
        }
    }

    public static class NodeIndexInfo {
        public final int id;
        public final boolean sorted;
        public final boolean cached;
        private NodeIndex index;

        public NodeIndexInfo(int id, boolean sorted, boolean cached, NodeIndex index) {
            this.id = id;
            this.sorted = sorted;
            this.cached = cached;
            this.index = index;
        }
    }

    public static class BlobIndexInfo {
        public final int id;
        private IUniqueIndex index;

        public BlobIndexInfo(int id, IUniqueIndex index) {
            this.id = id;
            this.index = index;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
