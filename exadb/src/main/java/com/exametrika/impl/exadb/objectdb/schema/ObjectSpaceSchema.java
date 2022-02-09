/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.SimpleAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.objectdb.IObjectMigrator;
import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.JsonWriter;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.objectdb.ObjectDatabaseExtension;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.ObjectSpaces;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ISpaceSchemaControl;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;
import com.exametrika.spi.exadb.objectdb.schema.INodeMigrationSchema;
import com.exametrika.spi.exadb.objectdb.schema.ISpaceMigrationSchema;


/**
 * The {@link ObjectSpaceSchema} represents a schema of object node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectSpaceSchema extends NodeSpaceSchema implements IObjectSpaceSchema, ISpaceSchemaControl {
    private final ObjectDatabaseExtension extension;
    private long spaceFileIndexFileOffset;
    private long indexFileIndexesFileOffset;
    private ObjectSpace space;

    public ObjectSpaceSchema(ObjectSpaceSchemaConfiguration configuration, IDatabaseContext context, int version) {
        super(context, configuration, version, TYPE);

        Assert.notNull(configuration);
        Assert.notNull(context);

        this.extension = context.findExtension(ObjectDatabaseExtensionConfiguration.NAME);
        Assert.notNull(this.extension);
    }

    @Override
    public void setParent(IDomainSchema domain, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(domain);

        super.setParent(domain, schemaObjects);
    }

    @Override
    public IDomainSchema getParent() {
        return (IDomainSchema) super.getParent();
    }

    public ObjectSpace compact() {
        ObjectSpace res = space;
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        int fileIndex = readSpaceFileIndex(transaction);

        migrate(null, null);

        removeSpaceFiles(transaction, fileIndex);

        return res;
    }

    @Override
    public ObjectSpaceSchemaConfiguration getConfiguration() {
        return (ObjectSpaceSchemaConfiguration) configuration;
    }

    @Override
    public boolean isCompatible(ISpaceSchema schema, IDataMigrator dataMigrator) {
        ObjectSpaceSchema objectSchema = (ObjectSpaceSchema) schema;
        for (INodeSchema oldNode : getNodes()) {
            INodeSchema newNode = objectSchema.findNode(oldNode.getConfiguration().getName());
            if (newNode != null && !isCompatible(oldNode, newNode, (IObjectMigrator) dataMigrator))
                return false;
        }
        return true;
    }

    @Override
    public void read(RawPageDeserialization deserialization) {
        long offset = deserialization.getFileOffset();
        Assert.checkState(offset != 0);
        Assert.checkState(spaceFileIndexFileOffset == 0);

        spaceFileIndexFileOffset = offset;
        deserialization.readInt();
        Assert.checkState(indexFileIndexesFileOffset == 0);
        indexFileIndexesFileOffset = deserialization.getFileOffset();
        if (configuration.hasFullTextIndex())
            deserialization.readInt();
        for (int i = 0; i < configuration.getTotalIndexCount(); i++)
            deserialization.readInt();
        for (int i = 0; i < configuration.getTotalBlobIndexCount(); i++)
            deserialization.readInt();
    }

    @Override
    public void write(RawPageSerialization serialization) {
        long offset = serialization.getFileOffset();
        Assert.checkState(offset != 0);
        Assert.checkState(spaceFileIndexFileOffset == 0);

        spaceFileIndexFileOffset = offset;
        serialization.writeInt(0);
        Assert.checkState(indexFileIndexesFileOffset == 0);
        indexFileIndexesFileOffset = serialization.getFileOffset();
        if (configuration.hasFullTextIndex())
            serialization.writeInt(0);
        for (int i = 0; i < configuration.getTotalIndexCount(); i++)
            serialization.writeInt(0);
        for (int i = 0; i < configuration.getTotalBlobIndexCount(); i++)
            serialization.writeInt(0);
    }

    @Override
    public List<String> beginSnapshot() {
        List<String> files = new ArrayList<String>();
        if (space != null)
            files.addAll(space.beginSnapshot());

        files.addAll(ObjectSpaces.getObjectSpaceFileNames(
                context.getConfiguration().getPaths(), getConfiguration().getPathIndex(),
                getParent().getConfiguration().getName(), getConfiguration(), getSpaceFileIndex()));

        return files;
    }

    @Override
    public void endSnapshot() {
        if (space != null)
            space.endSnapshot();
    }

    @Override
    public ObjectSpace getSpace() {
        if (space != null)
            return space;

        return readSpace();
    }

    @Override
    public void onTransactionStarted() {
    }

    @Override
    public void onTransactionCommitted() {
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        return false;
    }

    @Override
    public void onTransactionRolledBack() {
    }

    @Override
    public void clearCaches() {
        if (space != null) {
            space.unload();
            space = null;
        }
    }

    @Override
    public void onTimer(long currentTime) {
    }

    @Override
    public void onCreated() {
        createSpace();
    }

    @Override
    public void onModified(ISpaceSchema oldSchema, IDataMigrator dataMigrator) {
        ObjectSpaceSchema oldObjectSchema = (ObjectSpaceSchema) oldSchema;
        if (!oldSchema.getConfiguration().equalsStructured(configuration))
            changeSchema(oldObjectSchema, dataMigrator);
        else {
            boolean structuredChange = false;
            if (oldObjectSchema.getConfiguration().hasFullTextIndex() != getConfiguration().hasFullTextIndex())
                structuredChange = true;

            if (structuredChange)
                changeSchema(oldObjectSchema, dataMigrator);
            else
                bindSpace(oldObjectSchema);
        }
    }

    @Override
    public void onAfterModified(ISpaceSchema oldSchema) {
    }

    @Override
    public void onDeleted() {
        ObjectSpace space = getSpace();
        if (space != null) {
            space.delete();
            extension.getNodeCacheManager().unloadNodesOfDeletedSpaces(Collections.singleton(space));
        }

        removeSpace();
    }

    @Override
    public void dump(File path, IDumpContext context) {
        ObjectSpace space = getSpace();
        if (space == null)
            return;

        String filePrefix = ObjectSpaces.getObjectSpacePrefix(getParent().getConfiguration().getName(),
                (ObjectSpaceSchemaConfiguration) configuration, space.getFileIndex()) + ".json";
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(path, filePrefix)));
            JsonWriter jsonWriter = new JsonWriter(writer, 4);
            jsonWriter.startText();
            jsonWriter.startObject();

            space.dump(jsonWriter, (DumpContext) context);

            jsonWriter.endObject();
            jsonWriter.endText();
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        } finally {
            IOs.close(writer);
        }
    }

    @Override
    protected IDocumentSchema createDocumentSchema(NodeSchemaConfiguration node, List<IFieldSchema> fields) {
        String documentType = node.getDocumentType();
        if (documentType == null)
            documentType = node.getName();

        List<com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration> documentFields =
                new ArrayList<com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration>();
        documentFields.add(new NumericFieldSchemaConfiguration(NODE_ID_FIELD_NAME, DataType.LONG, true, true));
        documentFields.add(new StringFieldSchemaConfiguration(DOCUMENT_TYPE_FIELD_NAME, Enums.of(Option.INDEXED,
                Option.INDEX_DOCUMENTS, Option.OMIT_NORMS), new SimpleAnalyzerSchemaConfiguration()));
        for (IFieldSchema field : fields) {
            if (field.getConfiguration().isFullTextIndexed())
                documentFields.add(field.getConfiguration().createFullTextSchemaConfiguration(node.getName()));
        }

        if (!documentFields.isEmpty())
            return new DocumentSchemaConfiguration(documentType, documentFields, new StandardAnalyzerSchemaConfiguration(), 2).createSchema();
        else
            return null;
    }

    private int readSpaceFileIndex(IRawTransaction transaction) {
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction,
                0, Constants.pageIndexByFileOffset(spaceFileIndexFileOffset), Constants.pageOffsetByFileOffset(spaceFileIndexFileOffset));
        return deserialization.readInt();
    }

    private void writeSpaceFileIndex(IRawTransaction transaction, int spaceFileIndex) {
        RawPageSerialization serialization = new RawPageSerialization(transaction,
                0, Constants.pageIndexByFileOffset(spaceFileIndexFileOffset), Constants.pageOffsetByFileOffset(spaceFileIndexFileOffset));
        serialization.writeInt(spaceFileIndex);

        if (spaceFileIndex == 0) {
            if (configuration.hasFullTextIndex())
                serialization.writeInt(0);

            for (int i = 0; i < configuration.getTotalIndexCount(); i++)
                serialization.writeInt(0);

            for (int i = 0; i < configuration.getTotalBlobIndexCount(); i++)
                serialization.writeInt(0);
        }
    }

    private int getSpaceFileIndex() {
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        return readSpaceFileIndex(transaction);
    }

    private ObjectSpace createSpace() {
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        int spaceFileIndex = context.getSchemaSpace().allocateFile(transaction);
        writeSpaceFileIndex(transaction, spaceFileIndex);

        space = ObjectSpace.create(context.getTransactionProvider(), spaceFileIndex, this, extension.getNodeManager(),
                extension.getNodeCacheManager(), indexFileIndexesFileOffset);

        return space;
    }

    private void removeSpace() {
        getSpace();
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        int fileIndex = readSpaceFileIndex(transaction);
        writeSpaceFileIndex(transaction, 0);

        removeSpaceFiles(transaction, fileIndex);

        clearCaches();
    }

    private void changeSchema(ObjectSpaceSchema oldObjectSchema, IDataMigrator dataMigrator) {
        createSpace();
        migrate(oldObjectSchema, (IObjectMigrator) dataMigrator);

        ObjectSpace space = oldObjectSchema.getSpace();
        if (space != null) {
            space.delete();
            extension.getNodeCacheManager().unloadNodesOfDeletedSpaces(Collections.singleton(space));
        }

        oldObjectSchema.removeSpace();
    }

    private void bindSpace(ObjectSpaceSchema oldSchema) {
        Assert.checkState(getSpace() == null);

        IRawTransaction rawTransaction = context.getTransactionProvider().getRawTransaction();
        ITransaction transaction = context.getTransactionProvider().getTransaction();

        RawPageSerialization oldSerialization = new RawPageSerialization(rawTransaction,
                0, Constants.pageIndexByFileOffset(oldSchema.spaceFileIndexFileOffset), Constants.pageOffsetByFileOffset(oldSchema.spaceFileIndexFileOffset));
        RawPageSerialization newSerialization = new RawPageSerialization(rawTransaction,
                0, Constants.pageIndexByFileOffset(spaceFileIndexFileOffset), Constants.pageOffsetByFileOffset(spaceFileIndexFileOffset));

        int spaceFileIndex = oldSerialization.readInt();
        newSerialization.writeInt(spaceFileIndex);

        if (configuration.hasFullTextIndex())
            newSerialization.writeInt(oldSerialization.readInt());

        IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
        List<Integer> newIndexIds = new ArrayList<Integer>();
        List<Integer> newBlobIndexIds = new ArrayList<Integer>();
        ObjectSpace.createNewIndexes(indexManager, spaceFileIndex, this,
                oldSchema.getConfiguration().getTotalIndexCount(), oldSchema.getConfiguration().getTotalBlobIndexCount(),
                newIndexIds, newBlobIndexIds);
        int k = 0;
        for (int i = 0; i < configuration.getTotalIndexCount(); i++) {
            if (i < oldSchema.getConfiguration().getTotalIndexCount()) {
                int indexFileIndex = oldSerialization.readInt();
                newSerialization.writeInt(indexFileIndex);
            } else {
                newSerialization.writeInt(newIndexIds.get(k));
                k++;
            }
        }

        k = 0;
        for (int i = 0; i < configuration.getTotalBlobIndexCount(); i++) {
            if (i < oldSchema.getConfiguration().getTotalBlobIndexCount()) {
                int indexFileIndex = oldSerialization.readInt();
                newSerialization.writeInt(indexFileIndex);
            } else {
                newSerialization.writeInt(newBlobIndexIds.get(k));
                k++;
            }
        }

        oldSerialization.setPosition(Constants.pageIndexByFileOffset(oldSchema.spaceFileIndexFileOffset),
                Constants.pageOffsetByFileOffset(oldSchema.spaceFileIndexFileOffset));
        oldSerialization.writeInt(0);
        if (oldSchema.getConfiguration().hasFullTextIndex())
            oldSerialization.writeInt(0);
        for (int i = 0; i < oldSchema.getConfiguration().getTotalIndexCount(); i++)
            oldSerialization.writeInt(0);
        for (int i = 0; i < oldSchema.getConfiguration().getTotalBlobIndexCount(); i++)
            oldSerialization.writeInt(0);

        space = oldSchema.space;
    }

    private void migrate(ObjectSpaceSchema oldSchema, IObjectMigrator dataMigrator) {
        ObjectSpaceSchema newSchema = this;
        ObjectSpace oldSpace, newSpace;

        if (oldSchema == null) {
            oldSchema = this;
            oldSpace = getSpace();
            newSpace = createSpace();
        } else {
            oldSpace = oldSchema.getSpace();
            newSpace = getSpace();
        }

        ISpaceMigrationSchema migrationSchema = createSpaceMigrationSchema(oldSchema, newSchema, dataMigrator);

        long rootNodeId = oldSpace.getRootNode() != null ? ((INodeObject) oldSpace.getRootNode()).getNode().getId() : 0l;

        if (migrationSchema.getRoot() != null)
            migrateNode(migrationSchema, migrationSchema.getRoot(), (ObjectNode) ((INodeObject) oldSpace.getRootNode()).getNode(),
                    (ObjectNode) ((INodeObject) newSpace.getRootNode()).getNode());

        for (INodeObject oldObject : oldSpace.<INodeObject>getNodes()) {
            ObjectNode oldNode = (ObjectNode) oldObject.getNode();
            if (oldNode.getId() == rootNodeId)
                continue;

            INodeMigrationSchema nodeMigrationSchema = migrationSchema.getNodes().get(oldNode.getSchema().getIndex());
            if (nodeMigrationSchema == null)
                continue;

            Object key = null;
            if (nodeMigrationSchema.getPrimaryField() == null)
                continue;

            IFieldMigrationSchema primaryFieldMigrationSchema = nodeMigrationSchema.getPrimaryField();
            IPrimaryFieldConverter converter = (IPrimaryFieldConverter) primaryFieldMigrationSchema.getConverter();
            IField oldField = oldNode.getFieldInstance(primaryFieldMigrationSchema.getOldSchema().getIndex());
            key = converter.convert(oldField, primaryFieldMigrationSchema);

            INodeObject newObject = newSpace.findOrCreateMigratedNode(key, nodeMigrationSchema.getNewSchema());
            ObjectNode newNode = (ObjectNode) newObject.getNode();

            migrateNode(migrationSchema, nodeMigrationSchema, oldNode, newNode);
        }

        for (INodeObject newObject : newSpace.<INodeObject>getNodes())
            newObject.onMigrated();
    }

    private ObjectSpace readSpace() {
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        int spaceFileIndex = readSpaceFileIndex(transaction);

        if (spaceFileIndex != 0) {
            space = ObjectSpace.open(context.getTransactionProvider(), spaceFileIndex, this, extension.getNodeManager(),
                    extension.getNodeCacheManager(), indexFileIndexesFileOffset);

            return space;
        } else
            return null;
    }

    private void removeSpaceFiles(IRawTransaction transaction, int fileIndex) {
        transaction.getFile(fileIndex).delete();

        String filePrefix = ObjectSpaces.getObjectSpacePrefix(getParent().getConfiguration().getName(), (ObjectSpaceSchemaConfiguration) configuration, fileIndex);

        IIndexManager indexManager = context.findTransactionExtension(IIndexManager.NAME);
        for (IIndexManager.IndexInfo info : indexManager.getIndexes()) {
            if (info.filePrefix.startsWith(filePrefix))
                indexManager.getIndex(info.id).delete();
        }

        List<String> paths = context.getConfiguration().getPaths();
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            File file = new File(path, Spaces.getSpaceFilesDirName(filePrefix));
            if (file.exists()) {
                List<Pair<String, Integer>> spaceFiles = new ArrayList<Pair<String, Integer>>();
                Spaces.getSpaceFiles(file, path, spaceFiles);
                for (Pair<String, Integer> pair : spaceFiles) {
                    RawBindInfo info = new RawBindInfo();
                    info.setName(pair.getKey());
                    info.setPathIndex(i);
                    info.setFlags(RawBindInfo.TEMPORARY);
                    transaction.bindFile(pair.getValue(), info).delete();
                }
            }
        }
    }

    private SpaceMigrationSchema createSpaceMigrationSchema(ObjectSpaceSchema oldSchema,
                                                            ObjectSpaceSchema newSchema, IObjectMigrator dataMigrator) {
        ObjectSpaceSchemaConfiguration oldConfiguration = oldSchema.getConfiguration();
        ObjectSpaceSchemaConfiguration newConfiguration = newSchema.getConfiguration();

        INodeMigrationSchema root = null;
        if (oldConfiguration.getRootNodeType() != null && Objects.equals(oldConfiguration.getRootNodeType(), newConfiguration.getRootNodeType())) {
            INodeSchema oldNode = oldSchema.findNode(oldConfiguration.getRootNodeType());
            INodeSchema newNode = newSchema.findNode(newConfiguration.getRootNodeType());

            root = createNodeMigrationSchema(oldNode, newNode, dataMigrator);
        }

        List<INodeMigrationSchema> nodes = new ArrayList<INodeMigrationSchema>(oldSchema.getNodes().size());
        for (int i = 0; i < oldSchema.getNodes().size(); i++) {
            INodeSchema oldNode = oldSchema.getNodes().get(i);
            INodeSchema newNode = newSchema.findNode(oldNode.getConfiguration().getName());
            if (newNode != null)
                nodes.add(createNodeMigrationSchema(oldNode, newNode, dataMigrator));
            else
                nodes.add(null);
        }

        return new SpaceMigrationSchema(oldSchema, newSchema, root, nodes);
    }

    private NodeMigrationSchema createNodeMigrationSchema(INodeSchema oldSchema, INodeSchema newSchema, IObjectMigrator dataMigrator) {
        List<IFieldMigrationSchema> fields = new ArrayList<IFieldMigrationSchema>(oldSchema.getFields().size());

        IFieldMigrationSchema primaryField = null;
        for (int i = 0; i < oldSchema.getFields().size(); i++) {
            IFieldSchema oldField = oldSchema.getFields().get(i);
            IFieldSchema newField = newSchema.findField(oldField.getConfiguration().getName());
            if (newField == null) {
                fields.add(null);
                continue;
            }

            IFieldMigrationSchema fieldMigrationSchema = createFieldMigrationSchema(oldField, newField, dataMigrator);

            if (newField.getConfiguration().isPrimary()) {
                primaryField = fieldMigrationSchema;
                fields.add(null);
            } else
                fields.add(fieldMigrationSchema);
        }

        return new NodeMigrationSchema(oldSchema, newSchema, primaryField, fields);
    }

    private FieldMigrationSchema createFieldMigrationSchema(IFieldSchema oldSchema, IFieldSchema newSchema, IObjectMigrator dataMigrator) {
        IFieldConverter converter;
        if (dataMigrator != null && dataMigrator.supports(oldSchema, newSchema))
            converter = dataMigrator.createConverter(oldSchema, newSchema);
        else
            converter = oldSchema.getConfiguration().createConverter(newSchema.getConfiguration());

        return new FieldMigrationSchema(oldSchema, newSchema, converter);
    }

    public static void migrateNode(ISpaceMigrationSchema spaceMigrationSchema, INodeMigrationSchema migrationSchema,
                                   ObjectNode oldNode, ObjectNode newNode) {
        List<IFieldMigrationSchema> fields = migrationSchema.getFields();
        for (int i = 0; i < fields.size(); i++) {
            IFieldMigrationSchema fieldMigrationSchema = fields.get(i);
            if (fieldMigrationSchema == null)
                continue;

            IField oldField = oldNode.getFieldInstance(i);
            IField newField = newNode.getFieldInstance(fieldMigrationSchema.getNewSchema().getIndex());
            fieldMigrationSchema.getConverter().convert(oldField, newField, fieldMigrationSchema);
        }
    }

    private boolean isCompatible(INodeSchema oldNode, INodeSchema newNode, IObjectMigrator dataMigrator) {
        for (IFieldSchema oldField : oldNode.getFields()) {
            IFieldSchema newField = newNode.findField(oldField.getConfiguration().getName());
            if (newField != null) {
                if (dataMigrator != null && dataMigrator.supports(oldField, newField)) {
                    if (!dataMigrator.isCompatible(oldField, newField))
                        return false;
                } else if (!oldField.getConfiguration().isCompatible(newField.getConfiguration()))
                    return false;
            }
        }
        return true;
    }

    private static class SpaceMigrationSchema implements ISpaceMigrationSchema {
        private final ObjectSpaceSchema oldSchema;
        private final ObjectSpaceSchema newSchema;
        private final INodeMigrationSchema root;
        private final List<INodeMigrationSchema> nodes;

        public SpaceMigrationSchema(ObjectSpaceSchema oldSchema, ObjectSpaceSchema newSchema,
                                    INodeMigrationSchema root, List<INodeMigrationSchema> nodes) {
            Assert.notNull(oldSchema);
            Assert.notNull(newSchema);
            Assert.notNull(nodes);

            this.oldSchema = oldSchema;
            this.newSchema = newSchema;

            if (root != null)
                ((NodeMigrationSchema) root).setSpace(this);

            this.root = root;

            for (INodeMigrationSchema node : nodes) {
                if (node != null)
                    ((NodeMigrationSchema) node).setSpace(this);
            }

            this.nodes = Immutables.wrap(nodes);
        }

        @Override
        public ObjectSpaceSchema getOldSchema() {
            return oldSchema;
        }

        @Override
        public ObjectSpaceSchema getNewSchema() {
            return newSchema;
        }

        @Override
        public INodeMigrationSchema getRoot() {
            return root;
        }

        @Override
        public List<INodeMigrationSchema> getNodes() {
            return nodes;
        }
    }

    private static class NodeMigrationSchema implements INodeMigrationSchema {
        private final INodeSchema oldSchema;
        private final INodeSchema newSchema;
        private final IFieldMigrationSchema primaryField;
        private final List<IFieldMigrationSchema> fields;
        private ISpaceMigrationSchema space;

        public NodeMigrationSchema(INodeSchema oldSchema, INodeSchema newSchema,
                                   IFieldMigrationSchema primaryField, List<IFieldMigrationSchema> fields) {
            Assert.notNull(oldSchema);
            Assert.notNull(newSchema);
            Assert.notNull(fields);

            this.oldSchema = oldSchema;
            this.newSchema = newSchema;

            if (primaryField != null)
                ((FieldMigrationSchema) primaryField).setNode(this);

            this.primaryField = primaryField;

            for (IFieldMigrationSchema field : fields) {
                if (field != null)
                    ((FieldMigrationSchema) field).setNode(this);
            }

            this.fields = Immutables.wrap(fields);
        }

        public void setSpace(ISpaceMigrationSchema space) {
            this.space = space;
        }

        @Override
        public ISpaceMigrationSchema getSpace() {
            return space;
        }

        @Override
        public INodeSchema getOldSchema() {
            return oldSchema;
        }

        @Override
        public INodeSchema getNewSchema() {
            return newSchema;
        }

        @Override
        public IFieldMigrationSchema getPrimaryField() {
            return primaryField;
        }

        @Override
        public List<IFieldMigrationSchema> getFields() {
            return fields;
        }
    }

    private static class FieldMigrationSchema implements IFieldMigrationSchema {
        private final IFieldSchema oldSchema;
        private final IFieldSchema newSchema;
        private final IFieldConverter converter;
        private INodeMigrationSchema node;

        public FieldMigrationSchema(IFieldSchema oldSchema, IFieldSchema newSchema, IFieldConverter converter) {
            Assert.notNull(oldSchema);
            Assert.notNull(newSchema);
            Assert.notNull(converter);

            this.oldSchema = oldSchema;
            this.newSchema = newSchema;
            this.converter = converter;
        }

        public void setNode(INodeMigrationSchema node) {
            this.node = node;
        }

        @Override
        public INodeMigrationSchema getNode() {
            return node;
        }

        @Override
        public IFieldSchema getOldSchema() {
            return oldSchema;
        }

        @Override
        public IFieldSchema getNewSchema() {
            return newSchema;
        }

        @Override
        public IFieldConverter getConverter() {
            return converter;
        }
    }
}
