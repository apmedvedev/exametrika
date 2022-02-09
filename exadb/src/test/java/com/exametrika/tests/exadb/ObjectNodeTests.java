/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleDependencySchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.config.schema.Documents;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.IObjectMigrator;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectOperationManager;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IBlobFieldInitializer;
import com.exametrika.api.exadb.objectdb.fields.IFileField;
import com.exametrika.api.exadb.objectdb.fields.IFileFieldInitializer;
import com.exametrika.api.exadb.objectdb.fields.IIndexField;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.schema.IJsonDiagnostics;
import com.exametrika.common.json.schema.IJsonValidationContext;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonType;
import com.exametrika.common.json.schema.JsonValidationException;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.fields.JsonField;
import com.exametrika.impl.exadb.objectdb.fields.PrimitiveField;
import com.exametrika.impl.exadb.objectdb.fields.SerializableField;
import com.exametrika.impl.exadb.objectdb.fields.StringField;
import com.exametrika.impl.exadb.objectdb.schema.NodeSchema;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.impl.exadb.objectdb.schema.StringFieldSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonConverterSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonValidatorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link ObjectNodeTests} are tests for data nodes.
 *
 * @author Medvedev-A
 */
public class ObjectNodeTests {
    private Database database;
    private DatabaseConfiguration configuration;
    private IDatabaseFactory.Parameters parameters;
    private DatabaseConfigurationBuilder builder;

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        builder = new DatabaseConfigurationBuilder();
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
    public void testNodes() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new TestFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3"), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }

