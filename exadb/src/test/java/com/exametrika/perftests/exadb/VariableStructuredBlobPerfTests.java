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
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.VariableStructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField.IElementIterable;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


/**
 * The {@link VariableStructuredBlobPerfTests} are perftests for variable structured blob field.
 *
 * @author Medvedev-A
 */
public class VariableStructuredBlobPerfTests {
    private static final int COUNT = 10000;
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
            }
        });

        final long[] rids = new long[COUNT];
        final long[] eids = new long[COUNT * 100];
        long t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IVariableStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (int i = 0; i < COUNT; i++) {
                    rids[i] = field.addRecord();

                    for (int k = 0; k < 100; k++) {
                        int n = i * 100 + k;
                        eids[n] = field.addElement(rids[i], new TestRecord("test-" + n, n));
                    }
                }
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test addition: " + t);

        t = Times.getCurrentTime();
        final int r[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IVariableStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (IElementIterable<TestRecord> it : field.getRecords())
                    for (@SuppressWarnings("unused") TestRecord record : it)
                        r[0]++;
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test iteration: " + t + ", " + r[0]);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.findNodeById(ids[1]);
                IVariableStructuredBlobField<TestRecord> field = node.node.getField(1);

                for (int i = eids.length - 1; i >= 0; i--)
                    field.getElement(eids[i]);
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
                        new VariableStructuredBlobFieldSchemaConfiguration("field2", "node1", "field2")));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

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
        private static final UUID ID = UUID.fromString("6cbc827e-0900-4d62-9c79-1b71459e9fc6");

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
}
