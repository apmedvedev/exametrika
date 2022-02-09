/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.IPeriodOperationManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
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
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.FileArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.NullArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.OperationManager;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link OperationManagerTests} are tests for operation manager.
 *
 * @author Medvedev-A
 * @see OperationManager
 */
public class OperationManagerTests {
    final int COUNT = 10000;
    private Database database;
    private DatabaseConfiguration dbConfiguration;
    private IDatabaseFactory.Parameters parameters;
    private File testDir1;
    private File testDir2;

    @Before
    public void setUp() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        testDir1 = new File(System.getProperty("java.io.tmpdir"), "db-test1");
        Files.emptyDir(testDir1);
        testDir2 = new File(System.getProperty("java.io.tmpdir"), "db-test2");
        Files.emptyDir(testDir2);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(200000000)).toConfiguration());
        builder.addExtension(new IndexDatabaseExtensionConfiguration(100000, new FullTextIndexConfiguration(60000, 3000, 60000, 16000000)));
        //builder.setTimerPeriod(1000000);
        dbConfiguration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.initialSchemaProvider = new IInitialSchemaProvider() {
            @Override
            public ModularDatabaseSchemaConfiguration getInitialSchema() {
                return new ModularDatabaseSchemaConfiguration("test", Collections.<ModuleSchemaConfiguration>emptySet());
            }
        };
        parameters.parameters.put("disableModules", true);
        database = new DatabaseFactory().createDatabase(parameters, dbConfiguration);
        database.open();

        createDatabase();
        checkDatabase(database, false);

        builder.setTimerPeriod(1000);
        dbConfiguration = builder.toConfiguration();
        database.setConfiguration(dbConfiguration);
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testSnapshots() throws Throwable {
        database.getOperations().snapshot(testDir1.getPath(), null);
        checkSnapshot(testDir1);
    }

    @Test
    public void testBackup() throws Throwable {
        database.getOperations().backup(new FileArchiveStoreSchemaConfiguration(testDir1.getPath()), null);
        checkSnapshot(testDir1);
    }

    @Test
    public void testFullArchiving() throws IOException {
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
    public void testPartialArchiving() throws Throwable {
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(
                new NameFilter(Arrays.asList(new NameFilter("space3")),
                        Collections.<NameFilter>emptyList()), Arrays.asList("period1"), new SimpleArchivePolicySchemaConfiguration(1000),
                new NullArchiveStoreSchemaConfiguration(), null);

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
                            if (cycleSchema.getConfiguration().getName().equals("period1") &&
                                    cycleSchema.getParent().getConfiguration().getName().equals("space3")) {
                                IPeriodCycle cycle = cycleSchema.getCurrentCycle();
                                assertThat(cycle.isArchived(), is(true));
                            } else
                                assertThat(cycleSchema.getCurrentCycle(), nullValue());
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testFullTruncation() throws Throwable {
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(null, null,
                new SimpleArchivePolicySchemaConfiguration(1000), new NullArchiveStoreSchemaConfiguration(), null);
        checkDatabase(database, true);
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
    public void testPartialTruncation() throws Throwable {
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(
                new NameFilter(Arrays.asList(new NameFilter("space3")),
                        Collections.<NameFilter>emptyList()), Arrays.asList("period1"), new SimpleArchivePolicySchemaConfiguration(1000),
                new NullArchiveStoreSchemaConfiguration(), null);
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).truncateCycles(
                new NameFilter(Arrays.asList(new NameFilter("space3")),
                        Collections.<NameFilter>emptyList()), Arrays.asList("period1"), new SimpleTruncationPolicySchemaConfiguration(0, 0, 0, 0), true, null);

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
                            if (cycleSchema.getConfiguration().getName().equals("period1") &&
                                    cycleSchema.getParent().getConfiguration().getName().equals("space3")) {
                                IPeriodCycle cycle = cycleSchema.getCurrentCycle();
                                assertThat(cycle.isDeleted(), is(true));
                            } else
                                assertThat(cycleSchema.getCurrentCycle(), nullValue());
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testRestore() throws Throwable {
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).archiveCycles(null, null,
                new SimpleArchivePolicySchemaConfiguration(1000), new FileArchiveStoreSchemaConfiguration(testDir1.getPath()), null);
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).truncateCycles(null, null,
                new SimpleTruncationPolicySchemaConfiguration(0, 0, 0, 0), true, null);
        ((IPeriodOperationManager) database.findExtension(IPeriodOperationManager.NAME)).restoreCycles(
                com.exametrika.common.utils.Collections.asSet("test-space3-period1-24", "test-space3-period2-36",
                        "test-space4-period1-48", "test-space4-period2-60"
                ),
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

        checkDatabase(database, true);
    }

    private void createDatabase() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", "field1", "", DataType.INT, null, null, null, null, 0, IndexType.BTREE,
                        true, true, false, true, true, false, null, null),
                new StringFieldSchemaConfiguration("field2", 100),
                new JsonFieldSchemaConfiguration("field3"),
                new SingleReferenceFieldSchemaConfiguration("field4", null),
                new SerializableFieldSchemaConfiguration("field5"),
                new IndexedStringFieldSchemaConfiguration("field6", true, true, 0, 256, null, null, null, 0, null,
                        false, false, false, false, false, null, true, false, null, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new ObjectNodeSchemaConfiguration("node2", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodNodeSchemaConfiguration("node3",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(
                        new StringFieldSchemaConfiguration("field2", 100),
                        new JsonFieldSchemaConfiguration("field3"),
                        new SingleReferenceFieldSchemaConfiguration("field4", null),
                        new SerializableFieldSchemaConfiguration("field5"),
                        new IndexedStringFieldSchemaConfiguration("field6", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, null, true, false, null, null)), null);
        PeriodNodeSchemaConfiguration nodeConfiguration4 = new PeriodNodeSchemaConfiguration("node2",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(
                        new ReferenceFieldSchemaConfiguration("field2", null)), null);
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), "node2", 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), "node2", 0, 0);

        PeriodSpaceSchemaConfiguration space3 = new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                Arrays.asList(new PeriodSchemaConfiguration("period1", new HashSet(Arrays.asList(nodeConfiguration3, nodeConfiguration4)),
                        "node2", "node2", 1000000, 100, false, null), new PeriodSchemaConfiguration("period2", new HashSet(Arrays.asList(nodeConfiguration3, nodeConfiguration4)),
                        "node2", "node2", 100000000, 100, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space4 = new PeriodSpaceSchemaConfiguration("space4", "space4", null,
                Arrays.asList(new PeriodSchemaConfiguration("period1", new HashSet(Arrays.asList(nodeConfiguration3, nodeConfiguration4)),
                        "node2", "node2", 1000000, 100, false, null), new PeriodSchemaConfiguration("period2", new HashSet(Arrays.asList(nodeConfiguration3, nodeConfiguration4)),
                        "node2", "node2", 100000000, 100, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1,
                space2, space3, space4)));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                fillDataSpace(space);
                checkObjectSpace(space, false);

                dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                space = (ObjectSpace) dataSchema.getSpace();
                fillDataSpace(space);
                checkObjectSpace(space, false);

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space3");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();
                fillPeriod(period);
                checkPeriod(period, false);

                cycleSchema = (CycleSchema) periodSchema.getCycles().get(1);

                cycle = cycleSchema.getCurrentCycle().getSpace();
                period = cycle.getCurrentPeriod();
                fillPeriod(period);
                checkPeriod(period, false);

                periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space4");
                cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                cycle = cycleSchema.getCurrentCycle().getSpace();
                period = cycle.getCurrentPeriod();
                fillPeriod(period);
                checkPeriod(period, false);

                cycleSchema = (CycleSchema) periodSchema.getCycles().get(1);

                cycle = cycleSchema.getCurrentCycle().getSpace();
                period = cycle.getCurrentPeriod();
                fillPeriod(period);
                checkPeriod(period, false);
            }
        });
    }

    private void checkSnapshot(File path) throws Throwable {
        Files.unzip(path.listFiles()[0], path);
        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(path.getPath());
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(200000000)).toConfiguration());

        Database database = null;
        try {
            database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
            database.open();
            checkDatabase(database, false);
        } finally {
            IOs.close(database);
        }
    }

    private void checkDatabase(Database database, boolean readonly) throws Throwable {
        database.transactionSync(new Operation(readonly) {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                IObjectSpaceSchema dataSchema = schema.findDomain("test").findSpace("space1");
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                checkObjectSpace(space, true);

                dataSchema = schema.findDomain("test").findSpace("space2");
                space = (ObjectSpace) dataSchema.getSpace();
                checkObjectSpace(space, true);

                IPeriodSpaceSchema periodSchema = schema.findDomain("test").findSpace("space3");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();
                checkPeriod(period, true);

                cycleSchema = (CycleSchema) periodSchema.getCycles().get(1);

                cycle = cycleSchema.getCurrentCycle().getSpace();
                period = cycle.getCurrentPeriod();
                checkPeriod(period, true);

                periodSchema = schema.findDomain("test").findSpace("space4");
                cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                cycle = cycleSchema.getCurrentCycle().getSpace();
                period = cycle.getCurrentPeriod();
                checkPeriod(period, true);

                cycleSchema = (CycleSchema) periodSchema.getCycles().get(1);

                cycle = cycleSchema.getCurrentCycle().getSpace();
                period = cycle.getCurrentPeriod();
                checkPeriod(period, true);
            }
        });
    }

    private void fillDataSpace(ObjectSpace space) {
        INode node = space.getRootNode();
        IReferenceField<INode> field = node.getField(0);

        String s = "Helwjfirfiehfsvbabjv[dKVC]]]]]]]]]]]]]]]VMLFV]PMV]QFWB]PEQJB[QEJ";
        for (int i = 0; i < COUNT; i++) {
            INode node1 = space.addNode(i, 0);
            IStringField field2 = node1.getField(1);
            field2.set(s);
            IJsonField field3 = node1.getField(2);
            field3.set(Json.object().put("key", "value").toObject());
            ISingleReferenceField field4 = node1.getField(3);
            field4.set(node);
            ISerializableField field5 = node1.getField(4);
            field5.set(s);
            IStringField field6 = node1.getField(5);
            field6.set("Hello" + i);
            field.add(node1);
        }
    }

    private void checkObjectSpace(ObjectSpace space, boolean checkFullText) {
        IFieldSchema primaryFieldSchema = space.getSchema().findNode("node1").findField("field1");
        IDocumentSchema documentSchema = space.getSchema().findNode("node1").getFullTextSchema();
        for (@SuppressWarnings("unused") INode n : space.<INode>getNodes())
            ;
        INode node = space.getRootNode();
        IReferenceField<INode> field = node.getField(0);

        String s = "Helwjfirfiehfsvbabjv[dKVC]]]]]]]]]]]]]]]VMLFV]PMV]QFWB]PEQJB[QEJ";
        Set<INode> nodes = new HashSet<INode>();
        for (int i = 0; i < COUNT; i++) {
            INode node1 = space.<INodeIndex<Integer, INode>>getIndex(primaryFieldSchema).find(i);
            IStringField field2 = node1.getField(1);
            assertThat(field2.get(), is(s));
            IJsonField field3 = node1.getField(2);
            assertThat((JsonObject) field3.get(), is(Json.object().put("key", "value").toObject()));
            ISingleReferenceField field4 = node1.getField(3);
            assertThat((INode) field4.get(), is(node));
            ISerializableField field5 = node1.getField(4);
            assertThat((String) field5.get(), is(s));
            IStringField field6 = node1.getField(5);
            assertThat(field6.get(), is("Hello" + i));
            nodes.add(node1);
        }

        if (checkFullText) {
            INodeSearchResult result = space.getFullTextIndex().search(Queries.term("field6", "Hello5000").toQuery(documentSchema), 1);
            assertThat(result.getTotalCount(), is(1));
        }

        int i = 0;
        for (INode n : field) {
            i++;
            assertThat(nodes.contains(n), is(true));
        }

        assertThat(i, is(COUNT));
    }

    private void fillPeriod(Period period) {
        INode node = period.getRootNode();
        IReferenceField<INode> field = node.getField(1);

        String s = "Helwjfirfiehfsvbabjv[dKVC]]]]]]]]]]]]]]]VMLFV]PMV]QFWB]PEQJB[QEJ";
        for (int i = 0; i < COUNT; i++) {
            INode node1 = period.addNode(new Location(i + 1, i + 1), 1);
            IStringField field2 = node1.getField(1);
            field2.set(s);
            IJsonField field3 = node1.getField(2);
            field3.set(Json.object().put("key", "value").toObject());
            ISingleReferenceField field4 = node1.getField(3);
            field4.set(node);
            ISerializableField field5 = node1.getField(4);
            field5.set(s);
            IStringField field6 = node1.getField(5);
            field6.set("Hello" + i);
            field.add(node1);
        }
    }

    private void checkPeriod(Period period, boolean checkFullText) {
        IFieldSchema primaryFieldSchema = period.getSpace().getSchema().findNode("node3").findField("field");
        IDocumentSchema documentSchema = period.getSpace().getSchema().findNode("node3").getFullTextSchema();
        for (@SuppressWarnings("unused") INode n : period.<INode>getNodes())
            ;
        INode node = period.getRootNode();
        IReferenceField<INode> field = node.getField(1);

        String s = "Helwjfirfiehfsvbabjv[dKVC]]]]]]]]]]]]]]]VMLFV]PMV]QFWB]PEQJB[QEJ";
        Set<INode> nodes = new HashSet<INode>();
        for (int i = 0; i < COUNT; i++) {
            INode node1 = period.<INodeIndex<Location, INode>>getIndex(primaryFieldSchema).find(new Location(i + 1, i + 1));
            IStringField field2 = node1.getField(1);
            assertThat(field2.get(), is(s));
            IJsonField field3 = node1.getField(2);
            assertThat((JsonObject) field3.get(), is(Json.object().put("key", "value").toObject()));
            ISingleReferenceField field4 = node1.getField(3);
            assertThat((INode) field4.get(), is(node));
            ISerializableField field5 = node1.getField(4);
            assertThat((String) field5.get(), is(s));
            IStringField field6 = node1.getField(5);
            assertThat(field6.get(), is("Hello" + i));
            nodes.add(node1);
        }

        if (checkFullText) {
            INodeSearchResult result = period.getSpace().getFullTextIndex().search(Queries.term("field6", "Hello5000").toQuery(documentSchema), 1);
            assertThat(result.getTotalCount(), is(1));
        }

        int i = 0;
        for (INode n : field) {
            i++;
            assertThat(nodes.contains(n), is(true));
        }

        assertThat(i, is(COUNT));
    }
}