            @Override
            public int getSize() {
                return 1;
            }
        });

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                ObjectNode dataNode1 = (ObjectNode) node1.getNode();
                assertThat(((TestNode) space.findNodeById(dataNode1.getId())).getNode(), is((INode) dataNode1));
                assertThat(dataNode1.getSpace() == space, is(true));
                assertThat(dataNode1.getKey() == Integer.valueOf(123), is(true));
                assertThat(dataNode1.getFieldCount(), is(2));
                TestField field1 = dataNode1.getField(0);
                assertThat(field1.field.getNode() == dataNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                TestNode node2 = space.addNode(234, 1);
                ids[1] = node2.node.getId();
                ObjectNode dataNode2 = (ObjectNode) node2.getNode();
                assertThat(dataNode2.getSpace() == space, is(true));
                assertThat(dataNode2.getKey().equals(Integer.valueOf(234)), is(true));
                assertThat(dataNode2.getFieldCount(), is(3));
                field1 = dataNode2.getField(0);
                TestField field2 = dataNode2.getField(1);
                assertThat(field1.field.getNode() == dataNode2, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == dataNode2, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));

                TestNode node3 = space.addNode(345, 2);
                ids[2] = node3.node.getId();
                ObjectNode dataNode3 = (ObjectNode) node3.getNode();
                assertThat(dataNode3.getSpace() == space, is(true));
                assertThat(dataNode3.getKey().equals(Integer.valueOf(345)), is(true));
                assertThat(dataNode3.getFieldCount(), is(4));
                field1 = dataNode3.getField(0);
                field2 = dataNode3.getField(1);
                TestField field3 = dataNode3.getField(2);
                assertThat(field1.field.getNode() == dataNode3, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == dataNode3, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));
                assertThat(field3.field.getNode() == dataNode3, is(true));
                assertThat(field3.field.getSchema().getConfiguration().getName(), is("field3"));

                List<TestNode> nodes = new ArrayList<ObjectNodeTests.TestNode>();
                Iterable<TestNode> n = space.getNodes();
                for (Iterator<TestNode> it1 = n.iterator(); it1.hasNext(); )
                    nodes.add(it1.next());
                assertThat(nodes, is(Arrays.asList(node3, node2, node1)));

                assertThat((TestNode) space.findNodeById(node1.node.getId()), is(node1));
                assertThat((TestNode) space.findNodeById(node2.node.getId()), is(node2));
                assertThat((TestNode) space.findNodeById(node3.node.getId()), is(node3));

                assertThat((TestNode) space.findNodeById(node1.node.getId()), is(node1));
                assertThat((TestNode) space.findNodeById(node2.node.getId()), is(node2));
                assertThat((TestNode) space.findNodeById(node3.node.getId()), is(node3));

                assertThat((TestNode) space.addNode(123, 0), is(node1));
                assertThat((TestNode) space.addNode(234, 1), is(node2));
                assertThat((TestNode) space.addNode(345, 2), is(node3));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                ObjectNode dataNode1 = (ObjectNode) node1.getNode();
                assertThat(dataNode1.getSpace() == space, is(true));
                assertThat(dataNode1.getKey() == Integer.valueOf(123), is(true));
                assertThat(dataNode1.getFieldCount(), is(2));
                TestField field1 = dataNode1.getField(0);
                assertThat(field1.field.getNode() == dataNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                TestNode node2 = space.findNodeById(ids[1]);
                ObjectNode dataNode2 = (ObjectNode) node2.getNode();
                assertThat(dataNode2.getSpace() == space, is(true));
                assertThat(dataNode2.getKey().equals(Integer.valueOf(234)), is(true));
                assertThat(dataNode2.getFieldCount(), is(3));
                field1 = dataNode2.getField(0);
                TestField field2 = dataNode2.getField(1);
                assertThat(field1.field.getNode() == dataNode2, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == dataNode2, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));

                TestNode node3 = space.findNodeById(ids[2]);
                ObjectNode dataNode3 = (ObjectNode) node3.getNode();
                assertThat(dataNode3.getSpace() == space, is(true));
                assertThat(dataNode3.getKey().equals(Integer.valueOf(345)), is(true));
                assertThat(dataNode3.getFieldCount(), is(4));
                field1 = dataNode3.getField(0);
                assertThat(dataNode3.getField(0) == field1, is(true));
                field2 = dataNode3.getField(1);
                TestField field3 = dataNode3.getField(2);
                assertThat(field1.field.getNode() == dataNode3, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == dataNode3, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));
                assertThat(field3.field.getNode() == dataNode3, is(true));
                assertThat(field3.field.getSchema().getConfiguration().getName(), is("field3"));

                List<ObjectNodeTests.TestNode> nodes = new ArrayList<ObjectNodeTests.TestNode>();
                Iterable<TestNode> n = space.getNodes();
                for (Iterator<TestNode> it2 = n.iterator(); it2.hasNext(); )
                    nodes.add(it2.next());
                assertThat(nodes, is(Arrays.asList(node3, node2, node1)));

                assertThat((TestNode) space.findNodeById(node1.node.getId()), is(node1));
                assertThat((TestNode) space.findNodeById(node2.node.getId()), is(node2));
                assertThat((TestNode) space.findNodeById(node3.node.getId()), is(node3));

                assertThat((TestNode) space.findNodeById(node1.node.getId()), is(node1));
                assertThat((TestNode) space.findNodeById(node2.node.getId()), is(node2));
                assertThat((TestNode) space.findNodeById(node3.node.getId()), is(node3));
            }
        });
    }

    @Test
    public void testPrimaryFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", DataType.INT),
                new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", DataType.INT),
                new IndexedStringFieldSchemaConfiguration("field2", true, 256)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        new Expected(InvalidArgumentException.class, new Runnable() {
            @Override
            public void run() {
                new TestNodeSchemaConfiguration("node2", Arrays.asList(
                        new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
            }
        });

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.addNode(123, 0);
                    ids[0] = node1.node.getId();
                    final PrimitiveField field1 = node1.node.getField(1);
                    assertThat(field1.isReadOnly(), is(true));
                    assertThat(field1.getInt(), is(123));
                    assertThat(((Integer) (node1.node).getKey()), is(123));
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            field1.setInt(555);
                        }
                    });

                    TestNode node2 = space.addNode("123", 1);
                    ids[1] = node2.node.getId();
                    final StringField field2 = node2.node.getField(1);
                    assertThat(field2.isReadOnly(), is(true));
                    assertThat(field2.get(), is("123"));
                    assertThat(((String) (node2.node).getKey()), is("123"));
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            field2.set("555");
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.findNodeById(ids[0]);
                    final PrimitiveField field11 = node1.node.getField(1);
                    assertThat(field11.isReadOnly(), is(true));
                    assertThat(field11.getInt(), is(123));
                    assertThat(((Integer) (node1.node).getKey()), is(123));
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            field11.setInt(555);
                        }
                    });

                    TestNode node2 = space.findNodeById(ids[1]);
                    final StringField field22 = node2.node.getField(1);
                    assertThat(field22.isReadOnly(), is(true));
                    assertThat(field22.get(), is("123"));
                    assertThat(((String) (node2.node).getKey()), is("123"));
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            field22.set("555");
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testNodeCreationDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", DataType.INT),
                new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", DataType.INT),
                new IndexedTestFieldSchemaConfiguration("field2", 256)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        new Expected(InvalidArgumentException.class, new Runnable() {
            @Override
            public void run() {
                new TestNodeSchemaConfiguration("node2", Arrays.asList(
                        new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
            }
        });

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final TestNode[] nodes = new TestNode[COUNT];
        final Set<TestNode> deletedNodes = new HashSet<TestNode>();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    for (int i = 0; i < COUNT / 2; i++) {
                        Integer key1 = Integer.valueOf(2 * i);
                        TestNode node1 = space.addNode(key1, 0);
                        nodes[2 * i] = node1;
                        assertThat(node1.node.isModified(), is(true));
                        assertThat(node1.node.isDeleted(), is(false));
                        assertThat(node1.node.getDeletionCount(), is(0));
                        assertThat(node1.primaryKey, is((Object) key1));

                        String key2 = String.valueOf(2 * i + 1);
                        TestNode node2 = space.addNode(key2, 1);
                        nodes[2 * i + 1] = node2;
                        TestField field2 = node2.node.getField(1);
                        assertThat(field2.created, is(true));
                        assertThat(field2.deleted, is(false));
                        assertThat(field2.opened, is(false));
                        assertThat(field2.primaryKey, is(key2));
                        assertThat(node2.created, is(true));
                        assertThat(node2.deleted, is(false));
                        assertThat(node2.opened, is(false));
                        assertThat(node2.primaryKey, is((Object) key2));
                    }

                    for (int i = 0; i < COUNT / 4; i++) {
                        final TestNode node1 = (nodes[4 * i]);
                        deletedNodes.add(node1);
                        node1.node.delete();
                        assertThat(node1.node.isDeleted(), is(true));
                        assertThat(node1.node.isModified(), is(false));
                        assertThat(node1.node.isReadOnly(), is(true));
                        assertThat(node1.node.getDeletionCount(), is(1));
                        node1.reset();
                        new Expected(IllegalStateException.class, new Runnable() {
                            @Override
                            public void run() {
                                ((PrimitiveField) node1.node.getField(0)).setInt(123);

                            }
                        });
                        TestNode node2 = (nodes[4 * i + 1]);
                        deletedNodes.add(node2);
                        TestField field2 = node2.node.getField(1);
                        node2.node.delete();
                        assertThat(node2.node.isDeleted(), is(true));
                        assertThat(field2.deleted, is(true));
                        assertThat(field2.opened, is(false));
                        assertThat(node2.deleted, is(true));
                        assertThat(node2.unloaded, is(true));
                        assertThat(node2.opened, is(false));
                        node2.reset();
                        field2.reset();
                    }

                    int count = 0;
                    for (TestNode node : space.<TestNode>getNodes()) {
                        assertThat(!deletedNodes.contains(node), is(true));
                        count++;
                    }
                    assertThat(count, is(COUNT - COUNT / 2));

                    for (int i = 0; i < COUNT; i++) {
                        if (deletedNodes.contains(nodes[i]))
                            assertThat(space.findNodeById(nodes[i].node.getId()), nullValue());
                        else
                            assertThat(space.findNodeById(nodes[i].node.getId()) == nodes[i], is(true));
                    }
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
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
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        int count = 0;
                        for (TestNode node : space.<TestNode>getNodes()) {
                            assertThat(!deletedNodes.contains(node), is(true));
                            count++;
                        }
                        assertThat(count, is(COUNT - COUNT / 2));

                        for (int i = 0; i < COUNT; i++) {
                            Object key;
                            if ((i % 2) == 0)
                                key = Integer.valueOf(i);
                            else
                                key = String.valueOf(i);

                            if (deletedNodes.contains(nodes[i])) {
                                assertThat((i % 4) == 0 || (i % 4) == 1, is(true));
                                assertThat(space.findNodeById(nodes[i].node.getId()), nullValue());
                            } else {
                                assertThat((i % 4) == 2 || (i % 4) == 3, is(true));
                                TestNode node = space.findNodeById(nodes[i].node.getId());
                                assertThat(node.opened, is(true));
                                assertThat(node.node.getKey(), is(key));
                                assertThat(node, is(nodes[i]));
                                nodes[i] = node;

                                if ((i % 2) == 1) {
                                    TestField field = node.node.getField(1);
                                    assertThat(field.opened, is(true));
                                }
                            }
                        }

                        Map<TestNode, TestNode> deletedNodes2 = new HashMap<TestNode, TestNode>();
                        for (int i = 0; i < COUNT / 4; i++) {
                            final TestNode node1 = (nodes[4 * i + 3]);
                            TestField field = node1.node.getField(1);
                            deletedNodes2.put(node1, node1);
                            node1.node.delete();
                            node1.reset();
                            field.reset();
                        }

                        for (int i = 0; i < COUNT / 4; i++) {
                            Object key = String.valueOf(COUNT + 2 * i);
                            TestNode node2 = space.addNode(key, 1);
                            TestField field2 = node2.node.getField(1);
                            assertThat(deletedNodes2.remove(node2) == node2, is(true));

                            assertThat(node2.node.isModified(), is(true));
                            assertThat(node2.node.isDeleted(), is(false));
                            assertThat(field2.created, is(true));
                            assertThat(field2.deleted, is(false));
                            assertThat(field2.opened, is(false));
                            assertThat(field2.primaryKey, is(key));
                            assertThat(node2.created, is(true));
                            assertThat(node2.deleted, is(false));
                            assertThat(node2.opened, is(false));
                            assertThat(node2.primaryKey, is(key));
                            assertThat(node2.node.getKey(), is(key));
                            assertThat(field2.field.isReadOnly(), is(true));
                        }

                        assertThat(deletedNodes2.isEmpty(), is(true));

                        for (int i = 0; i < COUNT / 4; i++) {
                            Object key1 = Integer.valueOf(COUNT * 2 + 2 * i);
                            TestNode node1 = space.addNode(key1, 0);
                            PrimitiveField field1 = node1.node.getField(1);
                            assertThat(deletedNodes.remove(node1), is(true));

                            assertThat(node1.node.isModified(), is(true));
                            assertThat(node1.node.isDeleted(), is(false));
                            assertThat(node1.created, is(true));
                            assertThat(node1.deleted, is(false));
                            assertThat(node1.opened, is(false));
                            assertThat(node1.primaryKey, is(key1));
                            assertThat(node1.node.getKey(), is(key1));
                            assertThat(field1.isReadOnly(), is(true));

                            Object key2 = String.valueOf("a" + (COUNT * 2 + 2 * i));
                            TestNode node2 = space.addNode(key2, 1);
                            TestField field2 = node2.node.getField(1);
                            assertThat(deletedNodes.remove(node2), is(true));

                            assertThat(node2.node.isModified(), is(true));
                            assertThat(node2.node.isDeleted(), is(false));
                            assertThat(field2.created, is(true));
                            assertThat(field2.deleted, is(false));
                            assertThat(field2.opened, is(false));
                            assertThat(field2.primaryKey, is(key2));
                            assertThat(node2.created, is(true));
                            assertThat(node2.deleted, is(false));
                            assertThat(node2.opened, is(false));
                            assertThat(node2.primaryKey, is(key2));
                            assertThat(node2.node.getKey(), is(key2));
                            assertThat(field2.field.isReadOnly(), is(true));
                        }
                        assertThat(deletedNodes.isEmpty(), is(true));

                        for (int i = 0; i < COUNT; i++) {
                            TestNode node = space.findNodeById(nodes[i].node.getId());
                            assertThat(node, is(nodes[i]));
                        }

                        count = 0;
                        for (@SuppressWarnings("unused") TestNode node : space.<TestNode>getNodes()) {
                            count++;
                        }
                        assertThat(count, is(COUNT));

                        for (int i = 0; i < COUNT / 2; i++) {
                            TestNode node1 = space.addNode(COUNT * 3 + 2 * i, 0);
                            nodes[2 * i] = node1;
                            assertThat(node1.node.getId() > nodes[COUNT - 1].node.getId(), is(true));
                            assertThat(node1.node.isModified(), is(true));
                            assertThat(node1.node.isDeleted(), is(false));
                            assertThat(node1.node.getDeletionCount(), is(0));

                            String key = String.valueOf(COUNT * 3 + 2 * i + 1);
                            TestNode node2 = space.addNode(key, 1);
                            assertThat(node2.node.getId() > nodes[COUNT - 1].node.getId(), is(true));
                            nodes[2 * i + 1] = node2;
                            TestField field2 = node2.node.getField(1);
                            assertThat(field2.created, is(true));
                            assertThat(field2.deleted, is(false));
                            assertThat(field2.opened, is(false));
                            assertThat(field2.primaryKey, is(key));
                            assertThat(node2.created, is(true));
                            assertThat(node2.deleted, is(false));
                            assertThat(node2.opened, is(false));
                            assertThat(node2.primaryKey, is((Object) key));
                        }

                        TestNode node = space.addNode(-1, 0);
                        ((TestNodeSchema) node.node.getSchema()).invalid = true;
                    }
                });
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                int count = 0;
                for (@SuppressWarnings("unused") TestNode node1 : space.<TestNode>getNodes())
                    count++;
                assertThat(count, is(COUNT - COUNT / 2));
            }
        });
    }

    @Test
    public void testPerformance() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3"), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final long[] ids = new long[COUNT];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = dataSchema.getSpace();

                int p = 0;

                long t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestNode node = space.addNode(i, 0);
                        ids[i] = node.node.getId();
                        TestField field = node.node.getField(0);
                        p += field.field.getSchema().getIndex();
                    }

                System.out.println("write add node: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                Iterable<TestNode> n = space.getNodes();
                for (Iterator<TestNode> it = n.iterator(); it.hasNext(); ) {
                    TestNode node = it.next();
                    TestField field = node.node.getField(0);
                    p += field.field.getSchema().getIndex();
                }

                System.out.println("write space iterator: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestNode node = space.findNodeById(ids[i]);
                        TestField field = node.node.getField(0);
                        p += field.field.getSchema().getIndex();
                    }
                System.out.println("write find: " + (Times.getCurrentTime() - t) + " " + p);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                int p = 0;
                long t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestNode node = space.findNodeById(ids[i]);
                        TestField field = node.node.getField(0);
                        p += field.field.getSchema().getIndex();
                    }

                System.out.println("write add node2: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                Iterable<TestNode> n = space.getNodes();
                for (Iterator<TestNode> it = n.iterator(); it.hasNext(); ) {
                    TestNode node = it.next();
                    TestField field = node.node.getField(0);
                    p += field.field.getSchema().getIndex();
                }

                System.out.println("write space iterator2: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestNode node = space.findNodeById(ids[i]);
                        TestField field = node.node.getField(0);
                        p += field.field.getSchema().getIndex();
                    }
                System.out.println("write find2: " + (Times.getCurrentTime() - t) + " " + p);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                int p = 0;
                long t = Times.getCurrentTime();
                Iterable<TestNode> n = space.getNodes();
                for (Iterator<TestNode> it = n.iterator(); it.hasNext(); ) {
                    TestNode node = it.next();
                    TestField field = node.node.getField(0);
                    p += field.field.getSchema().getIndex();
                }

                System.out.println("read space iterator: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestNode node = space.findNodeById(ids[i]);
                        TestField field = node.node.getField(0);
                        p += field.field.getSchema().getIndex();
                    }
                System.out.println("read find: " + (Times.getCurrentTime() - t) + " " + p);
            }
        });
    }

    @Test
    public void testPrimitiveFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new PrimitiveFieldSchemaConfiguration("field1", DataType.BYTE),
                        new PrimitiveFieldSchemaConfiguration("field2", DataType.SHORT),
                        new PrimitiveFieldSchemaConfiguration("field3", DataType.CHAR),
                        new PrimitiveFieldSchemaConfiguration("field4", DataType.INT),
                        new PrimitiveFieldSchemaConfiguration("field5", DataType.LONG),
                        new PrimitiveFieldSchemaConfiguration("field6", DataType.BOOLEAN),
                        new PrimitiveFieldSchemaConfiguration("field7", DataType.DOUBLE),
                        new IndexedNumericFieldSchemaConfiguration("field", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                assertThat(node1.node.isModified(), is(true));

                IPrimitiveField field1 = node1.node.getField(0);
                field1.setByte((byte) 123);
                IPrimitiveField field2 = node1.node.getField(1);
                field2.setShort((short) 45678);
                IPrimitiveField field3 = node1.node.getField(2);
                field3.setChar('A');
                IPrimitiveField field4 = node1.node.getField(3);
                field4.setInt(Integer.MAX_VALUE);
                IPrimitiveField field5 = node1.node.getField(4);
                field5.setLong(Long.MAX_VALUE);
                IPrimitiveField field6 = node1.node.getField(5);
                field6.setBoolean(true);
                IPrimitiveField field7 = node1.node.getField(6);
                field7.setDouble(Double.MAX_VALUE);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                assertThat(node1.node.isModified(), is(false));
                IPrimitiveField field1 = node1.node.getField(0);
                assertThat(field1.getByte(), is((byte) 123));
                field1.setByte((byte) 23);
                IPrimitiveField field2 = node1.node.getField(1);
                assertThat(field2.getShort(), is((short) 45678));
                field2.setShort((short) 5678);
                IPrimitiveField field3 = node1.node.getField(2);
                assertThat(field3.getChar(), is('A'));
                field3.setChar('B');
                IPrimitiveField field4 = node1.node.getField(3);
                assertThat(field4.getInt(), is(Integer.MAX_VALUE));
                field4.setInt(Integer.MAX_VALUE - 1);
                IPrimitiveField field5 = node1.node.getField(4);
                assertThat(field5.getLong(), is(Long.MAX_VALUE));
                field5.setLong(Long.MAX_VALUE - 1);
                IPrimitiveField field6 = node1.node.getField(5);
                assertThat(field6.getBoolean(), is(true));
                field6.setBoolean(false);
                IPrimitiveField field7 = node1.node.getField(6);
                assertThat(field7.getDouble(), is(Double.MAX_VALUE));
                field7.setDouble(Double.MAX_VALUE - 1);
                assertThat(node1.node.isModified(), is(true));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.findNodeById(ids[0]);
                    IPrimitiveField field1 = node1.node.getField(0);
                    assertThat(field1.getByte(), is((byte) 23));
                    IPrimitiveField field2 = node1.node.getField(1);
                    assertThat(field2.getShort(), is((short) 5678));
                    IPrimitiveField field3 = node1.node.getField(2);
                    assertThat(field3.getChar(), is('B'));
                    IPrimitiveField field4 = node1.node.getField(3);
                    assertThat(field4.getInt(), is(Integer.MAX_VALUE - 1));
                    IPrimitiveField field5 = node1.node.getField(4);
                    assertThat(field5.getLong(), is(Long.MAX_VALUE - 1));
                    IPrimitiveField field6 = node1.node.getField(5);
                    assertThat(field6.getBoolean(), is(false));
                    IPrimitiveField field7 = node1.node.getField(6);
                    assertThat(field7.getDouble(), is(Double.MAX_VALUE - 1));

                    final IPrimitiveField f1 = field1;
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            f1.setByte((byte) 123);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testFileFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new FileFieldSchemaConfiguration("field1", "field1", null, 0, 0, null,
                                PageType.NORMAL, false, java.util.Collections.<String, String>emptyMap()),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                IFileField field1 = node1.node.getField(0);
                TestNode node2 = space.addNode(234, 0);
                ids[1] = node2.node.getId();
                IFileField field2 = node2.node.getField(0);
                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    writeRegion(i, page1.getWriteRegion());

                    IRawPage page2 = field2.getPage(i);
                    writeRegion(i, page2.getWriteRegion());
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IFileField field1 = node1.node.getField(0);
                TestNode node2 = space.findNodeById(ids[1]);
                IFileField field2 = node2.node.getField(0);

                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i));

                    IRawPage page2 = field2.getPage(i);
                    checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i));

                    writeRegion(i + 1, page1.getWriteRegion());
                    writeRegion(i + 2, page2.getWriteRegion());
                }

                assertThat(node1.node.isModified(), is(false));
                assertThat(node2.node.isModified(), is(false));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.findNodeById(ids[0]);
                    IFileField field1 = node1.node.getField(0);
                    TestNode node2 = space.findNodeById(ids[1]);
                    IFileField field2 = node2.node.getField(0);

                    for (int i = 0; i < 10; i++) {
                        IRawPage page1 = field1.getPage(i);
                        checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i + 1));

                        final IRawPage page2 = field2.getPage(i);
                        checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i + 2));

                        final IFileField f = field1;
                        new Expected(IllegalStateException.class, new Runnable() {
                            @Override
                            public void run() {
                                page2.getWriteRegion();
                            }
                        });

                        new Expected(UnsupportedOperationException.class, new Runnable() {
                            @Override
                            public void run() {
                                f.getFile().delete();
                            }
                        });
                    }
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        final String[] s = new String[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IFileField field1 = node1.node.getField(0);
                IRawDataFile file = field1.getFile();
                node1.node.delete();
                assertThat(file.isDeleted(), is(true));
                assertThat(new File(file.getPath()).exists(), is(true));
                s[0] = file.getPath();
            }
        });

        assertThat(new File(s[0]).exists(), is(false));
    }

    @Test
    public void testFileFieldsWithBinding() throws Throwable {
        database.close();

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(new File(tempDir, "db1").getPath());
        builder.addPath(new File(tempDir, "db2").getPath());
        builder.addPath(new File(tempDir, "db3").getPath());
        builder.setTimerPeriod(1000000);

        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new FileFieldSchemaConfiguration("field1", "field1", null, 0, 0, "fd1", PageType.NORMAL,
                                false, java.util.Collections.<String, String>emptyMap()),
                        new FileFieldSchemaConfiguration("field2", "field2", null, 1, 0, "fd2", PageType.NORMAL,
                                false, java.util.Collections.<String, String>emptyMap()),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                IFileField field11 = node1.node.getField(0);
                IFileField field12 = node1.node.getField(1);
                TestNode node2 = space.findOrCreateNode(234, space.getSchema().getNodes().get(0), 2, 300, 0, "fd3");
                ids[1] = node2.node.getId();
                IFileField field21 = node2.node.getField(0);
                IFileField field22 = node2.node.getField(1);
                for (int i = 0; i < 10; i++) {
                    IRawPage page11 = field11.getPage(i);
                    assertThat(page11.getSize(), is(0x4000));
                    assertThat(page11.getFile().getPath().contains("db1"), is(true));
                    assertThat(page11.getFile().getPath().contains("fd1"), is(true));

                    writeRegion(i, page11.getWriteRegion());
                    IRawPage page12 = field12.getPage(i);
                    assertThat(page12.getSize(), is(0x4000));
                    assertThat(page12.getFile().getPath().contains("db2"), is(true));
                    assertThat(page12.getFile().getPath().contains("fd2"), is(true));
                    writeRegion(i + 1, page12.getWriteRegion());

                    IRawPage page21 = field21.getPage(i);
                    assertThat(page21.getSize(), is(0x4000));
                    assertThat(page21.getFile().getPath().contains("db3"), is(true));
                    assertThat(page21.getFile().getPath().contains("fd3"), is(true));
                    writeRegion(i, page21.getWriteRegion());
                    IRawPage page22 = field22.getPage(i);
                    assertThat(page22.getSize(), is(0x4000));
                    assertThat(page22.getFile().getPath().contains("db2"), is(true));
                    assertThat(page22.getFile().getPath().contains("fd2"), is(true));
                    writeRegion(i + 1, page22.getWriteRegion());
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IFileField field11 = node1.node.getField(0);
                IFileField field12 = node1.node.getField(1);
                TestNode node2 = space.findNodeById(ids[1]);
                IFileField field21 = node2.node.getField(0);
                IFileField field22 = node2.node.getField(1);

                for (int i = 0; i < 10; i++) {
                    IRawPage page11 = field11.getPage(i);
                    assertThat(page11.getSize(), is(0x4000));
                    assertThat(page11.getFile().getPath().contains("db1"), is(true));
                    assertThat(page11.getFile().getPath().contains("fd1"), is(true));
                    checkRegion(page11.getReadRegion(), createBuffer(page11.getSize(), i));
                    IRawPage page12 = field12.getPage(i);
                    assertThat(page12.getSize(), is(0x4000));
                    assertThat(page12.getFile().getPath().contains("db2"), is(true));
                    assertThat(page12.getFile().getPath().contains("fd2"), is(true));
                    checkRegion(page12.getReadRegion(), createBuffer(page12.getSize(), i + 1));

                    IRawPage page21 = field21.getPage(i);
                    assertThat(page21.getSize(), is(0x4000));
                    checkRegion(page21.getReadRegion(), createBuffer(page21.getSize(), i));
                    IRawPage page22 = field22.getPage(i);
                    assertThat(page22.getFile().getPath().contains("db2"), is(true));
                    assertThat(page22.getFile().getPath().contains("fd2"), is(true));
                    checkRegion(page22.getReadRegion(), createBuffer(page22.getSize(), i + 1));

                    writeRegion(i + 1, page11.getWriteRegion());
                    writeRegion(i + 2, page12.getWriteRegion());
                    writeRegion(i + 3, page21.getWriteRegion());
                    writeRegion(i + 4, page22.getWriteRegion());
                }

                assertThat(node1.node.isModified(), is(false));
                assertThat(node2.node.isModified(), is(false));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.findNodeById(ids[0]);
                    IFileField field11 = node1.node.getField(0);
                    IFileField field12 = node1.node.getField(1);
                    TestNode node2 = space.findNodeById(ids[1]);
                    IFileField field21 = node2.node.getField(0);
                    IFileField field22 = node2.node.getField(1);

                    for (int i = 0; i < 10; i++) {
                        IRawPage page11 = field11.getPage(i);
                        checkRegion(page11.getReadRegion(), createBuffer(page11.getSize(), i + 1));
                        IRawPage page12 = field12.getPage(i);
                        checkRegion(page12.getReadRegion(), createBuffer(page12.getSize(), i + 2));

                        final IRawPage page21 = field21.getPage(i);
                        checkRegion(page21.getReadRegion(), createBuffer(page21.getSize(), i + 3));
                        final IRawPage page22 = field22.getPage(i);
                        checkRegion(page22.getReadRegion(), createBuffer(page22.getSize(), i + 4));

                        final IFileField f = field11;
                        new Expected(IllegalStateException.class, new Runnable() {
                            @Override
                            public void run() {
                                page21.getWriteRegion();
                            }
                        });

                        new Expected(UnsupportedOperationException.class, new Runnable() {
                            @Override
                            public void run() {
                                f.getFile().delete();
                            }
                        });
                    }
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        final String[] s = new String[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IFileField field11 = node1.node.getField(0);
                IFileField field12 = node1.node.getField(1);
                IRawDataFile file11 = field11.getFile();
                IRawDataFile file12 = field12.getFile();
                node1.node.delete();
                assertThat(file11.isDeleted(), is(true));
                assertThat(file12.isDeleted(), is(true));
                assertThat(new File(file11.getPath()).exists(), is(true));
                assertThat(new File(file12.getPath()).exists(), is(true));

                s[0] = file11.getPath();
                s[1] = file12.getPath();
            }
        });
        assertThat(new File(s[0]).exists(), is(false));
        assertThat(new File(s[1]).exists(), is(false));
    }

    @Test
    public void testReferenceFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final TestNode[] nodes = new TestNode[10000];
        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123000, 0);
                ids[0] = node1.node.getId();
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                TestNode node2 = space.addNode(234000, 0);
                ids[1] = node2.node.getId();
                IReferenceField<TestNode> field2 = node2.node.getField(0);
                TestNode node3 = space.addNode(345000, 0);
                ids[2] = node3.node.getId();
                IReferenceField<TestNode> field3 = node3.node.getField(0);
                TestNode node4 = space.addNode(456000, 0);
                ids[3] = node4.node.getId();
                IReferenceField<TestNode> field4 = node4.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space.addNode(i, 1);

                long t = Times.getCurrentTime();

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                field3.add(nodes[0]);
                field3.add(nodes[1]);
                field3.add(nodes[2]);

                field4.add(nodes[0]);
                field4.add(nodes[1]);
                field4.add(nodes[2]);

                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
                System.out.println("add reference: " + (Times.getCurrentTime() - t));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node3 = space.findNodeById(ids[2]);
                IReferenceField<TestNode> field3 = node3.node.getField(0);

                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode child1 = space.findNodeById(nodes[0].node.getId());
                    TestNode child4 = space.findNodeById(nodes[3].node.getId());
                    TestNode child5 = space.findNodeById(nodes[4].node.getId());

                    ISingleReferenceField<TestNode> field11 = child1.node.getField(0);
                    field11.set(null);

                    TestNode node1 = space.findNodeById(ids[0]);
                    field11.set(node1);
                    TestNode node2 = space.findNodeById(ids[1]);
                    IReferenceField<TestNode> field2 = node2.node.getField(0);
                    field2.add(child1);
                    assertThat(node2.node.isModified(), is(true));

                    TestNode node3 = space.findNodeById(ids[2]);
                    IReferenceField<TestNode> field3 = node3.node.getField(0);
                    final Iterator<TestNode> it1 = field3.iterator();
                    TestNode node4 = space.findNodeById(ids[3]);
                    IReferenceField<TestNode> field4 = node4.node.getField(0);
                    Iterator<TestNode> it4 = field4.iterator();
                    for (int i = 0; i < 2; i++) {
                        assertThat(it1.next(), is(nodes[i]));
                        assertThat(it4.next(), is(nodes[i]));
                    }

                    field3.add(child4);
                    field3.add(child5);

                    field4.add(child4);
                    field4.add(child5);

                    new Expected(ConcurrentModificationException.class, new Runnable() {
                        @Override
                        public void run() {
                            it1.next();
                        }
                    });

                    assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2], nodes[3], nodes[4])));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node4 = space.findNodeById(ids[3]);
                IReferenceField<TestNode> field4 = node4.node.getField(0);

                assertThat(new HashSet<TestNode>(Collections.toList(field4.iterator())), is(Collections.asSet(nodes[0], nodes[1], nodes[3], nodes[4], nodes[2])));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                List<TestNode> refs1 = Collections.toList(field1.iterator());
                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);
                List<TestNode> refs2 = Collections.toList(field2.iterator());

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        assertThat(field.get().equals(node1), is(true));
                        assertThat(refs1.indexOf(node) != -1, is(true));
                    } else {
                        assertThat(field.get().equals(node2), is(true));
                        assertThat(refs2.indexOf(node) != -1, is(true));
                    }
                }

                TestNode node3 = space.findNodeById(ids[2]);
                IReferenceField<TestNode> field3 = node3.node.getField(0);
                TestNode node4 = space.findNodeById(ids[3]);
                IReferenceField<TestNode> field4 = node4.node.getField(0);
                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2], nodes[3], nodes[4])));
                assertThat(new HashSet<TestNode>(Collections.toList(field4.iterator())), is(Collections.asSet(nodes[0], nodes[1], nodes[3], nodes[4], nodes[2])));
            }
        });
    }

    @Test
    public void testReferenceFieldStableOrder() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", true),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
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
        final TestNode[] nodes = new TestNode[COUNT];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123000, 0);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space.addNode(i, 1);

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    field1.add(node);
                    field.set(node1);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeSchema nodeSchema = dataSchema.getNodes().get(0);

                TestNode node1 = space.findNode(123000, nodeSchema);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                int i = 0;
                for (TestNode node : field1) {
                    assertThat(((IObjectNode) node.node).getKey(), is((Object) i));
                    i++;
                }

                assertThat(i, is(COUNT));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeSchema nodeSchema = dataSchema.getNodes().get(0);

                TestNode node1 = space.findNode(123000, nodeSchema);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                for (int i = 0; i < 10; i++)
                    field1.add((TestNode) space.addNode(COUNT + i, 1));

                int i = 0;
                for (TestNode node : field1) {
                    assertThat(((IObjectNode) node.node).getKey(), is((Object) i));
                    i++;
                }

                assertThat(i, is(COUNT + 10));
            }
        });
    }

    @Test
    public void testReferenceDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final TestNode[] nodes = new TestNode[10000];
        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123000, 0);
                ids[0] = node1.node.getId();
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                TestNode node2 = space.addNode(234000, 0);
                ids[1] = node2.node.getId();
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space.addNode(i, 1);

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (int i = 0; i < nodes.length / 2; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.remove(node);
                        field.set(null);
                    } else {
                        field2.remove(node);
                        field.set(null);
                    }
                }
                field1.add(nodes[0]);
                ((ISingleReferenceField<TestNode>) nodes[0].node.getField(0)).set(node1);

                HashSet<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                HashSet<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));
                for (int i = 0; i < nodes.length; i++) {
                    if (i > 0 && i < nodes.length / 2) {
                        TestNode node = nodes[i];
                        ISingleReferenceField<TestNode> field = node.node.getField(0);

                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        TestNode node = nodes[i];
                        ISingleReferenceField<TestNode> field = node.node.getField(0);

                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if (i > 0 && i < nodes.length / 2) {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }

                for (int i = 1; i < nodes.length / 2; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (int i = nodes.length / 2; i < nodes.length; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.remove(node);
                        field.set(null);
                    } else {
                        field2.remove(node);
                        field.set(null);
                    }
                }
                TestNode n = (TestNode) space.findNodeById(nodes[0].node.getId());
                field1.remove(n);
                ((ISingleReferenceField<TestNode>) n.node.getField(0)).set(null);

                set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if (i == 0 || i >= nodes.length / 2) {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if (i == 0 || i >= nodes.length / 2) {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }

                TestNode n = (TestNode) space.findNodeById(nodes[0].node.getId());
                field1.add(n);
                ((ISingleReferenceField<TestNode>) n.node.getField(0)).set(null);

                n = (TestNode) space.findNodeById(nodes[1].node.getId());
                field1.remove(n);
                ((ISingleReferenceField<TestNode>) n.node.getField(0)).set(null);
                field1.clear();
                set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set1.isEmpty(), is(true));

                TestNode node10 = space.addNode(111111111, 0);
                ids[2] = node10.node.getId();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node10 = space.findNodeById(ids[2]);
                IReferenceField<TestNode> field10 = node10.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field10.iterator()));
                assertThat(set1.isEmpty(), is(true));

                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set2.isEmpty(), is(true));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                for (int i = 0; i < nodes.length / 2; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                field1.iterator().next();
                field2.iterator().next();

                for (int i = nodes.length / 2; i < nodes.length; i++) {
                    TestNode node = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (Iterator it = field1.iterator(); it.hasNext(); ) {
                    it.next();
                    it.remove();
                }

                for (Iterator it = field2.iterator(); it.hasNext(); ) {
                    it.next();
                    it.remove();
                }

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set1.isEmpty(), is(true));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));
                assertThat(set2.isEmpty(), is(true));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(null, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set1.isEmpty(), is(true));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));
                assertThat(set2.isEmpty(), is(true));
            }
        });
    }

    @Test
    public void testReferenceAutoDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final TestNode[] nodes = new TestNode[10000];
        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123000, 0);
                ids[0] = node1.node.getId();
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                TestNode node2 = space.addNode(234000, 0);
                ids[1] = node2.node.getId();
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space.addNode(i, 1);

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (int i = 0; i < nodes.length; i++)
                    nodes[i].node.delete();

                for (int i = 0; i < nodes.length; i++) {
                    nodes[i] = space.addNode(i, 1);
                    ISingleReferenceField<TestNode> field = nodes[i].node.getField(0);
                    field.set(node1);
                }

                assertThat(field1.iterator().hasNext(), is(false));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                assertThat(field1.iterator().hasNext(), is(false));
                assertThat(field2.iterator().hasNext(), is(false));

                node1.node.delete();

                for (int i = 0; i < nodes.length / 2; i++) {
                    nodes[i] = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = nodes[i].node.getField(0);
                    assertThat(field.get(), nullValue());
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                for (int i = nodes.length / 2; i < nodes.length; i++) {
                    nodes[i] = space.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = nodes[i].node.getField(0);
                    assertThat(field.get(), nullValue());
                }
            }
        });
    }


    @Test
    public void testExternalReferenceFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2", "test2.space2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1", "test1.space1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "", new HashSet(Arrays.asList(
                nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(space1)));
        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", "test", null, new Version(1, 0, 0),
                        new DatabaseSchemaConfiguration("db",
                                com.exametrika.common.utils.Collections.asSet(configuration1, configuration2)),
                        java.util.Collections.<ModuleDependencySchemaConfiguration>emptySet()), null);
            }
        });

        final TestNode[] nodes = new TestNode[10000];
        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.addNode(123000, 0);
                ids[0] = node1.node.getId();
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                TestNode node2 = space1.addNode(234000, 0);
                ids[1] = node2.node.getId();
                IReferenceField<TestNode> field2 = node2.node.getField(0);
                TestNode node3 = space1.addNode(345000, 0);
                ids[2] = node3.node.getId();
                IReferenceField<TestNode> field3 = node3.node.getField(0);
                TestNode node4 = space1.addNode(456000, 0);
                ids[3] = node4.node.getId();
                IReferenceField<TestNode> field4 = node4.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space2.addNode(i, 0);

                long t = Times.getCurrentTime();

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                field3.add(nodes[0]);
                field3.add(nodes[1]);
                field3.add(nodes[2]);

                field4.add(nodes[0]);
                field4.add(nodes[1]);
                field4.add(nodes[2]);

                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
                System.out.println("add reference: " + (Times.getCurrentTime() - t));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                TestNode node3 = space1.findNodeById(ids[2]);
                IReferenceField<TestNode> field3 = node3.node.getField(0);

                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                    ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                    IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                    ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                    TestNode child1 = space2.findNodeById(nodes[0].node.getId());
                    TestNode child4 = space2.findNodeById(nodes[3].node.getId());
                    TestNode child5 = space2.findNodeById(nodes[4].node.getId());

                    ISingleReferenceField<TestNode> field11 = child1.node.getField(0);
                    field11.set(null);

                    TestNode node1 = space1.findNodeById(ids[0]);
                    field11.set(node1);
                    TestNode node2 = space1.findNodeById(ids[1]);
                    IReferenceField<TestNode> field2 = node2.node.getField(0);
                    field2.add(child1);
                    assertThat(node2.node.isModified(), is(true));

                    TestNode node3 = space1.findNodeById(ids[2]);
                    IReferenceField<TestNode> field3 = node3.node.getField(0);
                    final Iterator<TestNode> it1 = field3.iterator();
                    TestNode node4 = space1.findNodeById(ids[3]);
                    IReferenceField<TestNode> field4 = node4.node.getField(0);
                    Iterator<TestNode> it4 = field4.iterator();
                    for (int i = 0; i < 2; i++) {
                        assertThat(it1.next(), is(nodes[i]));
                        assertThat(it4.next(), is(nodes[i]));
                    }

                    field3.add(child4);
                    field3.add(child5);

                    field4.add(child4);
                    field4.add(child5);

                    new Expected(ConcurrentModificationException.class, new Runnable() {
                        @Override
                        public void run() {
                            it1.next();
                        }
                    });

                    assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2], nodes[3], nodes[4])));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node4 = space.findNodeById(ids[3]);
                IReferenceField<TestNode> field4 = node4.node.getField(0);

                assertThat(new HashSet<TestNode>(Collections.toList(field4.iterator())), is(Collections.asSet(nodes[0], nodes[1], nodes[3], nodes[4], nodes[2])));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                List<TestNode> refs1 = Collections.toList(field1.iterator());
                TestNode node2 = space1.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);
                List<TestNode> refs2 = Collections.toList(field2.iterator());

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        assertThat(field.get().equals(node1), is(true));
                        assertThat(refs1.indexOf(node) != -1, is(true));
                    } else {
                        assertThat(field.get().equals(node2), is(true));
                        assertThat(refs2.indexOf(node) != -1, is(true));
                    }
                }

                TestNode node3 = space1.findNodeById(ids[2]);
                IReferenceField<TestNode> field3 = node3.node.getField(0);
                TestNode node4 = space1.findNodeById(ids[3]);
                IReferenceField<TestNode> field4 = node4.node.getField(0);
                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2], nodes[3], nodes[4])));
                assertThat(new HashSet<TestNode>(Collections.toList(field4.iterator())), is(Collections.asSet(nodes[0], nodes[1], nodes[3], nodes[4], nodes[2])));
            }
        });
    }

    @Test
    public void testExternalReferenceDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2", "test2.space2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1", "test1.space1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "",
                new HashSet(Arrays.asList(nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(space1)));
        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0),
                        Collections.asSet(configuration1, configuration2)), null);
            }
        });

        final TestNode[] nodes = new TestNode[10000];
        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.addNode(123000, 0);
                ids[0] = node1.node.getId();
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                TestNode node2 = space1.addNode(234000, 0);
                ids[1] = node2.node.getId();
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space2.addNode(i, 0);

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (int i = 0; i < nodes.length / 2; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.remove(node);
                        field.set(null);
                    } else {
                        field2.remove(node);
                        field.set(null);
                    }
                }
                field1.add(nodes[0]);
                ((ISingleReferenceField<TestNode>) nodes[0].node.getField(0)).set(node1);

                HashSet<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                HashSet<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));
                for (int i = 0; i < nodes.length; i++) {
                    if (i > 0 && i < nodes.length / 2) {
                        TestNode node = nodes[i];
                        ISingleReferenceField<TestNode> field = node.node.getField(0);

                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        TestNode node = nodes[i];
                        ISingleReferenceField<TestNode> field = node.node.getField(0);

                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space1.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if (i > 0 && i < nodes.length / 2) {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }

                for (int i = 1; i < nodes.length / 2; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (int i = nodes.length / 2; i < nodes.length; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.remove(node);
                        field.set(null);
                    } else {
                        field2.remove(node);
                        field.set(null);
                    }
                }
                TestNode n = (TestNode) space2.findNodeById(nodes[0].node.getId());
                field1.remove(n);
                ((ISingleReferenceField<TestNode>) n.node.getField(0)).set(null);

                set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if (i == 0 || i >= nodes.length / 2) {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space1.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if (i == 0 || i >= nodes.length / 2) {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        } else {
                            assertThat(set2.contains(node), is(false));
                            assertThat(field.get(), nullValue());
                        }
                    } else {
                        if ((i % 2) == 0) {
                            assertThat(set1.contains(node), is(true));
                            assertThat(field.get() == node1, is(true));
                        } else {
                            assertThat(set2.contains(node), is(true));
                            assertThat(field.get() == node2, is(true));
                        }
                    }
                }

                TestNode n = (TestNode) space2.findNodeById(nodes[0].node.getId());
                field1.add(n);
                ((ISingleReferenceField<TestNode>) n.node.getField(0)).set(null);

                n = (TestNode) space2.findNodeById(nodes[1].node.getId());
                field1.remove(n);
                ((ISingleReferenceField<TestNode>) n.node.getField(0)).set(null);
                field1.clear();
                set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set1.isEmpty(), is(true));

                TestNode node10 = space1.addNode(111111111, 0);
                ids[2] = node10.node.getId();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node10 = space.findNodeById(ids[2]);
                IReferenceField<TestNode> field10 = node10.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field10.iterator()));
                assertThat(set1.isEmpty(), is(true));

                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set2.isEmpty(), is(true));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space1.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                for (int i = 0; i < nodes.length / 2; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space1.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                field1.iterator().next();
                field2.iterator().next();

                for (int i = nodes.length / 2; i < nodes.length; i++) {
                    TestNode node = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (Iterator it = field1.iterator(); it.hasNext(); ) {
                    it.next();
                    it.remove();
                }

                for (Iterator it = field2.iterator(); it.hasNext(); ) {
                    it.next();
                    it.remove();
                }

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set1.isEmpty(), is(true));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));
                assertThat(set2.isEmpty(), is(true));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(null, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                Set<TestNode> set1 = new HashSet<TestNode>(Collections.toList(field1.iterator()));
                assertThat(set1.isEmpty(), is(true));
                Set<TestNode> set2 = new HashSet<TestNode>(Collections.toList(field2.iterator()));
                assertThat(set2.isEmpty(), is(true));
            }
        });
    }

    @Test
    public void testExternalReferenceAutoDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2", "test2.space2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1", "test1.space1"),
                        new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "",
                new HashSet(Arrays.asList(nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(space1)));
        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0),
                        Collections.asSet(configuration1, configuration2)), null);
            }
        });

        final TestNode[] nodes = new TestNode[10000];
        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.addNode(123000, 0);
                ids[0] = node1.node.getId();
                IReferenceField<TestNode> field1 = node1.node.getField(0);
                TestNode node2 = space1.addNode(234000, 0);
                ids[1] = node2.node.getId();
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space2.addNode(i, 0);

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];
                    ISingleReferenceField<TestNode> field = node.node.getField(0);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                for (int i = 0; i < nodes.length; i++)
                    nodes[i].node.delete();

                for (int i = 0; i < nodes.length; i++) {
                    nodes[i] = space2.addNode(i, 0);
                    ISingleReferenceField<TestNode> field = nodes[i].node.getField(0);
                    field.set(node1);
                }

                assertThat(field1.iterator().hasNext(), is(false));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema1 = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                ObjectSpace space1 = (ObjectSpace) dataSchema1.getSpace();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestNode node1 = space1.findNodeById(ids[0]);
                IReferenceField<TestNode> field1 = node1.node.getField(0);

                TestNode node2 = space1.findNodeById(ids[1]);
                IReferenceField<TestNode> field2 = node2.node.getField(0);

                assertThat(field1.iterator().hasNext(), is(false));
                assertThat(field2.iterator().hasNext(), is(false));

                node1.node.delete();

                for (int i = 0; i < nodes.length / 2; i++) {
                    nodes[i] = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = nodes[i].node.getField(0);
                    assertThat(field.get(), nullValue());
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                for (int i = nodes.length / 2; i < nodes.length; i++) {
                    nodes[i] = space2.findNodeById(nodes[i].node.getId());
                    ISingleReferenceField<TestNode> field = nodes[i].node.getField(0);
                    assertThat(field.get(), nullValue());
                }
            }
        });
    }

    @Test
    public void testComplexFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3"), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] areaBlockIndex = new long[2];
        final int[] areaOffset = new int[2];
        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.addNode(123, 0);
                    ids[0] = node1.node.getId();
                    TestField field11 = node1.node.getField(0);

                    TestNode node2 = space.addNode(234, 1);
                    ids[1] = node2.node.getId();
                    TestField field21 = node2.node.getField(0);
                    TestField field22 = node2.node.getField(1);

                    TestNode node3 = space.addNode(345, 2);
                    ids[2] = node3.node.getId();
                    TestField field31 = node3.node.getField(0);
                    TestField field32 = node3.node.getField(1);
                    TestField field33 = node3.node.getField(2);

                    IFieldSerialization serialization11 = field11.field.createSerialization();
                    IFieldSerialization serialization21 = field21.field.createSerialization();
                    IFieldSerialization serialization22 = field22.field.createSerialization();
                    IFieldSerialization serialization31 = field31.field.createSerialization();
                    IFieldSerialization serialization32 = field32.field.createSerialization();
                    IFieldSerialization serialization33 = field33.field.createSerialization();

                    for (int i = 0; i < 10000; i++) {
                        if (i == 5000) {
                            areaBlockIndex[0] = serialization11.getAreaId();
                            areaOffset[0] = serialization11.getAreaOffset();
                        }
                        serialization11.writeInt(i);
                        serialization21.writeInt(i);
                        serialization22.writeInt(i);
                        serialization31.writeInt(i);
                        serialization32.writeInt(i);
                        serialization33.writeInt(i);
                    }

                    areaBlockIndex[1] = serialization11.getAreaId();
                    areaOffset[1] = serialization11.getAreaOffset();
                    serialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
                    serialization11.writeInt(5000);
                    serialization11.setPosition(areaBlockIndex[1], areaOffset[1]);

                    ByteArray buf = createBuffer(23456, 123);
                    String str = createString(34567, 9876);

                    serialization11.writeByteArray(buf);
                    serialization21.writeByteArray(buf);
                    serialization22.writeByteArray(buf);
                    serialization31.writeByteArray(buf);
                    serialization32.writeByteArray(buf);
                    serialization33.writeByteArray(buf);

                    serialization11.writeString(str);
                    serialization21.writeString(str);
                    serialization22.writeString(str);
                    serialization31.writeString(str);
                    serialization32.writeString(str);
                    serialization33.writeString(str);

                    IFieldDeserialization deserialization11 = field11.field.createDeserialization();
                    IFieldDeserialization deserialization21 = field21.field.createDeserialization();
                    IFieldDeserialization deserialization22 = field22.field.createDeserialization();
                    IFieldDeserialization deserialization31 = field31.field.createDeserialization();
                    IFieldDeserialization deserialization32 = field32.field.createDeserialization();
                    IFieldDeserialization deserialization33 = field33.field.createDeserialization();

                    for (int i = 0; i < 10000; i++) {
                        assertThat(deserialization33.readInt(), is(i));
                        assertThat(deserialization32.readInt(), is(i));
                        assertThat(deserialization31.readInt(), is(i));
                        assertThat(deserialization22.readInt(), is(i));
                        assertThat(deserialization21.readInt(), is(i));
                        assertThat(deserialization11.readInt(), is(i));
                    }

                    assertThat(deserialization33.readByteArray(), is(buf));
                    assertThat(deserialization33.readString(), is(str));
                    assertThat(deserialization32.readByteArray(), is(buf));
                    assertThat(deserialization32.readString(), is(str));
                    assertThat(deserialization31.readByteArray(), is(buf));
                    assertThat(deserialization31.readString(), is(str));
                    assertThat(deserialization22.readByteArray(), is(buf));
                    assertThat(deserialization22.readString(), is(str));
                    assertThat(deserialization21.readByteArray(), is(buf));
                    assertThat(deserialization21.readString(), is(str));
                    assertThat(deserialization11.readByteArray(), is(buf));
                    assertThat(deserialization11.readString(), is(str));

                    areaBlockIndex[1] = deserialization11.getAreaId();
                    areaOffset[1] = deserialization11.getAreaOffset();

                    deserialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
                    assertThat(deserialization11.readInt(), is(5000));

                    deserialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
                    final IFieldDeserialization d1 = deserialization11;
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 100; i++)
                                assertThat(d1.readInt(), is(0));
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.findNodeById(ids[0]);
                    TestField field11 = node1.node.getField(0);
                    TestNode node2 = space.findNodeById(ids[1]);
                    TestField field21 = node2.node.getField(0);
                    TestField field22 = node2.node.getField(1);
                    TestNode node3 = space.findNodeById(ids[2]);
                    TestField field31 = node3.node.getField(0);
                    TestField field32 = node3.node.getField(1);
                    TestField field33 = node3.node.getField(2);

                    IFieldDeserialization deserialization11 = field11.field.createDeserialization();
                    IFieldDeserialization deserialization21 = field21.field.createDeserialization();
                    IFieldDeserialization deserialization22 = field22.field.createDeserialization();
                    IFieldDeserialization deserialization31 = field31.field.createDeserialization();
                    IFieldDeserialization deserialization32 = field32.field.createDeserialization();
                    IFieldDeserialization deserialization33 = field33.field.createDeserialization();

                    for (int i = 0; i < 10000; i++) {
                        assertThat(deserialization11.readInt(), is(i));
                        assertThat(deserialization21.readInt(), is(i));
                        assertThat(deserialization22.readInt(), is(i));
                        assertThat(deserialization31.readInt(), is(i));
                        assertThat(deserialization32.readInt(), is(i));
                        assertThat(deserialization33.readInt(), is(i));
                    }

                    ByteArray buf = createBuffer(23456, 123);
                    String str = createString(34567, 9876);

                    assertThat(deserialization33.readByteArray(), is(buf));
                    assertThat(deserialization33.readString(), is(str));
                    assertThat(deserialization32.readByteArray(), is(buf));
                    assertThat(deserialization32.readString(), is(str));
                    assertThat(deserialization31.readByteArray(), is(buf));
                    assertThat(deserialization31.readString(), is(str));
                    assertThat(deserialization22.readByteArray(), is(buf));
                    assertThat(deserialization22.readString(), is(str));
                    assertThat(deserialization21.readByteArray(), is(buf));
                    assertThat(deserialization21.readString(), is(str));
                    assertThat(deserialization11.readByteArray(), is(buf));
                    assertThat(deserialization11.readString(), is(str));

                    deserialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
                    assertThat(deserialization11.readInt(), is(5000));

                    deserialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
                    final IFieldDeserialization d2 = deserialization11;
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 100; i++)
                                assertThat(d2.readInt(), is(0));
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testComplexFieldDeletion() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3"), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 1000;
        final TestNode[] nodes = new TestNode[COUNT];
        final long[] n = new long[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    for (int i = 0; i < COUNT; i++) {
                        nodes[i] = space.addNode(i, 0);
                        TestField field11 = nodes[i].node.getField(0);

                        ByteArray buf = createBuffer(200, i);

                        IFieldSerialization serialization = field11.field.createSerialization();
                        serialization.writeByteArray(buf);
                    }

                    n[0] = space.allocateBlocks(1);

                    for (int i = 0; i < COUNT; i++) {
                        TestField field11 = nodes[i].node.getField(0);

                        ByteArray buf = createBuffer(200, i);

                        IFieldDeserialization deserialization = field11.field.createDeserialization();
                        assertThat(deserialization.readByteArray(), is(buf));

                        IFieldSerialization serialization = field11.field.createSerialization();
                        buf = createBuffer(100, i + 1);
                        serialization.writeByteArray(buf);
                        serialization.removeRest();
                    }

                    assertThat(space.allocateBlocks(1), is(n[0] + 1));

                    for (int i = 0; i < COUNT; i++) {
                        TestField field11 = nodes[i].node.getField(0);

                        ByteArray buf = createBuffer(100, i + 1);

                        final IFieldDeserialization deserialization = field11.field.createDeserialization();
                        assertThat(deserialization.readByteArray(), is(buf));

                        new Expected(EndOfStreamException.class, new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < 100; i++)
                                    deserialization.readLong();
                            }
                        });
                    }

                    assertThat(space.allocateBlocks(1), is(n[0] + 2));

                    for (int i = 0; i < COUNT; i++)
                        nodes[i].node.delete();
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                for (int i = 0; i < COUNT; i++) {
                    nodes[i] = space.addNode(i, 0);
                    TestField field11 = nodes[i].node.getField(0);

                    ByteArray buf = createBuffer(200, i);

                    IFieldSerialization serialization = field11.field.createSerialization();
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(0l));
                    serialization = field11.field.createSerialization();
                    serialization.writeLong(0);
                    serialization.writeLong(0);
                    serialization.writeLong(0);
                    serialization.writeLong(0);
                    serialization.writeLong(0);
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(0l));

                    serialization = field11.field.createSerialization();
                    serialization.writeByteArray(buf);
                }

                assertThat(space.allocateBlocks(1), is(n[0] + 3));

                ((TestField) nodes[1].node.getField(0)).field.createSerialization().removeRest();

                TestNode node = space.addNode(-1, 0);
                TestField field = node.node.getField(0);
                IFieldSerialization serialization = field.field.createSerialization();
                serialization.writeByteArray(new ByteArray(new byte[Constants.COMPLEX_FIELD_AREA_DATA_SIZE]));
                assertThat(Constants.pageIndexByBlockIndex(serialization.getAreaId()) == Constants.pageIndexByBlockIndex(node.node.getId()), is(true));

                assertThat(Constants.pageIndexByBlockIndex(nodes[0].node.getId()) != Constants.pageIndexByBlockIndex(nodes[COUNT - 1].node.getId()), is(true));

                TestField field1 = nodes[0].node.getField(0);
                IFieldSerialization serialization1 = field1.field.createSerialization();
                serialization1.removeRest();

                TestField field2 = nodes[COUNT - 1].node.getField(0);
                IFieldSerialization serialization2 = field2.field.createSerialization();
                serialization2.removeRest();

                serialization1.writeByteArray(new ByteArray(new byte[Constants.COMPLEX_FIELD_AREA_DATA_SIZE]));
                assertThat(Constants.pageIndexByBlockIndex(serialization1.getAreaId()) == Constants.pageIndexByBlockIndex(nodes[0].node.getId()), is(true));

                serialization2.writeByteArray(new ByteArray(new byte[Constants.COMPLEX_FIELD_AREA_DATA_SIZE]));
                assertThat(Constants.pageIndexByBlockIndex(serialization2.getAreaId()) == Constants.pageIndexByBlockIndex(nodes[COUNT - 1].node.getId()), is(true));
            }
        });
    }

    @Test
    public void testAreaUsageCount() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3"), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null);

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
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node = space.addNode(0, 0);
                    TestField field = node.node.getField(0);
                    field.field.setAutoRemoveUnusedAreas();
                    IFieldSerialization serialization = field.field.createSerialization();
                    for (int i = 0; i < 28; i++) {
                        serialization.writeLong(i);
                        serialization.incrementCurrentAreaUsageCount();
                    }

                    IFieldDeserialization deserialization = field.field.createDeserialization();
                    long areaBlockIndex = 0;
                    for (int i = 0; i < 28; i++) {
                        assertThat(deserialization.readLong(), is((long) i));
                        if (i == 8)
                            areaBlockIndex = deserialization.getAreaId();
                    }

                    serialization = field.field.createSerialization();
                    serialization.setPosition(areaBlockIndex, 0);
                    for (int i = 0; i < 10; i++) {
                        serialization.writeLong(0);
                        serialization.decrementCurrentAreaUsageCount();
                    }

                    serialization = field.field.createSerialization();
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(1l));
                    assertThat(serialization.readLong(), is(2l));
                    assertThat(serialization.readLong(), is(3l));
                    assertThat(serialization.readLong(), is(4l));
                    assertThat(serialization.readLong(), is(5l));
                    assertThat(serialization.readLong(), is(6l));
                    assertThat(serialization.readLong(), is(7l));
                    for (long i = 18; i < 28; i++) {
                        assertThat(serialization.readLong(), is(i));
                        serialization.decrementCurrentAreaUsageCount();
                    }
                    final IFieldDeserialization deserialization2 = serialization;
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            deserialization2.readLong();
                        }
                    });

                    serialization = field.field.createSerialization();
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(1l));
                    assertThat(serialization.readLong(), is(2l));
                    assertThat(serialization.readLong(), is(3l));
                    assertThat(serialization.readLong(), is(4l));
                    assertThat(serialization.readLong(), is(5l));
                    assertThat(serialization.readLong(), is(6l));
                    assertThat(serialization.readLong(), is(7l));
                    final IFieldDeserialization deserialization3 = serialization;
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            deserialization3.readLong();
                        }
                    });
                    serialization = field.field.createSerialization();
                    assertThat(serialization.readLong(), is(0l));
                    assertThat(serialization.readLong(), is(1l));
                    assertThat(serialization.readLong(), is(2l));
                    assertThat(serialization.readLong(), is(3l));
                    assertThat(serialization.readLong(), is(4l));
                    assertThat(serialization.readLong(), is(5l));
                    assertThat(serialization.readLong(), is(6l));
                    assertThat(serialization.readLong(), is(7l));
                    for (long i = 8; i < 18; i++)
                        serialization.writeLong(i);

                    serialization.incrementCurrentAreaUsageCount();

                    deserialization = field.field.createDeserialization();
                    for (int i = 0; i < 18; i++) {
                        assertThat(deserialization.readLong(), is((long) i));
                    }

                    final IFieldDeserialization deserialization4 = deserialization;
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            deserialization4.readLong();
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testStringFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", 10000), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", 10000), new StringFieldSchemaConfiguration("field2", 10000),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", 10000), new StringFieldSchemaConfiguration("field2", 10000),
                        new StringFieldSchemaConfiguration("field3", 10000), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final String str1 = createString(10000, 9876);
        final String str2 = createString(34, 98);
        final String str3 = createString(3456, 987);
        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(0);

                TestNode node2 = space.addNode(234, 1);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(0);
                StringField field22 = node2.node.getField(1);

                TestNode node3 = space.addNode(345, 2);
                ids[2] = node3.node.getId();
                StringField field31 = node3.node.getField(0);
                StringField field32 = node3.node.getField(1);
                StringField field33 = node3.node.getField(2);

                for (int i = 0; i < 10000; i++) {
                    field11.set(str1);
                    field21.set(str2);
                    field22.set(str3);
                    field31.set(str1);
                    field32.set(str2);
                    field33.set(str3);
                }

                for (int i = 0; i < 10000; i++) {
                    assertThat(field33.get(), is(str3));
                    assertThat(field32.get(), is(str2));
                    assertThat(field31.get(), is(str1));
                    assertThat(field22.get(), is(str3));
                    assertThat(field21.get(), is(str2));
                    assertThat(field11.get(), is(str1));
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(null, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                StringField field11 = node1.node.getField(0);
                TestNode node2 = space.findNodeById(ids[1]);
                StringField field21 = node2.node.getField(0);
                StringField field22 = node2.node.getField(1);
                TestNode node3 = space.findNodeById(ids[2]);
                StringField field31 = node3.node.getField(0);
                StringField field32 = node3.node.getField(1);
                StringField field33 = node3.node.getField(2);

                for (int i = 0; i < 10000; i++) {
                    assertThat(field33.get(), is(str3));
                    assertThat(field32.get(), is(str2));
                    assertThat(field31.get(), is(str1));
                    assertThat(field22.get(), is(str3));
                    assertThat(field21.get(), is(str2));
                    assertThat(field11.get(), is(str1));
                }
            }
        });
    }

    @Test
    public void testSerializableFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new SerializableFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new SerializableFieldSchemaConfiguration("field1"), new SerializableFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new SerializableFieldSchemaConfiguration("field1"), new SerializableFieldSchemaConfiguration("field2"),
                        new SerializableFieldSchemaConfiguration("field3"), new IndexedNumericFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final String str1 = createString(34567, 9876);
        final String str2 = createString(34, 98);
        final String str3 = createString(3456, 987);
        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                SerializableField field11 = node1.node.getField(0);

                TestNode node2 = space.addNode(234, 1);
                ids[1] = node2.node.getId();
                SerializableField field21 = node2.node.getField(0);
                SerializableField field22 = node2.node.getField(1);

                TestNode node3 = space.addNode(345, 2);
                ids[2] = node3.node.getId();
                SerializableField field31 = node3.node.getField(0);
                SerializableField field32 = node3.node.getField(1);
                SerializableField field33 = node3.node.getField(2);

                for (int i = 0; i < 10000; i++) {
                    field11.set(str1);
                    field21.set(str2);
                    field22.set(str3);
                    field31.set(str1);
                    field32.set(str2);
                    field33.set(str3);
                }

                for (int i = 0; i < 10000; i++) {
                    assertThat((String) field33.get(), is(str3));
                    assertThat((String) field32.get(), is(str2));
                    assertThat((String) field31.get(), is(str1));
                    assertThat((String) field22.get(), is(str3));
                    assertThat((String) field21.get(), is(str2));
                    assertThat((String) field11.get(), is(str1));
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                SerializableField field11 = node1.node.getField(0);
                TestNode node2 = space.findNodeById(ids[1]);
                SerializableField field21 = node2.node.getField(0);
                SerializableField field22 = node2.node.getField(1);
                TestNode node3 = space.findNodeById(ids[2]);
                SerializableField field31 = node3.node.getField(0);
                SerializableField field32 = node3.node.getField(1);
                SerializableField field33 = node3.node.getField(2);

                for (int i = 0; i < 10000; i++) {
                    assertThat((String) field33.get(), is(str3));
                    assertThat((String) field32.get(), is(str2));
                    assertThat((String) field31.get(), is(str1));
                    assertThat((String) field22.get(), is(str3));
                    assertThat((String) field21.get(), is(str2));
                    assertThat((String) field11.get(), is(str1));
                }
            }
        });
    }

    @Test
    public void testJsonFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new JsonFieldSchemaConfiguration("field1")));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new JsonFieldSchemaConfiguration("field1", "field1", "",
                        IOs.read(Classes.getResource(getClass(), "test.schema"), "UTF-8"),
                        Collections.asSet(new TestJsonValidatorConfiguration("testValidator")),
                        Collections.<JsonConverterSchemaConfiguration>asSet(), "type1", true, false)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final JsonObject jsonObject1 = Json.object().put("key1", "value1").put("key2", "value2").toObject();
        final JsonObject jsonObject2 = Json.object().put("prop1", "value1").put("prop2", 50).toObject();
        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                ids[0] = node1.node.getId();
                JsonField field11 = node1.node.getField(0);
                assertThat(field11.get(), nullValue());

                TestNode node2 = space.addNode(null, 0);
                ids[1] = node2.node.getId();
                JsonField field2 = node2.node.getField(0);
                field2.set(jsonObject1);

                TestNode node3 = space.addNode(null, 1);
                ids[2] = node3.node.getId();
                JsonField field3 = node3.node.getField(0);
                field3.set(jsonObject2);
                assertThat(field3.get(), is((IJsonCollection) jsonObject2));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.findNodeById(ids[0]);
                        JsonField field11 = node1.node.getField(0);
                        assertThat(field11.get(), nullValue());

                        TestNode node2 = space.findNodeById(ids[1]);
                        JsonField field21 = node2.node.getField(0);
                        assertThat(field21.get(), is((IJsonCollection) jsonObject1));

                        TestNode node3 = space.findNodeById(ids[2]);
                        JsonField field31 = node3.node.getField(0);
                        assertThat(field31.get(), is((IJsonCollection) jsonObject2));

                        final TestNode node4 = space.addNode(null, 1);
                        ids[3] = node4.node.getId();
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    final ObjectSpace space0 = space;
                    new Expected(RawDatabaseException.class, new Runnable() {
                        @Override
                        public void run() {
                            space0.findNodeById(ids[3]);
                        }
                    });

                    final ObjectSpace space2 = space;
                    new Expected(JsonValidationException.class, new Runnable() {
                        @Override
                        public void run() {
                            TestNode node2 = space2.findNodeById(ids[2]);
                            JsonField field21 = node2.node.getField(0);
                            field21.set(null);
                            field21.set(jsonObject1);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    final JsonObject jsonObject3 = Json.object().put("prop1", "value1").put("prop2", 1000).toObject();
                    final ObjectSpace space3 = space;
                    new Expected(JsonValidationException.class, new Runnable() {
                        @Override
                        public void run() {
                            TestNode node2 = space3.findNodeById(ids[2]);
                            JsonField field21 = node2.node.getField(0);
                            field21.set(null);
                            field21.set(jsonObject3);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testIndexFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(
                        new IndexedNumericFieldSchemaConfiguration("field", DataType.INT),
                        new IndexFieldSchemaConfiguration("field1", new BTreeIndexSchemaConfiguration("test", 0,
                                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                                false, true, java.util.Collections.<String, String>emptyMap()))));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                IIndexField field1 = node1.node.getField(1);
                IUniqueIndex<String, Long> index = field1.getIndex();
                index.add("test1", 1l);
                index.add("test2", 2l);
                index.add("test3", 3l);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                assertThat(indexManager.getIndexes().size(), is(2));

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeIndex<Integer, TestNode> nodeIndex = space.getIndex(space.getSchema().findNode("node1").findField("field"));

                TestNode node1 = nodeIndex.find(123);
                IIndexField field1 = node1.node.getField(1);
                IUniqueIndex<String, Long> index = field1.getIndex();

                assertThat(index.find("test1"), is(1l));
                assertThat(index.find("test2"), is(2l));
                assertThat(index.find("test3"), is(3l));

                node1.node.delete();
                assertThat(index.isStale(), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                assertThat(indexManager.getIndexes().size(), is(1));
            }
        });
    }

    @Test
    public void testFullTextIndexFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(
                        new IndexedNumericFieldSchemaConfiguration("field", DataType.INT),
                        new IndexFieldSchemaConfiguration("field1", new FullTextIndexSchemaConfiguration("test", 0))));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);

        final IDocumentSchema schema = Documents.doc().numericField("field").stored().type(
                com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.INT).end().toSchema();
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                IIndexField field1 = node1.node.getField(1);
                IFullTextIndex index = field1.getIndex();
                index.add(schema.createDocument(100));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                assertThat(indexManager.getIndexes().size(), is(2));

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeIndex<Integer, TestNode> nodeIndex = space.getIndex(space.getSchema().findNode("node1").findField("field"));

                TestNode node1 = nodeIndex.find(123);
                IIndexField field1 = node1.node.getField(1);
                IFullTextIndex index = field1.getIndex();

                assertThat(index.search(Queries.term("field", "100").toQuery(schema), 1).getTotalCount(), is(1));

                node1.node.delete();
                assertThat(index.isStale(), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                assertThat(indexManager.getIndexes().size(), is(1));
            }
        });
    }

    @Test
    public void testIndexedFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", false, true, 0, 256, null, null, null, 0, IndexType.BTREE,
                        false, true, false, true, true, null, false, false, null, null), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1",
                new HashSet(Arrays.asList(nodeConfiguration1)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeIndex<Integer, TestNode> index1 = space.getIndex(space.getSchema().findNode("node1").findField("field2"));
                INodeIndex<String, TestNode> index2 = space.getIndex(space.getSchema().findNode("node1").findField("field1"));

                TestNode node1 = space.addNode(123, 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(0);
                field11.set("test11");

                TestNode node2 = space.addNode(234, 0);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(0);
                field21.set("test21");

                assertThat(index1.find(123) == node1, is(true));
                assertThat(index1.find(234) == node2, is(true));
                assertThat(index2.find("test11") == node1, is(true));
                assertThat(index2.find("test21") == node2, is(true));

                field11.set("test12");
                field21.set("test22");

                assertThat(index2.find("test12") == node1, is(true));
                assertThat(index2.find("test22") == node2, is(true));
                assertThat(index2.find("test11"), nullValue());
                assertThat(index2.find("test21"), nullValue());

                field11.set(null);
                field21.set(null);
                assertThat(index2.find("test12"), nullValue());
                assertThat(index2.find("test22"), nullValue());

                field11.set("test12");
                field21.set("test22");
                assertThat(index2.find("test12") == node1, is(true));
                assertThat(index2.find("test22") == node2, is(true));

                node2.node.delete();
                assertThat(index2.find("test22"), nullValue());
                assertThat(index1.find(234), nullValue());

                field11.set("test13");
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        try {
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                    INodeIndex<Integer, TestNode> index1 = space.getIndex(space.getSchema().findNode("node1").findField("field2"));
                    INodeIndex<String, TestNode> index2 = space.getIndex(space.getSchema().findNode("node1").findField("field1"));

                    TestNode node1 = space.findNodeById(ids[0]);
                    StringField field11 = node1.node.getField(0);

                    assertThat(index1.find(123) == node1, is(true));
                    assertThat(index2.find("test13") == node1, is(true));
                    assertThat(index2.find("test11"), nullValue());
                    assertThat(index2.find("test12"), nullValue());

                    assertThat(index2.find("test22"), nullValue());
                    assertThat(index1.find(234), nullValue());

                    field11.set("test23");

                    TestNode node3 = space.addNode(456, 0);
                    assertThat(index1.find(456) == node3, is(true));

                    throw new RuntimeException("test");
                }
            });
        } catch (RawDatabaseException e) {
            assertThat(e.getCause() instanceof RuntimeException, is(true));
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeIndex<Integer, TestNode> index1 = space.getIndex(space.getSchema().findNode("node1").findField("field2"));
                INodeIndex<String, TestNode> index2 = space.getIndex(space.getSchema().findNode("node1").findField("field1"));

                TestNode node1 = space.findNodeById(ids[0]);

                assertThat(index1.find(123) == node1, is(true));
                assertThat(index2.find("test13") == node1, is(true));
                assertThat(index1.find(456), nullValue());
            }
        });
    }

    @Test
    public void testFullTextIndexedFields() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, null, true, false, null, null),
                        new IndexedStringFieldSchemaConfiguration("field2", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, new CollatorSchemaConfiguration("ru_RU",
                                com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength.PRIMARY), true, true, null, null),
                        new IndexedNumericFieldSchemaConfiguration("field3", "field3", null, DataType.INT, null, null, null, null, 0, null, false, false,
                                false, false, false, true, null, null)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1",
                new HashSet(Arrays.asList(nodeConfiguration1)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(0);
                field11.set("test11");
                StringField field12 = node1.node.getField(1);
                field12.set("B Hello world!!!");
                IPrimitiveField field13 = node1.node.getField(2);
                field13.setInt(12345);

                TestNode node2 = space.addNode(null, 0);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(0);
                field21.set("test22");
                StringField field22 = node2.node.getField(1);
                field22.set("A Hello world!!!");
                IPrimitiveField field23 = node2.node.getField(2);
                field23.setInt(2222);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IDocumentSchema schema = dataSchema.findNode("node1").getFullTextSchema();

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeFullTextIndex index = space.getFullTextIndex();

                TestNode node1 = space.findNodeById(ids[0]);
                TestNode node2 = space.findNodeById(ids[1]);

                INodeSearchResult result = index.search(Queries.term("field1", "test11").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node1, is(true));

                result = index.search(Queries.expression("field3", "*:*").toQuery(schema), new Sort("field2"), 2);
                assertThat(result.getTotalCount(), is(2));
                assertThat(result.getTopElements().get(0).get() == node2, is(true));
                assertThat(result.getTopElements().get(1).get() == node1, is(true));

                IPrimitiveField field13 = node1.node.getField(2);
                field13.setInt(54321);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IDocumentSchema schema = dataSchema.findNode("node1").getFullTextSchema();

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeFullTextIndex index = space.getFullTextIndex();

                TestNode node1 = space.findNodeById(ids[0]);
                TestNode node2 = space.findNodeById(ids[1]);

                INodeSearchResult result = index.search(Queries.term("field3", "54321").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node1, is(true));

                node1.node.delete();
                node2.node.delete();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(null, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IDocumentSchema schema = dataSchema.findNode("node1").getFullTextSchema();

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeFullTextIndex index = space.getFullTextIndex();

                INodeSearchResult result = index.search(Queries.term("field3", "54321").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(0));
            }
        });
    }

    @Test
    public void testRootNode() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new TestFieldSchemaConfiguration("field1"), new IndexedNumericFieldSchemaConfiguration("field2", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new IndexedNumericFieldSchemaConfiguration("field3", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3"), new PrimitiveFieldSchemaConfiguration("field4", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), "node3");
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1, space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                    ObjectSpace space20 = (ObjectSpace) dataSchema2.getSpace();
                    assertThat(space20.getRootNode(), nullValue());

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.getRootNode();
                    ObjectNode dataNode1 = (ObjectNode) node1.getNode();
                    assertThat(((TestNode) space.findNodeById(dataNode1.getId())).getNode(), is((INode) dataNode1));
                    assertThat(dataNode1.getSpace() == space, is(true));
                    assertThat(dataNode1.getKey(), nullValue());
                    assertThat(dataNode1.getFieldCount(), is(4));
                    assertThat(dataNode1.getSchema().getConfiguration().getName(), is("node3"));
                    TestField field1 = dataNode1.getField(0);
                    assertThat(field1.field.getNode() == dataNode1, is(true));
                    assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                    final TestNode node2 = node1;
                    new Expected(UnsupportedOperationException.class, new Runnable() {
                        @Override
                        public void run() {
                            node2.node.delete();
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test").findSpace("space2");

                    ObjectSpace space20 = (ObjectSpace) dataSchema2.getSpace();
                    assertThat(space20.getRootNode(), nullValue());

                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node1 = space.getRootNode();
                    ObjectNode dataNode1 = (ObjectNode) node1.getNode();
                    assertThat(((TestNode) space.findNodeById(dataNode1.getId())).getNode(), is((INode) dataNode1));
                    assertThat(dataNode1.getSpace() == space, is(true));
                    assertThat(dataNode1.getKey(), nullValue());
                    assertThat(dataNode1.getFieldCount(), is(4));
                    assertThat(dataNode1.getSchema().getConfiguration().getName(), is("node3"));
                    TestField field1 = dataNode1.getField(0);
                    assertThat(field1.field.getNode() == dataNode1, is(true));
                    assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                    final TestNode node3 = node1;
                    new Expected(UnsupportedOperationException.class, new Runnable() {
                        @Override
                        public void run() {
                            node3.node.delete();
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testCompaction() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new StringFieldSchemaConfiguration("field2", 256),
                new JsonFieldSchemaConfiguration("field3"),
                new SingleReferenceFieldSchemaConfiguration("field4", null),
                new SerializableFieldSchemaConfiguration("field5")));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), "node2", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final TestNode[] nodes = new TestNode[COUNT];
        final Set<TestNode> nodesSet = new HashSet<TestNode>();
        final String s = createString(1000, 123);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);

                for (int i = 0; i < COUNT; i++) {
                    TestNode node1 = space.addNode(i, 0);
                    nodes[i] = node1;
                    nodesSet.add(node1);
                    IStringField field2 = node1.node.getField(1);
                    field2.set(s);
                    IJsonField field3 = node1.node.getField(2);
                    field3.set(Json.object().put("key", "value").toObject());
                    ISingleReferenceField field4 = node1.node.getField(3);
                    field4.set(node);
                    ISerializableField field5 = node1.node.getField(4);
                    field5.set(s);
                    field.add(node1);
                }
            }
        });

        final IRawDataFile[] file = new IRawDataFile[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    for (int i = 0; i < COUNT; i++) {
                        TestNode node1 = space.findNodeById(nodes[i].node.getId());
                        if ((i % 2) == 0) {
                            node1.node.delete();
                            nodesSet.remove(node1);
                        } else {
                            nodes[i] = node1;
                            IStringField field2 = node1.node.getField(1);
                            field2.set("Changed" + i);

                            ISerializableField field5 = node1.node.getField(4);
                            field5.set("Changed" + i);
                        }
                    }

                    file[0] = ((IRawPage) Tests.get(space, "headerPage")).getFile();
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        assertThat(file[0].isDeleted(), is(false));
        assertThat(new File(file[0].getPath()).exists(), is(true));

        ((IObjectOperationManager) database.findExtension(IObjectOperationManager.NAME)).compact(null);
        assertThat(file[0].isDeleted(), is(true));
        assertThat(new File(file[0].getPath()).exists(), is(false));
        Thread.sleep(100);

        final Set<TestNode> nodeSet2 = new HashSet<TestNode>();
        final Set<TestNode> nodeSet3 = new HashSet<TestNode>();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);
                for (TestNode n : space.<TestNode>getNodes()) {
                    if (n == node)
                        continue;

                    nodeSet2.add(n);

                    IPrimitiveField field1 = n.node.getField(0);
                    IStringField field2 = n.node.getField(1);
                    assertThat(field2.get(), is("Changed" + field1.getInt()));
                    IJsonField field3 = n.node.getField(2);
                    assertThat((JsonObject) field3.get(), is(Json.object().put("key", "value").toObject()));
                    ISingleReferenceField field4 = n.node.getField(3);
                    assertThat(field4.get() == node, is(true));
                    ISerializableField field5 = n.node.getField(4);
                    assertThat((String) field5.get(), is("Changed" + field1.getInt()));
                }

                assertThat(nodeSet2.size(), is(nodesSet.size()));

                for (TestNode n : field)
                    nodeSet3.add(n);

                assertThat(nodeSet3, is(nodeSet2));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);
                Set<TestNode> nodeSet4 = new HashSet<TestNode>();
                for (TestNode n : space.<TestNode>getNodes()) {
                    if (n == node)
                        continue;

                    nodeSet4.add(n);

                    IPrimitiveField field1 = n.node.getField(0);
                    IStringField field2 = n.node.getField(1);
                    assertThat(field2.get(), is("Changed" + field1.getInt()));
                    IJsonField field3 = n.node.getField(2);
                    assertThat((JsonObject) field3.get(), is(Json.object().put("key", "value").toObject()));
                    ISingleReferenceField field4 = n.node.getField(3);
                    assertThat(field4.get() == node, is(true));
                    ISerializableField field5 = n.node.getField(4);
                    assertThat((String) field5.get(), is("Changed" + field1.getInt()));
                }

                assertThat(nodeSet4.size(), is(nodesSet.size()));

                Set<TestNode> nodeSet5 = new HashSet<TestNode>();
                for (TestNode n : field)
                    nodeSet5.add(n);

                assertThat(nodeSet4, is(nodeSet2));
                assertThat(nodeSet5, is(nodeSet2));
            }
        });
    }

    @Test
    public void testMigration() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new StringFieldSchemaConfiguration("field2", 256),
                new JsonFieldSchemaConfiguration("field3"),
                new SingleReferenceFieldSchemaConfiguration("field4", null),
                new SerializableFieldSchemaConfiguration("field5")));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)), "node2", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final TestNode[] nodes = new TestNode[COUNT];
        final Set<TestNode> nodesSet = new HashSet<TestNode>();
        final String s = createString(1000, 123);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);

                space.addNode(null, 2);

                for (int i = 0; i < COUNT; i++) {
                    TestNode node1 = space.addNode(i, 0);
                    nodes[i] = node1;
                    nodesSet.add(node1);
                    IStringField field2 = node1.node.getField(1);
                    field2.set(s);
                    IJsonField field3 = node1.node.getField(2);
                    field3.set(Json.object().put("key", i).toObject());
                    ISingleReferenceField field4 = node1.node.getField(3);
                    field4.set(node);
                    ISerializableField field5 = node1.node.getField(4);
                    field5.set(s);
                    field.add(node1);
                }
            }
        });

        final IRawDataFile[] file = new IRawDataFile[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    for (int i = 0; i < COUNT; i++) {
                        TestNode node1 = space.findNodeById(nodes[i].node.getId());
                        if ((i % 2) == 0) {
                            node1.node.delete();
                            nodesSet.remove(node1);
                        } else {
                            nodes[i] = node1;
                            IStringField field2 = node1.node.getField(1);
                            field2.set("Changed" + i);
                            ISerializableField field5 = node1.node.getField(4);
                            field5.set("Changed" + i);
                        }
                    }

                    file[0] = ((IRawPage) Tests.get(space, "headerPage")).getFile();
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.LONG),
                new PrimitiveFieldSchemaConfiguration("field3", DataType.LONG),
                new SingleReferenceFieldSchemaConfiguration("field4", null)));
        space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), "node2", 0, 0);

        assertThat(file[0].isDeleted(), is(false));
        assertThat(new File(file[0].getPath()).exists(), is(true));

        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1), configuration2),
                        java.util.Collections.singletonMap("test.space1", new TestDataMigrator()));
            }
        });

        assertThat(file[0].isDeleted(), is(true));
        assertThat(new File(file[0].getPath()).exists(), is(false));

        final Set<TestNode> nodeSet2 = new HashSet<TestNode>();
        final Set<TestNode> nodeSet3 = new HashSet<TestNode>();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema2 = transaction.getSchemas().get(
                        transaction.getSchemas().size() - 2).findDomain("test").findSpace("space1");
                assertThat(dataSchema2.getSpace(), nullValue());
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);
                for (TestNode n : space.<TestNode>getNodes()) {
                    if (n == node)
                        continue;

                    nodeSet2.add(n);

                    IPrimitiveField field1 = n.node.getField(0);
                    IPrimitiveField field3 = n.node.getField(1);
                    assertThat(field3.get(), is((Object)field1.get()));
                    ISingleReferenceField field4 = n.node.getField(2);
                    assertThat(field4.get() == node, is(true));
                }

                assertThat(nodeSet2.size(), is(nodesSet.size()));

                for (TestNode n : field)
                    nodeSet3.add(n);

                assertThat(nodeSet3, is(nodeSet2));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema2 = transaction.getSchemas().get(
                        transaction.getSchemas().size() - 2).findDomain("test").findSpace("space1");
                assertThat(dataSchema2.getSpace(), nullValue());

                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);
                Set<TestNode> nodeSet4 = new HashSet<TestNode>();
                for (TestNode n : space.<TestNode>getNodes()) {
                    if (n == node)
                        continue;

                    nodeSet4.add(n);

                    IPrimitiveField field1 = n.node.getField(0);
                    IPrimitiveField field3 = n.node.getField(1);
                    assertThat(field3.get(), is((Object)field1.get()));
                    ISingleReferenceField field4 = n.node.getField(2);
                    assertThat(field4.get() == node, is(true));
                }

                assertThat(nodeSet4.size(), is(nodesSet.size()));

                Set<TestNode> nodeSet5 = new HashSet<TestNode>();
                for (TestNode n : field)
                    nodeSet5.add(n);

                assertThat(nodeSet4, is(nodeSet2));
                assertThat(nodeSet5, is(nodeSet2));
            }
        });
    }

    @Test
    public void testNonStructuredSchemaChange() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                new StringFieldSchemaConfiguration("field2", 256),
                new JsonFieldSchemaConfiguration("field3"),
                new SingleReferenceFieldSchemaConfiguration("field4", null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)), "node2", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final int[] fileIndex = new int[1];
        final long[] ids = new long[COUNT];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node = space.getRootNode();
                IReferenceField<TestNode> field = node.node.getField(0);

                space.addNode(null, 2);

                TestNode[] nodes = new TestNode[COUNT];
                Set<TestNode> nodesSet = new HashSet<TestNode>();
                for (int i = 0; i < COUNT; i++) {
                    TestNode node1 = space.addNode(i, 0);
                    ids[i] = node1.node.getId();
                    nodes[i] = node1;
                    nodesSet.add(node1);
                    IStringField field2 = node1.node.getField(1);
                    field2.set("Hello world!!!");
                    IJsonField field3 = node1.node.getField(2);
                    field3.set(Json.object().put("key", i).toObject());
                    ISingleReferenceField field4 = node1.node.getField(3);
                    field4.set(node);
                    field.add(node1);
                }

                fileIndex[0] = space.getFileIndex();
            }
        });

        TestNodeSchemaConfiguration nodeConfiguration4 = new TestNodeSchemaConfiguration("anode4", "node4Alias", "node4Desciption", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", "field1Alias", "field1Description", DataType.INT, null, null, null, null,
                        0, IndexType.BTREE, true, true, false, true, true, false, null, null)));
        nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", "node1Alias", "node1Desciption", Arrays.asList(
                new IndexedNumericFieldSchemaConfiguration("field1", "field1Alias", "field1Description", DataType.INT, null, null, null, null,
                        0, IndexType.BTREE, true, true, false, true, true, false, null, null),
                new StringFieldSchemaConfiguration("field2", 256),
                new JsonFieldSchemaConfiguration("field3"),
                new SingleReferenceFieldSchemaConfiguration("field4", null)));
        nodeConfiguration2 = new TestNodeSchemaConfiguration("node2", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        nodeConfiguration3 = new TestNodeSchemaConfiguration("node3", Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)));
        space1 = new ObjectSpaceSchemaConfiguration("space1", "space1Alias", "space1Description",
                new HashSet(Arrays.asList(nodeConfiguration4, nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)), "node2", 0, 0);

        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1), configuration2), null);
            }

            @Override
            public int getSize() {
                return 1;
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema2 = transaction.getSchemas().get(transaction.getSchemas().size() - 2).findDomain(
                        "test").findSpace("space1");
                assertThat(dataSchema2.getSpace(), nullValue());
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                assertThat(transaction.getCurrentSchema().findDomain("test").findSpaceByAlias("space1Alias") == dataSchema, is(true));
                NodeSchema nodeSchema = dataSchema.findNode("node1");
                assertThat(dataSchema.findNodeByAlias("node1Alias") == nodeSchema, is(true));
                assertThat(nodeSchema.getIndex(), is(0));
                assertThat(dataSchema.findNode("anode4").getIndex(), is(3));

                IFieldSchema fieldSchema = nodeSchema.findField("field1");
                assertThat(nodeSchema.findFieldByAlias("field1Alias") == fieldSchema, is(true));

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                assertThat(space.getFileIndex(), is(fileIndex[0]));

                for (int i = 0; i < COUNT; i++) {
                    TestNode node1 = space.findNodeById(ids[i]);
                    assertThat(node1.node.getSchema() == nodeSchema, is(true));
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

    private String createString(int size, int base) {
        char[] b = new char[size];
        for (int i = 0; i < size; i++)
            b[i] = (char) (i + base);

        return new String(b);
    }

    public static class TestNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
        public TestNodeSchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields) {
            super(name, fields);
        }

        public TestNodeSchemaConfiguration(String name, String alias, String desciption, List<? extends FieldSchemaConfiguration> fields) {
            super(name, alias, desciption, fields, null);
        }

        @Override
        public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            return new TestNodeSchema(this, index, fields, documentSchema);
        }

        @Override
        public INodeObject createNode(INode node) {
            return new TestNode(node);
        }
    }

    public static class TestNodeSchema extends ObjectNodeSchema {
        public boolean invalid;

        public TestNodeSchema(ObjectNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            super(configuration, index, fields, documentSchema);
        }

        @Override
        public void validate(INode node) {
            super.validate(node);

            if (invalid)
                throw new RuntimeException("test");
        }
    }

    public static class TestNode implements INodeObject {
        public final ObjectNode node;
        public boolean flushed;
        private Object primaryKey;
        private boolean created;
        private boolean opened;
        private boolean deleted;
        private boolean unloaded;

        public TestNode(INode node) {
            this.node = (ObjectNode) node;
        }

        @Override
        public INode getNode() {
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestNode))
                return false;

            TestNode n = (TestNode) o;
            return node.equals(n.node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public void onBeforeFlush() {
            flushed = true;
        }

        @Override
        public void onAfterFlush() {
        }

        @Override
        public String toString() {
            return node.toString();
        }

        @Override
        public void onCreated(Object primaryKey, Object[] args) {
            this.primaryKey = primaryKey;
            this.created = true;
        }

        @Override
        public void onOpened() {
            this.opened = true;
        }

        @Override
        public void onDeleted() {
            this.deleted = true;
        }

        @Override
        public void onUnloaded() {
            this.unloaded = true;
        }

        public void reset() {
            created = false;
            deleted = false;
            opened = false;
            unloaded = false;
            primaryKey = null;
        }

        @Override
        public void onBeforeCreated(Object primaryKey, Object[] args, Object[] fieldInitializers) {
            if (args != null && args.length == 4) {
                IFileFieldInitializer fileInitializer = (IFileFieldInitializer) fieldInitializers[0];
                fileInitializer.setPathIndex((Integer) args[0]);
                fileInitializer.setMaxFileSize((Integer) args[2]);
                fileInitializer.setDirectory((String) args[3]);
            }

            if (args != null && args.length == 1) {
                IBlobFieldInitializer blobInitializer1 = (IBlobFieldInitializer) fieldInitializers[1];
                blobInitializer1.setStore(args[0]);
                if (fieldInitializers.length > 2) {
                    IBlobFieldInitializer blobInitializer2 = (IBlobFieldInitializer) fieldInitializers[2];
                    blobInitializer2.setStore(args[0]);
                }
            }
        }

        @Override
        public void validate() {
        }

        @Override
        public void dump(IJsonHandler json, IDumpContext context) {
        }


        @Override
        public boolean allowModify() {
            return true;
        }

        @Override
        public boolean allowDeletion() {
            return true;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public void onMigrated() {
        }

        @Override
        public void onBeforeMigrated(Object primaryKey) {
        }
    }

    public static class TestFieldSchemaConfiguration extends StringFieldSchemaConfiguration {
        public TestFieldSchemaConfiguration(String name) {
            this(name, 256);
        }

        public TestFieldSchemaConfiguration(String name, int maxSize) {
            super(name, maxSize);
        }

        @Override
        public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
            return new TestFieldSchema(this, index, offset, indexTotalIndex);
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
        public Object createInitializer() {
            return null;
        }
    }

    public static class IndexedTestFieldSchemaConfiguration extends IndexedStringFieldSchemaConfiguration {
        public IndexedTestFieldSchemaConfiguration(String name) {
            this(name, 256);
        }

        public IndexedTestFieldSchemaConfiguration(String name, int maxSize) {
            super(name, true, maxSize);
        }

        @Override
        public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
            return new TestFieldSchema(this, index, offset, indexTotalIndex);
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
        public Object createInitializer() {
            return null;
        }
    }

    public static class TestFieldSchema extends StringFieldSchema {
        private final int indexTotalIndex;

        public TestFieldSchema(StringFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
            super(configuration, index, offset, indexTotalIndex);

            this.indexTotalIndex = indexTotalIndex;
        }

        @Override
        public IFieldObject createField(IField field) {
            return new TestField((IComplexField) field);
        }

        @Override
        public int getIndexTotalIndex() {
            return indexTotalIndex;
        }

        @Override
        public void validate(IField field) {
        }
    }

    public static class TestField extends StringField {
        private final IComplexField field;
        private String primaryKey;
        private boolean created;
        private boolean opened;
        private boolean deleted;

        public TestField(IComplexField field) {
            super(field);

            this.field = field;
        }

        @Override
        public void flush() {
            super.flush();
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            super.onCreated(primaryKey, initializer);
            this.primaryKey = (String) primaryKey;
            this.created = true;
        }

        @Override
        public void onOpened() {
            super.onOpened();
            this.opened = true;
        }

        @Override
        public void onDeleted() {
            super.onDeleted();
            this.deleted = true;
        }

        public void reset() {
            created = false;
            deleted = false;
            opened = false;
            primaryKey = null;
        }
    }

    private static class TestJsonValidatorConfiguration extends JsonValidatorSchemaConfiguration {
        public TestJsonValidatorConfiguration(String name) {
            super(name);
        }

        @Override
        public IJsonValidator createValidator() {
            return new TestJsonValidator();
        }
    }

    private static class TestJsonValidator implements IJsonValidator {
        @Override
        public boolean supports(Class clazz) {
            return clazz == Long.class;
        }

        @Override
        public void validate(JsonType type, Object instance, IJsonValidationContext context) {
            IJsonDiagnostics diagnostics = context.getDiagnostics();

            Long l = (Long) instance;
            if (l < 10 || l > 100)
                diagnostics.addError(new NonLocalizedMessage("Property is not in range [10..100]."));
        }
    }

    private static class TestDataMigrator implements IObjectMigrator {
        @Override
        public boolean supports(IFieldSchema oldSchema, IFieldSchema newSchema) {
            if (oldSchema.getParent().getParent().getConfiguration().getName().equals("space1") &&
                    oldSchema.getParent().getConfiguration().getName().equals("node1") &&
                    oldSchema.getConfiguration().getName().equals("field3"))
                return true;
            else
                return false;
        }

        @Override
        public boolean isCompatible(IFieldSchema oldSchema, IFieldSchema newSchema) {
            return oldSchema.getConfiguration() instanceof JsonFieldSchemaConfiguration &&
                    newSchema.getConfiguration() instanceof PrimitiveFieldSchemaConfiguration &&
                    ((PrimitiveFieldSchemaConfiguration) newSchema.getConfiguration()).getDataType() == DataType.LONG;
        }

        @Override
        public IFieldConverter createConverter(IFieldSchema oldSchema, IFieldSchema newSchema) {
            return new TestFieldConverter();
        }
    }

    private static class TestFieldConverter implements IFieldConverter {
        @Override
        public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
            IJsonField oldField = oldFieldInstance.getObject();
            IPrimitiveField newField = newFieldInstance.getObject();

            JsonObject object = oldField.get();
            newField.setLong((Long) object.get("key"));
        }
    }
}
