/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.PeriodDatabaseExtensionConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodDatabaseExtension;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.cache.PeriodNodeCache;
import com.exametrika.impl.aggregator.cache.PeriodNodeManager;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.ObjectDatabaseExtension;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.cache.NodeManager;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCache;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeManager;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.PeriodTestNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNode;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNodeSchema;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * The {@link NodeCacheTests} are tests for {@link NodeManager}.
 *
 * @author Medvedev-A
 */
public class NodeCacheTests {
    private Database database;
    private DatabaseConfigurationBuilder builder;
    private DatabaseConfiguration configuration;
    private IDatabaseFactory.Parameters parameters;

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(1000000);
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
    public void testNodes() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", DataType.INT)));

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 2, false, null)), 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", new HashSet(Arrays.asList(nodeConfiguration2)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1, space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final TestPeriodNode[] nodes = new TestPeriodNode[4];
        final TestNode[] nodes2 = new TestNode[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    PeriodNodeManager nodeManager1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    PeriodNodeCache nodeCache1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                    nodes[0] = node1;
                    TestNode node2 = space.addNode(123, 0);
                    nodes2[1] = node2;
                    assertThat(node1.node.isModified(), is(true));
                    assertThat(period.addNode(new Location(1, 1), 0) == node1, is(true));
                    assertThat(node2.node.isModified(), is(true));
                    assertThat(space.addNode(123, 0) == node2, is(true));
                    assertThat(nodeCache1.findById(cycle.getFileIndex(), node1.node.getId()) == node1.node, is(true));
                    assertThat(nodeCache1.findById(cycle.getFileIndex(), node1.node.getId()) == node1.node, is(true));
                    assertThat(nodeCache2.findById(space.getFileIndex(), node2.node.getId()) == node2.node, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache1, "nodes")).find(node1.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(node2.node) != null, is(true));
                    assertThat(((Long) Tests.get(nodeCache1, "cacheSize")).intValue(), is(node1.node.getCacheSize()));
                    assertThat(((Long) Tests.get(nodeCache2, "cacheSize")).intValue(), is(node2.node.getCacheSize()));
                    assertThat(((SimpleList) Tests.get(nodeManager1, "writeNodes")).find(node1.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(node2.node) != null, is(true));

                    IPrimitiveField field1 = node1.node.getField(1);
                    field1.setInt(123);

                    IPrimitiveField field2 = node2.node.getField(1);
                    field2.setInt(123);
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.close();

        assertThat(nodes[0].flushed, is(true));
        assertThat(nodes2[1].flushed, is(true));

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        try {
                            PeriodNodeManager nodeManager1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                                    PeriodDatabaseExtensionConfiguration.NAME)).getNodeManager();
                            ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                            PeriodNodeCache nodeCache1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                                    PeriodDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
                            ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                            IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                            IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                            CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                            PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                            Period period = cycle.getCurrentPeriod();
                            ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                            TestPeriodNode node1 = period.findNodeById(nodes[0].node.getId());
                            nodes[0] = node1;
                            TestNode node2 = space.findNodeById(nodes2[1].node.getId());
                            nodes2[1] = node2;
                            assertThat(node1.node.isModified(), is(false));
                            assertThat(node2.node.isModified(), is(false));
                            assertThat(nodeCache1.findById(cycle.getFileIndex(), node1.node.getId()) == node1.node, is(true));
                            assertThat(nodeCache1.findById(cycle.getFileIndex(), node1.node.getId()) == node1.node, is(true));
                            assertThat(nodeCache2.findById(space.getFileIndex(), node2.node.getId()) == node2.node, is(true));
                            assertThat(((Long) Tests.get(nodeCache1, "cacheSize")).intValue(), is(node1.node.getCacheSize()));
                            assertThat(((Long) Tests.get(nodeCache2, "cacheSize")).intValue(), is(node2.node.getCacheSize()));
                            assertThat(((SimpleList) Tests.get(nodeCache1, "nodes")).find(node1.node) != null, is(true));
                            assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(node2.node) != null, is(true));

                            IPrimitiveField field1 = node1.node.getField(1);
                            field1.setInt(456);
                            IPrimitiveField field2 = node2.node.getField(1);
                            field2.setInt(456);
                            assertThat(((SimpleList) Tests.get(nodeManager1, "writeNodes")).find(node1.node) != null, is(true));
                            assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(node2.node) != null, is(true));
                            assertThat(node1.node.isModified(), is(true));
                            assertThat(node2.node.isModified(), is(true));

                            ((TestPeriodNodeSchema) node1.node.getSchema()).invalid = true;
                        } catch (Throwable e) {
                            Exceptions.wrapAndThrow(e);
                        }
                    }
                });
            }
        });

        PeriodNodeCache nodeCache1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                PeriodDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
        ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
        assertThat(((Long) Tests.get(nodeCache1, "cacheSize")).intValue(), is(0));
        assertThat(((Long) Tests.get(nodeCache2, "cacheSize")).intValue(), is(0));
        assertThat(((Map) Tests.get(nodeCache1, "nodeByIdMap")).isEmpty(), is(true));
        assertThat(nodes[0].node.isStale(), is(true));
        assertThat(nodes2[1].node.isStale(), is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    PeriodNodeManager nodeManager1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    PeriodNodeCache nodeCache1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestPeriodNode node3 = period.addNode(new Location(2, 2), 0);
                    nodes[2] = node3;
                    TestNode node4 = space.addNode(777, 0);
                    nodes2[3] = node4;

                    nodes[0] = period.findNodeById(nodes[0].node.getId());
                    ((TestPeriodNodeSchema) nodes[0].node.getSchema()).invalid = false;
                    nodes2[1] = space.findNodeById(nodes2[1].node.getId());
                    assertThat(nodeCache1.findById(cycle.getFileIndex(), nodes[0].node.getId()) == nodes[0].node, is(true));
                    assertThat(nodeCache2.findById(space.getFileIndex(), nodes2[1].node.getId()) == nodes2[1].node, is(true));
                    assertThat(nodes[0].node.isModified(), is(false));
                    assertThat(nodes2[1].node.isModified(), is(false));

                    assertThat(period.findNodeById(nodes[0].node.getId()) == nodes[0], is(true));

                    assertThat((Long) Tests.get(nodeCache1, "cacheSize"), is(((long) nodes[0].node.getCacheSize() + node3.node.getCacheSize())));
                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(((long) nodes2[1].node.getCacheSize() + node4.node.getCacheSize())));
                    assertThat(((SimpleList) Tests.get(nodeCache1, "nodes")).find(nodes[0].node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(nodes2[1].node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache1, "nodes")).find(node3.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(node4.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager1, "writeNodes")).find(nodes[0].node) == null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager1, "writeNodes")).find(node3.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(nodes2[1].node) == null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(node4.node) != null, is(true));
                    IPrimitiveField field1 = nodes[0].node.getField(1);
                    assertThat(field1.getInt(), is(123));
                    field1.setInt(456);

                    IPrimitiveField field2 = nodes2[1].node.getField(1);
                    assertThat(field2.getInt(), is(123));
                    field2.setInt(456);

                    //Tests.set(db, "currentTime", Times.getCurrentTime());
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                nodes[0] = period.findNodeById(nodes[0].node.getId());
                IPrimitiveField field1 = nodes[0].node.getField(1);
                field1.setInt(456);

                nodes2[1] = space.findNodeById(nodes2[1].node.getId());
                IPrimitiveField field2 = nodes2[1].node.getField(1);
                field2.setInt(456);
            }
        });

        assertThat(nodes[0].flushed, is(true));
        assertThat(nodes2[1].flushed, is(true));

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    PeriodNodeManager nodeManager1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    PeriodNodeCache nodeCache1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);


                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestPeriodNode node3 = period.findNodeById(nodes[2].node.getId());
                    TestPeriodNode node1 = period.findNodeById(nodes[0].node.getId());
                    TestNode node4 = space.findNodeById(nodes2[3].node.getId());
                    TestNode node2 = space.findNodeById(nodes2[1].node.getId());
                    assertThat((Long) Tests.get(nodeCache1, "cacheSize"), is(((long) node1.node.getCacheSize() + node3.node.getCacheSize())));
                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(((long) node2.node.getCacheSize() + node4.node.getCacheSize())));
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache1, "nodes")).toList(), is(Arrays.asList(node3.node, node1.node)));
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<Node>asList(node4.node, node2.node)));
                    node3 = period.findNodeById(node3.node.getId());
                    node4 = space.findNodeById(node4.node.getId());
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache1, "nodes")).toList(), is(Arrays.asList(node3.node, node1.node)));
                    assertThat(((SimpleList<INode>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<INode>asList(node4.node, node2.node)));

                    IPrimitiveField field1 = node1.node.getField(1);
                    assertThat(field1.getInt(), is(456));
                    field1.setInt(789);

                    IPrimitiveField field2 = node2.node.getField(1);
                    assertThat(field2.getInt(), is(456));
                    field2.setInt(789);

                    assertThat(period.getNodes().iterator().next() == node1, is(true));
                    assertThat(period.findNode(node1) == node1, is(true));

                    assertThat(space.getNodes().iterator().next() == node4, is(true));

                    cycle.addPeriod();

                    nodeManager1.flush(true);
                    nodeManager2.flush(true);
                    nodeCache1.unloadNodes(true);
                    nodeCache2.unloadNodes(true);
                    assertThat(node1.flushed, is(true));
                    assertThat(node2.flushed, is(true));

                    assertThat((Long) Tests.get(nodeCache1, "cacheSize"), is(0l));
                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(0l));
                    assertThat(((Map) Tests.get(nodeCache1, "nodeByIdMap")).isEmpty(), is(true));
                    assertThat(((Map) Tests.get(nodeCache2, "nodeByIdMap")).isEmpty(), is(true));
                    assertThat(node3.node.isStale(), is(true));
                    assertThat(node1.node.isStale(), is(true));
                    assertThat(node4.node.isStale(), is(true));
                    assertThat(node2.node.isStale(), is(true));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    PeriodNodeCache nodeCache1 = ((PeriodDatabaseExtension) database.getContext().findExtension(
                            PeriodDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestPeriodNode node3 = period.findNodeById(nodes[2].node.getId());
                    TestPeriodNode node1 = period.findNodeById(nodes[0].node.getId());
                    TestNode node4 = space.findNodeById(nodes2[3].node.getId());
                    TestNode node2 = space.findNodeById(nodes2[1].node.getId());
                    assertThat((Long) Tests.get(nodeCache1, "cacheSize"), is(((long) node1.node.getCacheSize() + node3.node.getCacheSize())));
                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(((long) node2.node.getCacheSize() + node4.node.getCacheSize())));
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache1, "nodes")).toList(), is(Arrays.asList(node3.node, node1.node)));
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<Node>asList(node4.node, node2.node)));
                    node3 = period.findNodeById(node3.node.getId());
                    node4 = space.findNodeById(node4.node.getId());
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache1, "nodes")).toList(), is(Arrays.asList(node3.node, node1.node)));
                    assertThat(((SimpleList<INode>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<INode>asList(node4.node, node2.node)));

                    IPrimitiveField field1 = node1.node.getField(1);
                    assertThat(field1.getInt(), is(789));

                    IPrimitiveField field2 = node2.node.getField(1);
                    assertThat(field2.getInt(), is(789));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    public static class TestOperation extends Operation {
        RuntimeException exception;

        public TestOperation() {
        }

        public TestOperation(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public void run(ITransaction transaction) {
            if (exception != null)
                throw exception;
        }

        @Override
        public void onCommitted() {
        }

        @Override
        public void onRolledBack() {
        }
    }
}
