/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.IPeriodOperationManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.NameSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodDatabaseExtensionSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.SimpleArchivePolicySchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.SimpleTruncationPolicySchemaConfiguration;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.FileArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.NullArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.fields.IFileField;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodNode;
import com.exametrika.impl.aggregator.nodes.PeriodNodeObject;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * The {@link MultipathTests} are tests for multipath exa database.
 *
 * @author Medvedev-A
 */
public class MultipathTests {
    private Database database;
    private DatabaseConfiguration configuration;
    private IDatabaseFactory.Parameters parameters;
    private File testDir1;
    private long[] ids = new long[4];

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        testDir1 = new File(System.getProperty("java.io.tmpdir"), "db-test1");
        Files.emptyDir(testDir1);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(new File(tempDir, "path1").getPath());
        builder.addPath(new File(tempDir, "path2").getPath());
        builder.addPath(new File(tempDir, "path3").getPath());
        builder.addPath(new File(tempDir, "path4").getPath());
        builder.setTimerPeriod(1000000);
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.initialSchemaProvider = new IInitialSchemaProvider() {
            @Override
            public ModularDatabaseSchemaConfiguration getInitialSchema() {
                return new ModularDatabaseSchemaConfiguration("test", com.exametrika.common.utils.Collections.asSet(new ModuleSchemaConfiguration("test", new Version(1, 0, 0),
                        new DatabaseSchemaConfiguration("test", "test", null, Collections.<DomainSchemaConfiguration>emptySet(),
                                Collections.singleton(
                                        new PeriodDatabaseExtensionSchemaConfiguration(new NameSpaceSchemaConfiguration("test",
                                                1, 2, 256)))))));
            }
        };
        parameters.parameters.put("disableModules", true);
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testPeriodSpace() {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field2"),
                Arrays.asList(new FileFieldSchemaConfiguration("field1", "field1", null, 3, 0, "dir1",
                        PageType.NORMAL, false, Collections.<String, String>emptyMap())), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 2, false, null)), 1, 1);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1),
                        new DatabaseSchemaConfiguration("test", "test", null, Collections.singleton(configuration1),
                                Collections.singleton(
                                        new PeriodDatabaseExtensionSchemaConfiguration(new NameSpaceSchemaConfiguration("test",
                                                1, 2, 256))))), null);
            }
        });

        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node1 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(1, 1), 0)).getNode();
                ids[0] = node1.getId();
                IFileField field1 = node1.getField(1);
                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(1, 1), 0)).getNode();
                ids[1] = node2.getId();
                IFileField field2 = node2.getField(1);
                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    writeRegion(i, page1.getWriteRegion());

                    IRawPage page2 = field2.getPage(i);
                    writeRegion(i, page2.getWriteRegion());
                }

                ((CycleSchema) periodSchema.findCycle("p1")).addPeriod();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node1 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[0])).getNode();
                IFileField field1 = node1.getField(1);
                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[1])).getNode();
                IFileField field2 = node2.getField(1);

                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i));

                    IRawPage page2 = field2.getPage(i);
                    checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i));
                }
            }
        });
    }

    @Test
    public void testSnapshot() throws Throwable {
        createDatabase();

        database.getOperations().snapshot(testDir1.getPath(), null);
        checkSnapshot(testDir1);
    }

    @Test
    public void testArchiving() throws Throwable {
        createDatabase();

        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(null, null,
                new SimpleArchivePolicySchemaConfiguration(1000), new FileArchiveStoreSchemaConfiguration(testDir1.getPath()), null);

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                        if (spaceSchema instanceof IObjectSpaceSchema)
                            continue;

                        if (!(spaceSchema instanceof IPeriodSpaceSchema))
                            Assert.error();

                        for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                            IPeriodCycle cycle = cycleSchema.getCurrentCycle();
                            assertThat(cycle.isArchived(), is(true));
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testTruncation() throws Throwable {
        createDatabase();

        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(null, null,
                new SimpleArchivePolicySchemaConfiguration(1000), new NullArchiveStoreSchemaConfiguration(), null);
        checkDatabase(database);

        database.close();

        Thread.sleep(3000);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).truncateCycles(null, null,
                new SimpleTruncationPolicySchemaConfiguration(0, 0, 0, 0), true, null);

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                        if (spaceSchema instanceof IObjectSpaceSchema)
                            continue;

                        if (!(spaceSchema instanceof IPeriodSpaceSchema))
                            Assert.error();

                        for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                            IPeriodCycle cycle = cycleSchema.getCurrentCycle();
                            assertThat(cycle.isDeleted(), is(true));
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testRestore() {
        createDatabase();

        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(null, null,
                new SimpleArchivePolicySchemaConfiguration(1000), new FileArchiveStoreSchemaConfiguration(testDir1.getPath()), null);
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).truncateCycles(null, null,
                new SimpleTruncationPolicySchemaConfiguration(0, 0, 0, 0), true, null);
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).restoreCycles(
                com.exametrika.common.utils.Collections.asSet("test-space1-p1-14"),
                new FileArchiveStoreSchemaConfiguration(testDir1.getPath()), null);
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                        if (spaceSchema instanceof IObjectSpaceSchema)
                            continue;

                        if (!(spaceSchema instanceof IPeriodSpaceSchema))
                            Assert.error();

                        for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                            IPeriodCycle cycle = cycleSchema.getCurrentCycle();

                            assertThat(cycle.isDeleted(), is(false));
                            assertThat(cycle.isRestored(), is(true));
                        }
                    }
                }
            }
        });

        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).truncateCycles(null, null,
                new SimpleTruncationPolicySchemaConfiguration(0, 0, 0, 0), true, null);

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                        if (spaceSchema instanceof IObjectSpaceSchema)
                            continue;

                        if (!(spaceSchema instanceof IPeriodSpaceSchema))
                            Assert.error();

                        for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                            IPeriodCycle cycle = cycleSchema.getCurrentCycle();

                            assertThat(cycle.isDeleted(), is(false));
                            assertThat(cycle.isRestored(), is(true));
                        }
                    }
                }
            }
        });

        checkDatabase(database);
    }

    private void createDatabase() {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field2", "field2", null, 1),
                Arrays.asList(new FileFieldSchemaConfiguration("field1", "field1", null, 3, 0, "dir1",
                        PageType.NORMAL, false, Collections.<String, String>emptyMap())), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 2, false, null)), 1, 1);

        NodeSchemaConfiguration nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node1",
                Arrays.asList(new FileFieldSchemaConfiguration("field1", "field1", null, 2, 0, "dir1",
                                PageType.NORMAL, false, Collections.<String, String>emptyMap()),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "",
                new HashSet(Arrays.asList(nodeConfiguration2)), null, 1, 1);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1, space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1),
                        new DatabaseSchemaConfiguration("test", "test", null, Collections.singleton(configuration1),
                                Collections.singleton(
                                        new PeriodDatabaseExtensionSchemaConfiguration(new NameSpaceSchemaConfiguration("test",
                                                1, 2, 256))))), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node1 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(1, 1), 0)).getNode();
                ids[2] = node1.getId();
                IFileField field1 = node1.getField(1);
                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(2, 2), 0)).getNode();
                ids[3] = node2.getId();
                IFileField field2 = node2.getField(1);
                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    writeRegion(i, page1.getWriteRegion());

                    IRawPage page2 = field2.getPage(i);
                    writeRegion(i, page2.getWriteRegion());
                }

                ((CycleSchema) periodSchema.findCycle("p1")).addPeriod();

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                ObjectNode node3 = (ObjectNode) ((ObjectNodeObject) space.addNode(123, 0)).getNode();
                ids[0] = node3.getId();
                IFileField field3 = node3.getField(0);
                ObjectNode node4 = (ObjectNode) ((ObjectNodeObject) space.addNode(234, 0)).getNode();
                ids[1] = node4.getId();
                IFileField field4 = node4.getField(0);
                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field3.getPage(i);
                    writeRegion(i, page1.getWriteRegion());

                    IRawPage page2 = field4.getPage(i);
                    writeRegion(i, page2.getWriteRegion());
                }
            }
        });
    }

    private void checkSnapshot(File path) throws Throwable {
        Files.unzip(path.listFiles()[0], path);
        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(new File(path, "0").getPath());
        builder.addPath(new File(path, "1").getPath());
        builder.addPath(new File(path, "2").getPath());
        builder.addPath(new File(path, "3").getPath());

        Database database = null;
        try {
            database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
            database.open();
            checkDatabase(database);
        } finally {
            IOs.close(database);
        }
    }

    private void checkDatabase(Database database) {
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                ObjectNode node1 = (ObjectNode) ((ObjectNodeObject) space.findNodeById(ids[0])).getNode();
                IFileField field1 = node1.getField(0);
                ObjectNode node2 = (ObjectNode) ((ObjectNodeObject) space.findNodeById(ids[1])).getNode();
                IFileField field2 = node2.getField(0);

                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i));

                    IRawPage page2 = field2.getPage(i);
                    checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i));
                }
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node1 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[2])).getNode();
                IFileField field1 = node1.getField(1);
                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[3])).getNode();
                IFileField field2 = node2.getField(1);

                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i));

                    IRawPage page2 = field2.getPage(i);
                    checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i));
                }
            }
        });
    }

    private void writeRegion(int base, IRawWriteRegion region) {
        for (int i = 0; i < region.getLength(); i++)
            region.writeByte(i, (byte) (base + i));
    }

    private void checkRegion(IRawReadRegion region, ByteArray array) {
        assertThat(region.readByteArray(0, array.getLength()), is(array));
    }

    private ByteArray createBuffer(int size, int base) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++)
            b[i] = (byte) (i + base);

        return new ByteArray(b);
    }
}
