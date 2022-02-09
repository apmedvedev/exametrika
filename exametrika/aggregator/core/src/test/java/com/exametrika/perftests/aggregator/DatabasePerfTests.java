/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.aggregator;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
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
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.schema.BlobFieldSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.PeriodTestNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


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
    public void testPeriodPerformance() throws Throwable {
        PeriodTestNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node1")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 10000000, 2, false, null)), 0, 0);

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
                        IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                        CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                        PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                        final Period period = cycle.getCurrentPeriod();

                        logger.log(LogLevel.INFO, messages.separator());
                        logger.log(LogLevel.INFO, messages.addNodes(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int i = 0; i < COUNT; i++) {
                                    TestPeriodNode node = period.addNode(new Location(1, i + 1), 0);
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
                        IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                        CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                        PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                        final Period period = cycle.getCurrentPeriod();
                        final INodeIndex<Location, TestPeriodNode> index = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));

                        logger.log(LogLevel.INFO, messages.separator());
                        logger.log(LogLevel.INFO, messages.findNodesByLocators(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int k = 0; k < COUNT2; k++)
                                    for (int i = 0; i < COUNT; i++)
                                        Assert.notNull(index.find(new Location(1, i + 1)));
                            }
                        }, 1, 0)));
                        logger.log(LogLevel.INFO, messages.findNodesByIds(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int k = 0; k < COUNT2; k++)
                                    for (int i = 0; i < COUNT; i++)
                                        Assert.notNull(period.findNodeById(nodeIds[i]));
                            }
                        }, 1, 0)));
                        logger.log(LogLevel.INFO, messages.nodePeriodIteratorHot(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int k = 0; k < COUNT2; k++)
                                    for (TestPeriodNode node : period.<TestPeriodNode>getNodes())
                                        Assert.notNull(node);
                            }
                        }, 1, 0)));

                        TestPeriodNode node = period.findNodeById(nodeIds[0]);
                        final IReferenceField<TestPeriodNode> field = node.node.getField(1);
                        logger.log(LogLevel.INFO, messages.addReferences(new Benchmark(new Probe() {
                            @Override
                            public void runOnce() {
                                for (int i = 0; i < COUNT; i++) {
                                    TestPeriodNode node2 = period.findNodeById(nodeIds[i]);
                                    field.add(node2);
                                }
                            }
                        }, 1, 0)));

                        cycle.addPeriod();
                    }
                });
            }
        }, 1, 0)));

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                final Period period = cycle.getCurrentPeriod();

                TestPeriodNode node = period.findNodeById(nodeIds[0]);
                final IReferenceField<TestPeriodNode> field = node.node.getField(1);
                logger.log(LogLevel.INFO, messages.referencesIteratorHot(new Benchmark(new Probe() {
                    @Override
                    public void runOnce() {
                        for (int k = 0; k < COUNT2; k++)
                            for (TestPeriodNode node : field)
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
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                final Period period2 = cycle.getCurrentPeriod();

                TestPeriodNode node2 = period2.findNodeById(nodeIds[0]);
                final IReferenceField<TestPeriodNode> field2 = node2.node.getField(1);

                logger.log(LogLevel.INFO, messages.nodePeriodIteratorCold(new Benchmark(new Probe() {
                    @Override
                    public void runOnce() {
                        for (TestPeriodNode node : period2.<TestPeriodNode>getNodes())
                            Assert.notNull(node);
                    }
                }, 1, 0)));
                logger.log(LogLevel.INFO, messages.referencesIteratorCold(new Benchmark(new Probe() {
                    @Override
                    public void runOnce() {
                        int i = 0;
                        for (TestPeriodNode node : field2)
                            Assert.isTrue(node.node.getId() == nodeIds[i++]);
                        Assert.isTrue(i == COUNT);
                    }
                }, 1, 0)));
            }
        });
    }

    @Test
    public void testPeriodScalability() throws Throwable {
        PeriodTestNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node1")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 10000, false, null)), 0, 0);

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

                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    for (int i = 0; i < 100000; i++)
                        period.addNode(new Location(i + 1, i + 1), 0);

                    cycle.addPeriod();

                    System.out.println(Times.getCurrentTime() - t + " " + k);

                    database.printStatistics();
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

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                int k = 0;
                for (int i = 0; i < cycle.getPeriodsCount(); i++) {
                    Period period = cycle.getPeriod(i);

                    for (@SuppressWarnings("unused") TestPeriodNode node : period.<TestPeriodNode>getNodes())
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
    public void testPeriodIndexedPerformance() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                java.util.Collections.<FieldSchemaConfiguration>emptyList());

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);

                for (int i = 0; i < 10000000; i++) {
                    period.addNode(new Location(i + 2, i + 2), 0);
                    node1.node.refresh();
                    if (i > 0 && (i % 10000) == 0) {
                        System.out.println(i);
                        database.printStatistics();
                    }
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