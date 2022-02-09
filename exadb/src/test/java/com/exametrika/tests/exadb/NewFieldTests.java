/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

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
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeNonUniqueSortedIndex;
import com.exametrika.api.exadb.objectdb.config.schema.BodyFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ComputedFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedUuidFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.TagFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.UuidFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.VersionFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IComputedField;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.INumericSequenceField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ITagField;
import com.exametrika.api.exadb.objectdb.fields.IUuidField;
import com.exametrika.api.exadb.objectdb.fields.IVersionField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.fields.StringField;
import com.exametrika.impl.exadb.objectdb.fields.StringSequenceField;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.INodeBody;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


/**
 * The {@link NewFieldTests} are tests for new fields.
 *
 * @author Medvedev-A
 */
public class NewFieldTests {
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
        builder.setTimerPeriod(10);
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
    public void testStringFieldConstraints() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", "field1", null, true, true, 10, 10000, null, null, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", "field1", null, true, true, 0, 10000, "h*llo", null, null)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", "field1", null, true, true, 0, 10000, null, Collections.asSet("first", "second", "third"), null)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 0);
                        StringField field = node1.node.getField(0);
                        field.set("a");
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                StringField field = node1.node.getField(0);
                field.set("aoooooooooooooooooooooooooooooo");
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof RawRollbackException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 1);
                        StringField field = node1.node.getField(0);
                        field.set("abc");
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 1);
                StringField field = node1.node.getField(0);
                field.set("heeeeeeello");
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof RawRollbackException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 2);
                        StringField field = node1.node.getField(0);
                        field.set("fourth");
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 2);
                StringField field = node1.node.getField(0);
                field.set("third");
            }
        });
    }

    @Test
    public void testStringSequenceField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", "field1", null, true, true, 0, 256, null, null, "seq2"),
                        new IndexedStringFieldSchemaConfiguration("field2", "field2", null, true, true, 0, 256, null, null, "seq1",
                                0, IndexType.BTREE, true, true, false, true, true, null, false, false, null, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new StringSequenceFieldSchemaConfiguration("seq1", "seq1", null, "prefix-", "-suffix", null,
                                1, 1, null),
                        new StringSequenceFieldSchemaConfiguration("seq2", "seq2", null, "p-", "-s", "0000",
                                100, 10, new StandardSchedulePeriodSchemaConfiguration(UnitType.MILLISECOND, Kind.RELATIVE, 200))));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), "node2");
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                StringField field11 = node1.node.getField(1);
                assertThat(field11.get(), is("prefix-1-suffix"));
                StringField field12 = node1.node.getField(0);
                assertThat(field12.get(), is("p-0100-s"));
                assertThat(space.getIndex(field11.getSchema()).find("prefix-1-suffix") == node1, is(true));

                TestNode node2 = space.addNode(null, 0);
                StringField field21 = node2.node.getField(1);
                assertThat(field21.get(), is("prefix-2-suffix"));
                StringField field22 = node2.node.getField(0);
                assertThat(field22.get(), is("p-0110-s"));
                assertThat(space.getIndex(field21.getSchema()).find("prefix-2-suffix") == node2, is(true));

                TestNode root = space.getRootNode();
                StringSequenceField rootField1 = root.node.getField(0);
                rootField1.setLong(100);

                TestNode node3 = space.addNode(null, 0);
                StringField field31 = node3.node.getField(1);
                assertThat(field31.get(), is("prefix-100-suffix"));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                StringField field11 = node1.node.getField(1);
                assertThat(field11.get(), is("prefix-101-suffix"));
                StringField field12 = node1.node.getField(0);
                assertThat(field12.get(), is("p-0130-s"));
            }
        });

        Thread.sleep(300);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                StringField field11 = node1.node.getField(0);
                assertThat(field11.get(), is("p-0100-s"));
            }
        });
    }

    @Test
    public void testNumericFieldConstraints() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", "field1", null, DataType.INT, 10, 100, null, null)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", "field1", null, DataType.INT, null, null, Collections.asSet(100, 200, 300), null)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration3)), null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof RawRollbackException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 0);
                        INumericField field = node1.node.getField(0);
                        field.set(1000);
                    }
                });
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof RawRollbackException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 0);
                        INumericField field = node1.node.getField(0);
                        field.set(0);
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                INumericField field = node1.node.getField(0);
                field.set(50);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof RawRollbackException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 1);
                        INumericField field = node1.node.getField(0);
                        field.set(123);
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 1);
                INumericField field = node1.node.getField(0);
                field.set(200);
            }
        });
    }

    @Test
    public void testNumericSequenceField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", "field1", null, DataType.INT, null, null, null, "seq2"),
                        new IndexedNumericFieldSchemaConfiguration("field2", "field2", null, DataType.INT, null, null, null, "seq1",
                                0, IndexType.BTREE, true, true, false, true, true, false, null, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new NumericSequenceFieldSchemaConfiguration("seq1", "seq1", null, 1, 1, null),
                        new NumericSequenceFieldSchemaConfiguration("seq2", "seq2", null,
                                100, 10, new StandardSchedulePeriodSchemaConfiguration(UnitType.MILLISECOND, Kind.RELATIVE, 200))));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), "node2");
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                INumericField field11 = node1.node.getField(1);
                assertThat(field11.getInt(), is(1));
                INumericField field12 = node1.node.getField(0);
                assertThat(field12.getInt(), is(100));
                assertThat(space.getIndex(field11.getSchema()).find(1) == node1, is(true));

                TestNode node2 = space.addNode(null, 0);
                INumericField field21 = node2.node.getField(1);
                assertThat(field21.getInt(), is(2));
                INumericField field22 = node2.node.getField(0);
                assertThat(field22.getInt(), is(110));
                assertThat(space.getIndex(field21.getSchema()).find(2) == node2, is(true));

                TestNode root = space.getRootNode();
                INumericSequenceField rootField1 = root.node.getField(0);
                rootField1.setLong(100);

                TestNode node3 = space.addNode(null, 0);
                INumericField field31 = node3.node.getField(1);
                assertThat(field31.getInt(), is(100));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                INumericField field11 = node1.node.getField(1);
                assertThat(field11.getInt(), is(101));
                INumericField field12 = node1.node.getField(0);
                assertThat(field12.getInt(), is(130));
            }
        });

        Thread.sleep(300);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                INumericField field11 = node1.node.getField(0);
                assertThat(field11.getInt(), is(100));
            }
        });
    }

    @Test
    public void testSerializableFieldConstraints() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new SerializableFieldSchemaConfiguration("field1", "field1", null, true, true,
                        Collections.asSet(BigDecimal.class.getName(), BigInteger.class.getName()))));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)), null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node1 = space.addNode(null, 0);
                        ISerializableField field = node1.node.getField(0);
                        field.set("Test");
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                ISerializableField field = node1.node.getField(0);
                field.set(new BigDecimal(123d));
                field.set(new BigDecimal(123).toBigInteger());
            }
        });
    }

    @Test
    public void testUuidField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedUuidFieldSchemaConfiguration("field1", "field1", null, true, 0, IndexType.BTREE, true, true, true, null),
                        new UuidFieldSchemaConfiguration("field2", true),
                        new IndexedUuidFieldSchemaConfiguration("field3", "field3", null, false, 0, IndexType.BTREE, false, true, true, null)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)), null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();
        final UUID id3 = UUID.randomUUID();
        final UUID id4 = UUID.randomUUID();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(id1, 0);
                IUuidField field1 = node1.node.getField(0);
                assertThat(field1.get(), is(id1));
                IUuidField field2 = node1.node.getField(1);
                field2.set(id2);

                assertThat(space.getIndex(field1.getSchema()).find(id1) == node1, is(true));

                IUuidField field3 = node1.node.getField(2);
                field3.set(id3);
                assertThat(space.getIndex(field3.getSchema()).find(id3) == node1, is(true));

                field3.set(id4);
                assertThat(space.getIndex(field3.getSchema()).find(id3), nullValue());
                assertThat(space.getIndex(field3.getSchema()).find(id4) == node1, is(true));

                field3.set(null);
                assertThat(space.getIndex(field3.getSchema()).find(id4), nullValue());

                field3.set(id4);
                assertThat(space.getIndex(field3.getSchema()).find(id4) == node1, is(true));
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

                TestNode node1 = (TestNode) space.getIndex(dataSchema.findNode("node1").findField("field1")).find(id1);
                IUuidField field1 = node1.node.getField(0);
                assertThat(field1.get(), is(id1));
                IUuidField field2 = node1.node.getField(1);
                assertThat(field2.get(), is(id2));

                IUuidField field3 = node1.node.getField(2);
                assertThat(space.getIndex(field3.getSchema()).find(id4) == node1, is(true));

                node1.node.delete();

                assertThat(space.getIndex(field3.getSchema()).find(id4), nullValue());
            }
        });
    }

    @Test
    public void testTagField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TagFieldSchemaConfiguration("field2")));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TagFieldSchemaConfiguration("field2")));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null);
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(1, 0);
                ITagField field1 = node1.node.getField(1);
                field1.set(Arrays.asList("a.b", "a", "c.d"));

                TestNode node2 = space.addNode(2, 1);
                ITagField field2 = node2.node.getField(1);
                field2.set(Arrays.asList("c", "c.d"));

                space.addNode(3, 1);
                node2.node.getField(1);

                assertThat(space.getIndex(field1.getSchema()) == space.getIndex(field2.getSchema()), is(true));
                INodeNonUniqueSortedIndex<String, TestNode> index = space.getIndex(field1.getSchema());
                assertThat(Collections.toList(index.findValues("a").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("a.b").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("c").iterator()), is(Arrays.asList(node1, node2)));
                assertThat(Collections.toList(index.findValues("c.d").iterator()), is(Arrays.asList(node1, node2)));
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

                TestNode node1 = space.addNode(1, 0);
                ITagField field1 = node1.node.getField(1);
                TestNode node2 = space.addNode(2, 1);
                ITagField field2 = node2.node.getField(1);
                TestNode node3 = space.addNode(3, 1);
                ITagField field3 = node3.node.getField(1);

                INodeNonUniqueSortedIndex<String, TestNode> index = space.getIndex(field1.getSchema());

                assertThat(Collections.toList(index.findValues("a").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("a.b").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("c").iterator()), is(Arrays.asList(node1, node2)));
                assertThat(Collections.toList(index.findValues("c.d").iterator()), is(Arrays.asList(node1, node2)));

                field2.set(null);
                field3.set(Arrays.asList("c", "c.d"));

                assertThat(Collections.toList(index.findValues("a").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("a.b").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("c").iterator()), is(Arrays.asList(node1, node3)));
                assertThat(Collections.toList(index.findValues("c.d").iterator()), is(Arrays.asList(node1, node3)));

                node3.node.delete();

                assertThat(Collections.toList(index.findValues("a").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("a.b").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("c").iterator()), is(Arrays.asList(node1)));
                assertThat(Collections.toList(index.findValues("c.d").iterator()), is(Arrays.asList(node1)));
            }
        });
    }

    @Test
    public void testComputedField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new ComputedFieldSchemaConfiguration("field1", "10 + 10"),
                        new ComputedFieldSchemaConfiguration("field2", "$.a + $.b")));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)), null);
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                IComputedField field1 = node1.node.getField(0);
                assertThat((Long) field1.get(), is(20l));

                IComputedField field2 = node1.node.getField(1);
                assertThat((Long) field2.execute(new MapBuilder().put("a", 10).put("b", 100).toMap()), is(110l));
            }
        });
    }

    @Test
    public void testVersionField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", DataType.INT),
                        new VersionFieldSchemaConfiguration("field2")));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)),
                null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long id[] = new long[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                id[0] = node1.node.getId();
                INumericField field1 = node1.node.getField(0);

                IVersionField field2 = node1.node.getField(1);
                assertThat(field2.getLong(), is(1l));

                field1.setInt(123);
                assertThat(field2.getLong(), is(1l));

                field1.setInt(345);
                assertThat(field2.getLong(), is(1l));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(id[0]);
                INumericField field1 = node1.node.getField(0);

                IVersionField field2 = node1.node.getField(1);
                assertThat(field2.getLong(), is(1l));

                field1.setInt(456);
                assertThat(field2.getLong(), is(2l));

                field1.setInt(678);
                assertThat(field2.getLong(), is(2l));
            }
        });
    }

    @Test
    public void testBodyField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestBodySchemaConfiguration("node1",
                Arrays.asList(new BodyFieldSchemaConfiguration("field1")));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1)),
                null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long ids[] = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestBody node1 = space.addNode(null, 0);
                ids[0] = node1.getNode().getId();
                node1.set(123, "Hello123");

                TestBody node2 = space.addNode(null, 0);
                ids[1] = node2.getNode().getId();
                node2.set(234, "Hello234");
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestBody node1 = space.findNodeById(ids[0]);
                assertThat(node1.field1, is(123l));
                assertThat(node1.field2, is("Hello123"));
                node1.set(345, "Hello345");

                TestBody node2 = space.findNodeById(ids[1]);
                assertThat(node2.field1, is(234l));
                assertThat(node2.field2, is("Hello234"));
                node2.set(456, "Hello456");
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

                TestBody node1 = space.findNodeById(ids[0]);
                assertThat(node1.getNode().getId(), is(ids[0]));
                assertThat(node1.field1, is(345l));
                assertThat(node1.field2, is("Hello345"));

                TestBody node2 = space.findNodeById(ids[1]);
                assertThat(node2.getNode().getId(), is(ids[1]));
                assertThat(node2.field1, is(456l));
                assertThat(node2.field2, is("Hello456"));
            }
        });
    }

    @Test
    public void testReferenceFieldConstraints() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("field1", "field1", null, null, Collections.asSet("node3", "node4"),
                        true, false, false, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("field1", "field1", null, null, Collections.asSet("node3", "node4"),
                        true, false, false, null, false)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration4 = new TestNodeSchemaConfiguration("node4",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration5 = new TestNodeSchemaConfiguration("node5",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", DataType.INT)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3, nodeConfiguration4, nodeConfiguration5)),
                null);
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node5 = space.addNode(null, 4);
                        TestNode node1 = space.addNode(null, 0);
                        ISingleReferenceField field = node1.node.getField(0);
                        field.set(node5);
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node3 = space.addNode(null, 2);
                TestNode node4 = space.addNode(null, 3);
                TestNode node1 = space.addNode(null, 0);
                ISingleReferenceField field = node1.node.getField(0);
                field.set(node3);
                field.set(node4);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                        ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                        TestNode node5 = space.addNode(null, 4);
                        TestNode node1 = space.addNode(null, 1);
                        IReferenceField field = node1.node.getField(0);
                        field.add(node5);
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node3 = space.addNode(null, 2);
                TestNode node4 = space.addNode(null, 3);
                TestNode node1 = space.addNode(null, 1);
                IReferenceField field = node1.node.getField(0);
                field.add(node3);
                field.add(node4);
            }
        });
    }

    @Test
    public void testOwningReferenceField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("field1", "field1", null, null, Collections.asSet("node3"),
                        true, true, false, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("field1", "field1", null, null, Collections.asSet("node4"),
                        true, true, false, null, false)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", DataType.INT)));
        NodeSchemaConfiguration nodeConfiguration4 = new TestNodeSchemaConfiguration("node4",
                Arrays.asList(new NumericFieldSchemaConfiguration("field1", DataType.INT)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3, nodeConfiguration4)),
                null);
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node3 = space.addNode(null, 2);
                TestNode node1 = space.addNode(null, 0);
                ISingleReferenceField field = node1.node.getField(0);
                field.set(node3);

                node1.node.delete();
                assertThat(node3.node.isDeleted(), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node41 = space.addNode(null, 3);
                TestNode node42 = space.addNode(null, 3);
                TestNode node2 = space.addNode(null, 1);
                IReferenceField field = node2.node.getField(0);
                field.add(node41);
                field.add(node42);

                node2.node.delete();
                assertThat(node41.node.isDeleted(), is(true));
                assertThat(node42.node.isDeleted(), is(true));
            }
        });
    }

    @Test
    public void testBidirectionalReferenceField() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("field1", "field1", null, "node3.field1", null,
                                true, true, true, null),
                        new SingleReferenceFieldSchemaConfiguration("field2", "field2", null, "node3.field2", null,
                                true, true, true, null)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("field1", "field1", null, "node4.field1", null,
                                true, true, true, null, false),
                        new ReferenceFieldSchemaConfiguration("field2", "field2", null, "node4.field2", null,
                                true, true, true, null, false)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("field1", "field1", null, "node1.field1", null,
                                true, true, true, null),
                        new ReferenceFieldSchemaConfiguration("field2", "field2", null, "node1.field2", null,
                                true, true, true, null, false)));
        NodeSchemaConfiguration nodeConfiguration4 = new TestNodeSchemaConfiguration("node4",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("field1", "field1", null, "node2.field1", null,
                                true, true, true, null),
                        new ReferenceFieldSchemaConfiguration("field2", "field2", null, "node2.field2", null,
                                true, true, true, null, false)));

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3, nodeConfiguration4)),
                null);
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(null, 0);
                ISingleReferenceField field11 = node1.node.getField(0);
                ISingleReferenceField field12 = node1.node.getField(1);

                TestNode node2 = space.addNode(null, 1);
                IReferenceField field21 = node2.node.getField(0);
                IReferenceField field22 = node2.node.getField(1);

                TestNode node3 = space.addNode(null, 2);
                ISingleReferenceField field31 = node3.node.getField(0);
                IReferenceField field32 = node3.node.getField(1);

                TestNode node4 = space.addNode(null, 3);
                ISingleReferenceField field41 = node4.node.getField(0);
                IReferenceField field42 = node4.node.getField(1);

                field11.set(node3);
                field12.set(node3);
                assertThat(field31.get() == node1, is(true));
                assertThat(Collections.toList(field32.iterator()), is((List) Arrays.asList(node1)));

                field21.add(node4);
                field22.add(node4);
                assertThat(field41.get() == node2, is(true));
                assertThat(Collections.toList(field42.iterator()), is((List) Arrays.asList(node2)));

                field11.set(null);
                field12.set(null);
                assertThat(field31.get(), nullValue());
                assertThat(Collections.toList(field32.iterator()), is((List) Arrays.asList()));

                field21.remove(node4);
                field22.remove(node4);
                assertThat(field41.get(), nullValue());
                assertThat(Collections.toList(field42.iterator()), is((List) Arrays.asList()));

                field11.set(node3);
                field12.set(node3);

                field21.add(node4);
                field22.add(node4);

                node1.node.delete();
                node2.node.delete();

                assertThat(node3.node.isDeleted(), is(true));
                assertThat(node4.node.isDeleted(), is(true));
            }
        });
    }

    public static class TestBodySchemaConfiguration extends ObjectNodeSchemaConfiguration {
        public TestBodySchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields) {
            super(name, fields);
        }

        @Override
        public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            return new ObjectNodeSchema(this, index, fields, documentSchema);
        }

        @Override
        public INodeObject createNode(INode node) {
            return new TestBody();
        }
    }

    private static class TestBody implements INodeBody, Serializable {
        private transient IField field;
        private long field1;
        private String field2;

        public void set(long field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
            field.setModified();
        }

        @Override
        public INode getNode() {
            return field.getNode();
        }

        @Override
        public void validate() {
        }

        @Override
        public void onBeforeCreated(Object primaryKey, Object[] args, Object[] fieldInitializers) {
        }

        @Override
        public void onCreated(Object primaryKey, Object[] args) {
        }

        @Override
        public void onOpened() {
        }

        @Override
        public void onDeleted() {
        }

        @Override
        public void onUnloaded() {
        }

        @Override
        public void onBeforeFlush() {
        }

        @Override
        public void onAfterFlush() {
        }

        @Override
        public void setField(IField field) {
            this.field = field;
        }

        @Override
        public void migrate(INodeBody body) {
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
}
