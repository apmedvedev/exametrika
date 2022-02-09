/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.exadb;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.RecordIndexerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


/**
 * The {@link StructuredBlobPerfTests} are perftests for structured blob field.
 *
 * @author Medvedev-A
 */
public class StructuredBlobPerfTests {
    private static final int COUNT = 1000000;
    private Database database;
    private DatabaseConfiguration configuration;
    private IDatabaseFactory.Parameters parameters;

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(1000000);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(100000000)).toConfiguration());
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put("disableModules", true);
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testPerformance() {
        createDatabase();

        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(1, 0);
                ids[0] = node1.node.getId();

                TestNode node2 = space.addNode(2, 1, node1);
                ids[1] = node2.node.getId();

                TestNode node3 = space.addNode(3, 2, node1);
                ids[2] = node3.node.getId();

                TestNode node4 = space.addNode(4, 3, node1);
                ids[3] = node4.node.getId();
            }
        });

        final long[] rids = new long[COUNT / 100];
        long t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (int i = 0; i < COUNT; i++) {
                    long rid = field.add(new TestRecord("test-" + i, i));
                    if (i < rids.length)
                        rids[i] = rid;
                }
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test addition without indexes: " + t);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[2]);
                IStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (int i = 0; i < COUNT; i++)
                    field.add(new TestRecord("test-" + i, i));
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test addition with 2 native indexes: " + t);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[3]);
                IStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (int i = 0; i < COUNT; i++)
                    field.add(new TestRecord("test-" + i, i));
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test addition with full text index: " + t);

        t = Times.getCurrentTime();
        final int r[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (@SuppressWarnings("unused") TestRecord record : field.getRecords())
                    r[0]++;
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test iteration: " + t + ", " + r[0]);

        t = Times.getCurrentTime();
        final int rr[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (@SuppressWarnings("unused") TestRecord record : field.getReverseRecords())
                    rr[0]++;
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test reverse iteration: " + t + ", " + rr[0]);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (int i = 0; i < 100; i++)
                    for (int k = rids.length - 1; k >= 0; k--)
                        field.get(rids[k]);
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test find: " + t);
    }

    private void createDatabase() {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1",
                                PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "field2", null, 0, Integer.MAX_VALUE, null, PageType.NORMAL,
                                false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new StructuredBlobFieldSchemaConfiguration("field2", "node1", "field2")));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new StructuredBlobFieldSchemaConfiguration("field2", "field2", null, "node1", "field2", true, true,
                                Collections.singleton(TestRecord.class.getName()), false, 0,
                                Arrays.asList(new StructuredBlobIndexSchemaConfiguration("index1", 0, IndexType.BTREE,
                                                false, 256, new StringKeyNormalizerSchemaConfiguration(), true, true, null),
                                        new StructuredBlobIndexSchemaConfiguration("index2", 0, IndexType.BTREE,
                                                true, 8, new NumericKeyNormalizerSchemaConfiguration(NumericKeyNormalizerSchemaConfiguration.DataType.LONG),
                                                true, true, null)), false, new TestRecordIndexerSchemaConfiguration(true, false))));
        NodeSchemaConfiguration nodeConfiguration4 = new TestNodeSchemaConfiguration("node4",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new StructuredBlobFieldSchemaConfiguration("field2", "field2", null, "node1", "field2", true, true,
                                Collections.singleton(TestRecord.class.getName()), false, 0,
                                Collections.<StructuredBlobIndexSchemaConfiguration>emptyList(), true, new TestRecordIndexerSchemaConfiguration(false, true))));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3, nodeConfiguration4)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });
    }

    private static class TestRecord {
        private final String field1;
        private final long field2;

        public TestRecord(String field1, long field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRecord))
                return false;

            TestRecord record = (TestRecord) o;
            return field1.equals(record.field1) && field2 == record.field2;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(field1, field2);
        }
    }

    public static final class TestRecordSerializer extends AbstractSerializer {
        private static final UUID ID = UUID.fromString("61673251-c6c8-4d57-96cc-3e5f88d55a77");

        public TestRecordSerializer() {
            super(ID, TestRecord.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object) {
            TestRecord record = (TestRecord) object;
            serialization.writeString(record.field1);
            serialization.writeLong(record.field2);
        }

        @Override
        public Object deserialize(IDeserialization deserialization, UUID id) {
            String field1 = deserialization.readString();
            long field2 = deserialization.readLong();
            return new TestRecord(field1, field2);
        }
    }

    public static final class TestRecordIndexerSchemaConfiguration extends RecordIndexerSchemaConfiguration {
        private final boolean indexes;
        private final boolean fullText;

        public TestRecordIndexerSchemaConfiguration(boolean indexes, boolean fullText) {
            this.indexes = indexes;
            this.fullText = fullText;
        }

        @Override
        public IRecordIndexer createIndexer(IField field, IRecordIndexProvider indexProvider) {
            return new TestRecordIndexer(this, field, indexProvider);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRecordIndexerSchemaConfiguration))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    private static class TestRecordIndexer implements IRecordIndexer {
        private final IRecordIndexProvider indexProvider;
        private final IDocumentSchema documentSchema;
        private final TestRecordIndexerSchemaConfiguration configuration;

        public TestRecordIndexer(TestRecordIndexerSchemaConfiguration configuration, IField field, IRecordIndexProvider indexProvider) {
            this.configuration = configuration;
            this.indexProvider = indexProvider;
            if (configuration.fullText)
                this.documentSchema = indexProvider.createDocumentSchema(new DocumentSchemaConfiguration("test", Arrays.asList(
                        new StringFieldSchemaConfiguration("field1", Enums.of(Option.INDEX_DOCUMENTS, Option.INDEXED, Option.OMIT_NORMS), null),
                        new NumericFieldSchemaConfiguration("field2", DataType.LONG, true, true)),
                        new StandardAnalyzerSchemaConfiguration()));
            else
                this.documentSchema = null;
        }

        @Override
        public void addRecord(Object r, long id) {
            TestRecord record = (TestRecord) r;

            if (configuration.indexes) {
                indexProvider.add(0, record.field1, id);
                indexProvider.add(1, record.field2, id);
            }
            if (configuration.fullText)
                indexProvider.add(documentSchema, id, record.field1, record.field2);
        }

        @Override
        public void removeRecord(Object r) {
            TestRecord record = (TestRecord) r;

            if (configuration.indexes) {
                indexProvider.remove(0, record.field1);
                indexProvider.remove(1, record.field2);
            }
        }

        @Override
        public void reindex(Object r, long id) {
            TestRecord record = (TestRecord) r;
            indexProvider.add(documentSchema, id, record.field1, record.field2);
        }
    }
}
