/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.IObjectNode;
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
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.ObjectDatabaseExtension;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.cache.NodeManager;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCache;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeManager;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchema;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


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
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", DataType.INT)));

        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", new HashSet(Arrays.asList(nodeConfiguration2)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final TestNode[] nodes2 = new TestNode[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node2 = space.addNode(123, 0);
                    nodes2[1] = node2;
                    assertThat(node2.node.isModified(), is(true));
                    assertThat(space.addNode(123, 0) == node2, is(true));
                    assertThat(nodeCache2.findById(space.getFileIndex(), node2.node.getId()) == node2.node, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(node2.node) != null, is(true));
                    assertThat(((Long) Tests.get(nodeCache2, "cacheSize")).intValue(), is(node2.node.getCacheSize()));
                    assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(node2.node) != null, is(true));

                    IPrimitiveField field2 = node2.node.getField(1);
                    field2.setInt(123);
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.close();

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
                            ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                            ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                            IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                            ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                            TestNode node2 = space.findNodeById(nodes2[1].node.getId());
                            nodes2[1] = node2;
                            assertThat(node2.node.isModified(), is(false));
                            assertThat(nodeCache2.findById(space.getFileIndex(), node2.node.getId()) == node2.node, is(true));
                            assertThat(((Long) Tests.get(nodeCache2, "cacheSize")).intValue(), is(node2.node.getCacheSize()));
                            assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(node2.node) != null, is(true));

                            IPrimitiveField field2 = node2.node.getField(1);
                            field2.setInt(456);
                            assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(node2.node) != null, is(true));
                            assertThat(node2.node.isModified(), is(true));

                            throw new RawDatabaseException();
                        } catch (Throwable e) {
                            Exceptions.wrapAndThrow(e);
                        }
                    }
                });
            }
        });

        ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
        assertThat(((Long) Tests.get(nodeCache2, "cacheSize")).intValue(), is(0));
        assertThat(nodes2[1].node.isStale(), is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node4 = space.addNode(777, 0);
                    nodes2[3] = node4;

                    nodes2[1] = space.findNodeById(nodes2[1].node.getId());
                    assertThat(nodeCache2.findById(space.getFileIndex(), nodes2[1].node.getId()) == nodes2[1].node, is(true));
                    assertThat(nodes2[1].node.isModified(), is(false));

                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(((long) nodes2[1].node.getCacheSize() + node4.node.getCacheSize())));
                    assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(nodes2[1].node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeCache2, "nodes")).find(node4.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(nodes2[1].node) == null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager2, "writeNodes")).find(node4.node) != null, is(true));

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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                nodes2[1] = space.findNodeById(nodes2[1].node.getId());
                IPrimitiveField field2 = nodes2[1].node.getField(1);
                field2.setInt(456);
            }
        });

        assertThat(nodes2[1].flushed, is(true));

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    ObjectNodeManager nodeManager2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);


                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node4 = space.findNodeById(nodes2[3].node.getId());
                    TestNode node2 = space.findNodeById(nodes2[1].node.getId());
                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(((long) node2.node.getCacheSize() + node4.node.getCacheSize())));
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<Node>asList(node4.node, node2.node)));
                    node4 = space.findNodeById(node4.node.getId());
                    assertThat(((SimpleList<INode>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<INode>asList(node4.node, node2.node)));

                    IPrimitiveField field2 = node2.node.getField(1);
                    assertThat(field2.getInt(), is(456));
                    field2.setInt(789);

                    assertThat(space.getNodes().iterator().next() == node4, is(true));

                    nodeManager2.flush(true);
                    nodeCache2.unloadNodes(true);
                    assertThat(node2.flushed, is(true));

                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(0l));
                    assertThat(((Map) Tests.get(nodeCache2, "nodeByIdMap")).isEmpty(), is(true));
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
                    ObjectNodeCache nodeCache2 = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node4 = space.findNodeById(nodes2[3].node.getId());
                    TestNode node2 = space.findNodeById(nodes2[1].node.getId());
                    assertThat((Long) Tests.get(nodeCache2, "cacheSize"), is(((long) node2.node.getCacheSize() + node4.node.getCacheSize())));
                    assertThat(((SimpleList<Node>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<Node>asList(node4.node, node2.node)));
                    node4 = space.findNodeById(node4.node.getId());
                    assertThat(((SimpleList<INode>) Tests.get(nodeCache2, "nodes")).toList(), is(Arrays.<INode>asList(node4.node, node2.node)));

                    IPrimitiveField field2 = node2.node.getField(1);
                    assertThat(field2.getInt(), is(789));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testDelayedFlush() throws Throwable {
        database.close();
        builder.setTimerPeriod(1000);
        configuration = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        NodeSchemaConfiguration nodeConfiguration = new TestNodeSchemaConfiguration("node", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", DataType.INT)));

        ObjectSpaceSchemaConfiguration space = new ObjectSpaceSchemaConfiguration("space", new HashSet(Arrays.asList(nodeConfiguration)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] id = new long[1];
        database.transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node2 = space.addNode(123, 0);
                    id[0] = node2.node.getId();
                    IPrimitiveField field2 = node2.node.getField(1);
                    field2.setInt(1000);
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        for (int i = 0; i < 1000; i++) {
            final int n = i;
            database.transaction(new Operation(IOperation.DELAYED_FLUSH) {
                @Override
                public void run(ITransaction transaction) {
                    try {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space");
                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node2 = space.findNodeById(id[0]);
                        IPrimitiveField field2 = node2.node.getField(1);

                        assertThat(field2.getInt(), is(n + 1000));
                        field2.setInt(field2.getInt() + 1);
                    } catch (Throwable e) {
                        Exceptions.wrapAndThrow(e);
                    }
                }
            });
        }

        database.transactionSync(new Operation(IOperation.FLUSH) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node2 = space.findNodeById(id[0]);
                    IPrimitiveField field2 = node2.node.getField(1);
                    assertThat(field2.getInt(), is(2000));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        Threads.sleep(1000);

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node2 = space.findNodeById(id[0]);
                    IPrimitiveField field2 = node2.node.getField(1);
                    assertThat(field2.getInt(), is(2000));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", new HashSet(Arrays.asList(nodeConfiguration2)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);

            }
        });

        final TestNode[] nodes = new TestNode[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                nodes[0] = space.addNode(111, 0);
                nodes[1] = space.addNode(123, 0);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        try {
                            ObjectNodeManager nodeManager = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                            ObjectNodeCache nodeCache = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                            IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                            ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                            TestNode node3 = space.addNode(222, 0);
                            nodes[2] = node3;
                            nodes[0] = space.findNodeById(nodes[0].node.getId());
                            nodes[1] = space.findNodeById(nodes[1].node.getId());
                            assertThat(((IObjectNode) nodes[0].node).getKey(), is((Object) 111));
                            assertThat(((IObjectNode) nodes[1].node).getKey(), is((Object) 123));

                            IPrimitiveField field1 = nodes[0].node.getField(1);
                            field1.setInt(456);

                            ((IObjectNode) nodes[0].node).delete();
                            ((IObjectNode) nodes[1].node).delete();

                            assertThat(((Long) Tests.get(nodeCache, "cacheSize")).intValue(), is(node3.node.getCacheSize()));
                            assertThat(((SimpleList) Tests.get(nodeCache, "nodes")).find(node3.node) != null, is(true));
                            assertThat(((SimpleList) Tests.get(nodeManager, "writeNodes")).find(node3.node) != null, is(true));
                            assertThat((Integer) Tests.get(nodeManager, "freeNodeCount"), is(2));
                            assertThat(((SimpleList) Tests.get(nodeManager, "freeNodes")).find(nodes[0].node) != null, is(true));
                            assertThat(((SimpleList) Tests.get(nodeManager, "freeNodes")).find(nodes[1].node) != null, is(true));
                            assertThat(((Map) Tests.get(nodeManager, "freeNodeMap")).size(), is(1));
                            assertThat(nodes[0].node.isModified(), is(false));
                            assertThat(nodes[1].node.isModified(), is(false));
                            assertThat(nodes[0].node.isDeleted(), is(true));
                            assertThat(nodes[1].node.isDeleted(), is(true));

                            ((TestNodeSchema) node3.node.getSchema()).invalid = true;
                        } catch (Throwable e) {
                            Exceptions.wrapAndThrow(e);
                        }
                    }
                });
            }
        });

        ObjectNodeManager nodeManager = ((ObjectDatabaseExtension) database.getContext().findExtension(
                ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
        ObjectNodeCache nodeCache = ((ObjectDatabaseExtension) database.getContext().findExtension(
                ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
        assertThat(((Long) Tests.get(nodeCache, "cacheSize")).intValue(), is(0));
        assertThat((Integer) Tests.get(nodeManager, "freeNodeCount"), is(0));
        assertThat(((Map) Tests.get(nodeCache, "nodeByIdMap")).isEmpty(), is(true));
        assertThat(((Map) Tests.get(nodeManager, "freeNodeMap")).isEmpty(), is(true));
        assertThat(nodes[0].node.isStale(), is(true));
        assertThat(nodes[1].node.isStale(), is(true));
        assertThat(nodes[2].node.isStale(), is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    ObjectNodeManager nodeManager = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                    ObjectNodeCache nodeCache = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.findNodeById(nodes[0].node.getId());
                    ((TestNodeSchema) node1.node.getSchema()).invalid = false;

                    ((IObjectNode) node1.node).delete();

                    assertThat(space.addNode(456, 0) == node1, is(true));
                    assertThat(node1.node.isModified(), is(true));
                    assertThat(node1.node.isDeleted(), is(false));
                    assertThat(((IObjectNode) node1.node).getKey(), is((Object) 456));

                    assertThat((Integer) Tests.get(nodeManager, "freeNodeCount"), is(0));
                    assertThat(((SimpleList) Tests.get(nodeManager, "freeNodes")).isEmpty(), is(true));
                    assertThat(((SimpleList) ((Map) Tests.get(nodeManager, "freeNodeMap")).values().iterator().next()).isEmpty(), is(true));
                    assertThat(((Long) Tests.get(nodeCache, "cacheSize")).intValue(), is(node1.node.getCacheSize()));
                    assertThat(((SimpleList) Tests.get(nodeCache, "nodes")).find(node1.node) != null, is(true));
                    assertThat(((SimpleList) Tests.get(nodeManager, "writeNodes")).find(node1.node) != null, is(true));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testBigTransaction() throws Throwable {
        IOs.close(database);
        configuration = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", new HashSet(Arrays.asList(nodeConfiguration2)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final TestNode[] nodes = new TestNode[6];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    ObjectNodeCache nodeCache = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                    boolean is64Bit = System.getProperty("os.arch").contains("64");
                    nodeCache.setBatchMaxCacheSize(is64Bit ? 2000 : 1400);

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    nodes[0] = space.addNode(111, 0);
                    nodes[1] = space.addNode(123, 0);
                    nodes[2] = space.addNode(124, 0);
                    nodes[3] = space.addNode(125, 0);
                    assertThat(nodes[0].node.isStale(), is(false));
                    assertThat(((Long) Tests.get(nodeCache, "cacheSize")).intValue(), is(nodes[0].node.getCacheSize() +
                            nodes[1].node.getCacheSize() + nodes[2].node.getCacheSize() + nodes[3].node.getCacheSize()));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        try {
                            ObjectNodeManager nodeManager = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
                            ObjectNodeCache nodeCache = ((ObjectDatabaseExtension) database.getContext().findExtension(
                                    ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);

                            IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                            ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                            nodes[1] = space.findNodeById(nodes[1].node.getId());
                            ((IPrimitiveField) nodes[1].node.getField(1)).setInt(987);
                            ((IPrimitiveField) nodes[2].node.getField(1)).setInt(987);
                            ((IPrimitiveField) nodes[3].node.getField(1)).setInt(987);
                            nodes[3].node.refresh();
                            nodes[4] = space.addNode(126, 0);
                            nodes[5] = space.addNode(127, 0);

                            assertThat(((SimpleList) Tests.get(nodeCache, "nodes")).getLast() == ((Node) nodes[5].node).getElement(), is(true));
                            assertThat(((Long) Tests.get(nodeCache, "cacheSize")).intValue(), is(nodes[1].node.getCacheSize() +
                                    nodes[4].node.getCacheSize() + nodes[5].node.getCacheSize() + nodes[3].node.getCacheSize()));
                            assertThat((Boolean) Tests.get(nodeManager, "bigTransaction"), is(true));

                            assertThat(nodes[0].node.isStale(), is(true));
                            assertThat(nodes[1].node.isStale(), is(false));
                            assertThat(nodes[1].node.isModified(), is(true));
                            assertThat(nodes[2].node.isStale(), is(true));
                            assertThat(nodes[2].node.isModified(), is(false));
                            assertThat(nodes[3].node.isStale(), is(false));
                            assertThat(nodes[3].node.isModified(), is(true));

                            nodes[1] = space.findNodeById(nodes[1].node.getId());
                            assertThat(nodes[1].node.isStale(), is(false));
                            assertThat(((SimpleList) Tests.get(nodeCache, "nodes")).getLast() == ((Node) nodes[1].node).getElement(), is(true));
                            assertThat(((IPrimitiveField) nodes[1].node.getField(1)).getInt(), is(987));

                            nodes[5].node.refresh();
                            assertThat(((SimpleList) Tests.get(nodeCache, "nodes")).getLast() == ((Node) nodes[5].node).getElement(), is(true));

                            ((TestNodeSchema) nodes[1].node.getSchema()).invalid = true;
                        } catch (Throwable e) {
                            Exceptions.wrapAndThrow(e);
                        }
                    }
                });
            }
        });

        assertThat(nodes[1].node.isStale(), is(true));
        assertThat(nodes[4].node.isStale(), is(true));
        assertThat(nodes[5].node.isStale(), is(true));

        ObjectNodeManager nodeManager = ((ObjectDatabaseExtension) database.getContext().findExtension(
                ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();
        ObjectNodeCache nodeCache = ((ObjectDatabaseExtension) database.getContext().findExtension(
                ObjectDatabaseExtensionConfiguration.NAME)).getNodeCacheManager().getNodeCache(null, null);
        assertThat(((Long) Tests.get(nodeCache, "cacheSize")).intValue(), is(0));
        assertThat(((SimpleList) Tests.get(nodeCache, "nodes")).isEmpty(), is(true));
        assertThat((Boolean) Tests.get(nodeManager, "bigTransaction"), is(false));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    ObjectNodeManager nodeManager = ((ObjectDatabaseExtension) database.getContext().findExtension(
                            ObjectDatabaseExtensionConfiguration.NAME)).getNodeManager();

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    nodes[0] = space.findNodeById(nodes[0].node.getId());
                    ((TestNodeSchema) nodes[0].node.getSchema()).invalid = false;

                    nodes[1] = space.findNodeById(nodes[1].node.getId());
                    ((IPrimitiveField) nodes[1].node.getField(1)).setInt(987);
                    nodes[2] = space.findNodeById(nodes[2].node.getId());
                    nodes[3] = space.findNodeById(nodes[3].node.getId());
                    nodes[4] = space.addNode(126, 0);
                    nodes[5] = space.addNode(127, 0);
                    assertThat((Boolean) Tests.get(nodeManager, "bigTransaction"), is(true));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        assertThat((Boolean) Tests.get(nodeManager, "bigTransaction"), is(false));

        IOs.close(database);
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                assertThat(space.findNodeById(nodes[0].node.getId()) != null, is(true));
                assertThat(space.findNodeById(nodes[2].node.getId()) != null, is(true));
                assertThat(space.findNodeById(nodes[3].node.getId()) != null, is(true));
                assertThat(space.findNodeById(nodes[4].node.getId()) != null, is(true));
                assertThat(space.findNodeById(nodes[5].node.getId()) != null, is(true));

                nodes[1] = space.findNodeById(nodes[1].node.getId());
                assertThat(((IPrimitiveField) nodes[1].node.getField(1)).getInt(), is(987));
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
