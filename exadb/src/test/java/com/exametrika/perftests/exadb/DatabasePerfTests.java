/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.config.schema.BinaryFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IBinaryField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ITextField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.schema.BlobFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


/**
 * The {@link DatabasePerfTests} are performance tests for exa database framework.
 *
 * @author Medvedev-A
 */
public class DatabasePerfTests {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(DatabasePerfTests.class);
    private Database database;
    private DatabaseConfiguration parameters;
    private DatabaseConfigurationBuilder builder;

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(200000000)).toConfiguration());
        parameters = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(null, parameters);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testDataPerformance() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node1"), new PrimitiveFieldSchemaConfiguration("field", DataType.INT),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 100000;
        final int COUNT2 = 100;
        final long[] nodeIds = new long[COUNT];

        logger.log(LogLevel.INFO, messages.addNodesFlush(new Benchmark(new Probe() {
            @Override
            public void runOnce() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        final ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        logger.log(LogLevel.INFO, messages.separator());
                        logger.log(LogLevel.INFO, messages.addNodes(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int i = 0; i < COUNT; i++) {
                                    TestNode node = space.addNode(i, 0);
                                    nodeIds[i] = node.node.getId();
                                }
                            }
                        }, 1, 0)));
                    }
                });

            }
        }, 1, 0)));

        logger.log(LogLevel.INFO, messages.addReferencesFlush(new Benchmark(new Probe() {
            @Override
            public void runOnce() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        final ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        logger.log(LogLevel.INFO, messages.separator());
                        logger.log(LogLevel.INFO, messages.findNodesByLocators(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int k = 0; k < COUNT2; k++)
                                    for (int i = 0; i < COUNT; i++)
                                        Assert.notNull(space.findNodeById(nodeIds[i]));
                            }
                        }, 1, 0)));
                        logger.log(LogLevel.INFO, messages.findNodesByIds(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int k = 0; k < COUNT2; k++)
                                    for (int i = 0; i < COUNT; i++)
                                        Assert.notNull(space.findNodeById(nodeIds[i]));
                            }
                        }, 1, 0)));
                        logger.log(LogLevel.INFO, messages.nodePeriodIteratorHot(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int k = 0; k < COUNT2; k++)
                                    for (TestNode node : space.<TestNode>getNodes())
                                        Assert.notNull(node);
                            }
                        }, 1, 0)));

                        TestNode node = space.findNodeById(nodeIds[0]);
                        final IReferenceField<TestNode> field = node.node.getField(0);
                        logger.log(LogLevel.INFO, messages.addReferences(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int i = 0; i < COUNT; i++) {
                                    TestNode node2 = space.findNodeById(nodeIds[i]);
                                    field.add(node2);
                                }
                            }
                        }, 1, 0)));
                    }
                });
            }
        }, 1, 0)));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                final ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                TestNode node = space.findNodeById(nodeIds[0]);
                final IReferenceField<TestNode> field = node.node.getField(0);

                logger.log(LogLevel.INFO, messages.referencesIteratorHot(new Benchmark(new Probe() {
                    @Override
                    public void runOnce() {
                        for (int k = 0; k < COUNT2; k++)
                            for (TestNode node : field)
                                Assert.notNull(node);
                    }
                }, 1, 0)));
            }
        });

        database.close();
        database = new DatabaseFactory().createDatabase(null, parameters);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                final IObjectSpace space2 = dataSchema.getSpace();

                logger.log(LogLevel.INFO, messages.nodePeriodIteratorCold(new Benchmark(new Probe() {
                    @Override
                    public void runOnce() {
                        for (TestNode node : space2.<TestNode>getNodes())
                            Assert.notNull(node);
                    }
                }, 1, 0)));
                logger.log(LogLevel.INFO, messages.referencesIteratorCold(new Benchmark(new Probe() {
                    @Override
                    public void runOnce() {
                        int i = 0;
                        TestNode node2 = space2.findNodeById(nodeIds[0]);
                        final IReferenceField<TestNode> field2 = node2.node.getField(0);

                        for (TestNode node : field2)
                            Assert.isTrue(node.node.getId() == nodeIds[i++]);
                        Assert.isTrue(i == COUNT);
                    }
                }, 1, 0)));
            }
        });
    }

    @Test
    public void testDataScalability() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node1"),
                        new PrimitiveFieldSchemaConfiguration("field", DataType.INT),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        long l = Times.getCurrentTime();
        for (int m = 0; m < 100; m++) {
            boolean sync = false;
            if (m > 0 && (m % 10) == 0)
                sync = true;

            final int k = m;
            IOperation operation = new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    long t = Times.getCurrentTime();

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    for (int i = 0; i < 100000; i++)
                        space.addNode(k * 100000 + i, 0);

                    System.out.println(Times.getCurrentTime() - t + " " + k);
                }

                @Override
                public void onRolledBack() {
                }

                @Override
                public void onCommitted() {
                }
            };
            if (sync)
                database.transactionSync(operation);
            else
                database.transaction(operation);
        }

        System.out.println("Write nodes: " + (Times.getCurrentTime() - l));

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                long t = Times.getCurrentTime();

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                int k = 0;
                for (@SuppressWarnings("unused") TestNode node : space.<TestNode>getNodes())
                    k++;

                System.out.println("Read nodes: " + (Times.getCurrentTime() - t) + " " + k);
            }

            @Override
            public void onRolledBack() {
            }

            @Override
            public void onCommitted() {
            }
        });
    }

    @Test
    public void testLargeDataScalability() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node1"),
                        new PrimitiveFieldSchemaConfiguration("field", DataType.INT),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        long l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                long t = Times.getCurrentTime();

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                for (int i = 0; i < 10000000; i++) {
                    space.addNode(i, 0);

                    if (i != 0 && (i % 100000) == 0) {
                        System.out.println(Times.getCurrentTime() - t + " " + i);
                        t = Times.getCurrentTime();
                    }
                }

                System.out.println(Times.getCurrentTime() - t + " " + 100000000);
            }

            @Override
            public void onRolledBack() {
            }

            @Override
            public void onCommitted() {
            }
        });

        System.out.println("Write nodes: " + (Times.getCurrentTime() - l));

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                long t = Times.getCurrentTime();

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                int k = 0;
                for (@SuppressWarnings("unused") TestNode node : space.<TestNode>getNodes()) {
                    if (k != 0 && (k % 100000) == 0) {
                        System.out.println(Times.getCurrentTime() - t + " " + k);
                        t = Times.getCurrentTime();
                    }
                    k++;
                }

                System.out.println("Read nodes: " + (Times.getCurrentTime() - t) + " " + k);
            }

            @Override
            public void onRolledBack() {
            }

            @Override
            public void onCommitted() {
            }
        });
    }

    @Test
    public void testBlobPerformance() {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TestBlobFieldSchemaConfiguration("field3", "node1", "field2")));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final long[] ids = new long[COUNT + 1];
        long t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.addNode(0, 0);
                ids[0] = node1.getId();
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.addNode(i, 1);
                    ids[i] = node2.getId();
                    IBlobField field2 = node2.getField(1);
                    field2.set(field1.createBlob());

                    IBlobSerialization serialization2 = field2.get().createSerialization();

                    for (int k = 0; k < 100; k++)
                        serialization2.writeByteArray(createBuffer(1000, 567 + i * 100 + k));
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Write blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    IBlobField field2 = node2.getField(1);

                    IBlobDeserialization deserialization2 = field2.get().createDeserialization();

                    for (int k = 0; k < 100; k++)
                        Assert.isTrue(deserialization2.readByteArray().equals(createBuffer(1000, 567 + i * 100 + k)));
                }
            }
        });
        System.out.println("Read blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    IBlobField field2 = node2.getField(1);
                    field2.get().delete();
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Delete blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    IBlobField field2 = node2.getField(1);
                    field2.set(field1.createBlob());

                    IBlobSerialization serialization2 = field2.get().createSerialization();

                    for (int k = 0; k < 100; k++)
                        serialization2.writeByteArray(createBuffer(1000, 567 + i * 100 + k));
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Write blobs2: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    IBlobField field2 = node2.getField(1);

                    IBlobDeserialization deserialization2 = field2.get().createDeserialization();

                    for (int k = 0; k < 100; k++)
                        Assert.isTrue(deserialization2.readByteArray().equals(createBuffer(1000, 567 + i * 100 + k)));
                }
            }
        });
        System.out.println("Read blobs2: " + (Times.getCurrentTime() - t));
    }

    @Test
    public void testBinaryBlobPerformance() {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BinaryFieldSchemaConfiguration("field3", "field3", null, "node1", "field2", false, false)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        System.out.println("Uncompressed binary blob performance:");
        testBinaryBlobPerformance(space1, 0);

        nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BinaryFieldSchemaConfiguration("field3", "field3", null, "node1", "field2", false, true)));
        space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        System.out.println("\nCompressed binary blob performance:");
        testBinaryBlobPerformance(space1, 1);
    }

    @Test
    public void testTextBlobPerformance() {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TextFieldSchemaConfiguration("field3", "field3", null, "node1", "field2", false, false)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        System.out.println("Uncompressed text blob performance:");
        testTextBlobPerformance(space1, 0);

        nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TextFieldSchemaConfiguration("field3", "field3", null, "node1", "field2", false, true)));
        space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        System.out.println("\nCompressed text blob performance:");
        testTextBlobPerformance(space1, 1);
    }

    private void testBinaryBlobPerformance(ObjectSpaceSchemaConfiguration space1, final int version) {
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, version), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final long[] ids = new long[COUNT + 1];
        long t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.addNode(0, 0);
                ids[0] = node1.getId();
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.addNode(i, 1);
                        ids[i] = node2.getId();
                        IBinaryField field2 = node2.getField(1);
                        field2.setStore(node1);

                        OutputStream stream = field2.createOutputStream();

                        for (int k = 0; k < 100; k++) {
                            ByteArray buf = createBuffer(1000, 567 + i * 100 + k);
                            stream.write(buf.getBuffer(), buf.getOffset(), buf.getLength());
                        }

                        stream.close();
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Write blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.findNodeById(ids[i]);
                        IBinaryField field2 = node2.getField(1);

                        InputStream stream = field2.createInputStream();

                        for (int k = 0; k < 100; k++)
                            read(stream, createBuffer(1000, 567 + i * 100 + k));
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
        System.out.println("Read blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    IBinaryField field2 = node2.getField(1);
                    field2.setStore(null);
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Delete blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.findNodeById(ids[i]);
                        IBinaryField field2 = node2.getField(1);
                        field2.setStore(node1);

                        OutputStream stream = field2.createOutputStream();

                        for (int k = 0; k < 100; k++) {
                            ByteArray buf = createBuffer(1000, 567 + i * 100 + k);
                            stream.write(buf.getBuffer(), buf.getOffset(), buf.getLength());
                        }

                        stream.close();
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Write blobs2: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.findNodeById(ids[i]);
                        IBinaryField field2 = node2.getField(1);

                        InputStream stream = field2.createInputStream();

                        for (int k = 0; k < 100; k++)
                            read(stream, createBuffer(1000, 567 + i * 100 + k));
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
        System.out.println("Read blobs2: " + (Times.getCurrentTime() - t));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                node1.delete();

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    node2.delete();
                }
            }
        });
    }

    private void testTextBlobPerformance(ObjectSpaceSchemaConfiguration space1, final int version) {
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, version), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final long[] ids = new long[COUNT + 1];
        long t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.addNode(0, 0);
                ids[0] = node1.getId();
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.addNode(i, 1);
                        ids[i] = node2.getId();
                        ITextField field2 = node2.getField(1);
                        field2.setStore(node1);

                        Writer stream = field2.createWriter();

                        for (int k = 0; k < 50; k++) {
                            String buf = createString(1000, 567 + i * 100 + k);
                            stream.write(buf);
                        }

                        stream.close();
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Write blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.findNodeById(ids[i]);
                        ITextField field2 = node2.getField(1);

                        Reader stream = field2.createReader();

                        for (int k = 0; k < 50; k++)
                            read(stream, createString(1000, 567 + i * 100 + k));
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
        System.out.println("Read blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    ITextField field2 = node2.getField(1);
                    field2.setStore(null);
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Delete blobs: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.findNodeById(ids[i]);
                        ITextField field2 = node2.getField(1);
                        field2.setStore(node1);

                        Writer stream = field2.createWriter();

                        for (int k = 0; k < 50; k++) {
                            String buf = createString(1000, 567 + i * 100 + k);
                            stream.write(buf);
                        }

                        stream.close();
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }

                System.out.println("free space:" + field1.getFreeSpace());
            }
        });
        System.out.println("Write blobs2: " + (Times.getCurrentTime() - t));

        t = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                IBlobStoreField field1 = node1.getField(1);
                System.out.println("free space:" + field1.getFreeSpace());

                try {
                    for (int i = 1; i <= COUNT; i++) {
                        IObjectNode node2 = space.findNodeById(ids[i]);
                        ITextField field2 = node2.getField(1);

                        Reader stream = field2.createReader();

                        for (int k = 0; k < 50; k++)
                            read(stream, createString(1000, 567 + i * 100 + k));
                    }
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
        System.out.println("Read blobs2: " + (Times.getCurrentTime() - t));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                IObjectNode node1 = space.findNodeById(ids[0]);
                node1.delete();

                for (int i = 1; i <= COUNT; i++) {
                    IObjectNode node2 = space.findNodeById(ids[i]);
                    node2.delete();
                }
            }
        });
    }

    private ByteArray createBuffer(int size, int base) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++)
            b[i] = (byte) (i + base);

        return new ByteArray(b);
    }

    private String createString(int size, int base) {
        char[] b = new char[size];
        for (int i = 0; i < size; i++)
            b[i] = (char) (128 + (byte) (i + base));

        return new String(b);
    }

    private void read(InputStream stream, ByteArray buffer) throws IOException {
        byte[] b = new byte[buffer.getLength()];
        int read = 0;
        while (read < b.length) {
            int n = stream.read(b, read, b.length - read);
            Assert.checkState(n != -1);
            read += n;
        }

        assertThat(new ByteArray(b), is(buffer));
    }

    private void read(Reader reader, String buffer) throws IOException {
        char[] b = new char[buffer.length()];
        int read = 0;
        while (read < b.length) {
            int n = reader.read(b, read, b.length - read);
            Assert.checkState(n != -1);
            read += n;
        }

        assertThat(new String(b), is(buffer));
    }

    public static class TestBlobFieldSchemaConfiguration extends BlobFieldSchemaConfiguration {
        public TestBlobFieldSchemaConfiguration(String name, String blobStoreNodeType, String blobStoreFieldName) {
            this(name, name, null, blobStoreNodeType, blobStoreFieldName, false);
        }

        public TestBlobFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType, String blobStoreFieldName,
                                                boolean required) {
            super(name, alias, description, blobStoreNodeType, blobStoreFieldName, required);
        }

        @Override
        public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
            return new BlobFieldSchema(this, index, offset);
        }

        @Override
        public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
            return false;
        }

        @Override
        public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestBlobFieldSchemaConfiguration))
                return false;

            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }
    }

    private interface IMessages {
        @DefaultMessage("Add nodes ''{0}''.")
        ILocalizedMessage addNodes(Object benchmark);

        @DefaultMessage("References iterator hot''{0}''.")
        ILocalizedMessage referencesIteratorHot(Benchmark benchmark);

        @DefaultMessage("References iterator cold''{0}''.")
        ILocalizedMessage referencesIteratorCold(Benchmark benchmark);

        @DefaultMessage("Add references flush ''{0}''.")
        ILocalizedMessage addReferencesFlush(Benchmark benchmark);

        @DefaultMessage("Add references ''{0}''.")
        ILocalizedMessage addReferences(Benchmark benchmark);

        @DefaultMessage("Node period iterator hot ''{0}''.")
        ILocalizedMessage nodePeriodIteratorHot(Benchmark benchmark);

        @DefaultMessage("Node period iterator cold ''{0}''.")
        ILocalizedMessage nodePeriodIteratorCold(Benchmark benchmark);

        @DefaultMessage("Find nodes by ids ''{0}''.")
        ILocalizedMessage findNodesByIds(Benchmark benchmark);

        @DefaultMessage("Find nodes by locators ''{0}''.")
        ILocalizedMessage findNodesByLocators(Benchmark benchmark);

        @DefaultMessage("Add nodes flush ''{0}''.")
        ILocalizedMessage addNodesFlush(Object benchmark);

        @DefaultMessage("====================================================================")
        ILocalizedMessage separator();
    }
}