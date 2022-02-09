/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.NameSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodDatabaseExtensionSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
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
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.aggregator.schema.PeriodSpaceSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.schema.DatabaseSchema;
import com.exametrika.impl.exadb.core.schema.DomainSchema;
import com.exametrika.impl.exadb.core.schema.SchemaSpace;
import com.exametrika.impl.exadb.core.tx.Transaction;
import com.exametrika.impl.exadb.objectdb.schema.FieldSchema;
import com.exametrika.impl.exadb.objectdb.schema.NodeSchema;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSpaceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;
import com.exametrika.tests.exadb.DomainServiceTests.TestDomainServiceSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link SchemaSpaceTests} are tests for schema space.
 *
 * @author Medvedev-A
 * @see SchemaSpace
 */
public class SchemaSpaceTests {
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
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.initialSchemaProvider = new IInitialSchemaProvider() {
            @Override
            public ModularDatabaseSchemaConfiguration getInitialSchema() {
                return new ModularDatabaseSchemaConfiguration("dbName", "dbAlias", "dbDescription",
                        Collections.<ModuleSchemaConfiguration>emptySet(), null, null);
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
    public void testPeriodSpaceConfiguration() throws Throwable {
        new Expected(InvalidArgumentException.class, new Runnable() {
            @Override
            public void run() {
                PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                        new IndexedLocationFieldSchemaConfiguration("field"),
                        Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
                new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                        Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                                null, null, 1, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(node1)),
                                null, null, 1, 1000, false, null)), 0, 0);
            }
        });

        new Expected(InvalidArgumentException.class, new Runnable() {
            @Override
            public void run() {
                PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                        new IndexedLocationFieldSchemaConfiguration("field"),
                        Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
                new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                        Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                                null, null, 5, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(node1)),
                                null, null, 1, 1000, false, null)), 0, 0);
            }
        });

        new Expected(InvalidArgumentException.class, new Runnable() {
            @Override
            public void run() {
                PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                        new IndexedLocationFieldSchemaConfiguration("field"),
                        Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
                new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                        Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                                null, null, 10, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(node1)),
                                null, null, 21, 1000, false, null)), 0, 0);
            }
        });
    }

    @Test
    public void testSchemaChange() throws Throwable {
        PeriodNodeSchemaConfiguration periodNode1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodNodeSchemaConfiguration periodNode2 = new PeriodNodeSchemaConfiguration("node2",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                        new TestFieldSchemaConfiguration("field2")), null);
        PeriodNodeSchemaConfiguration periodNode3 = new PeriodNodeSchemaConfiguration("node3",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                        new TestFieldSchemaConfiguration("field2"), new TestFieldSchemaConfiguration("field3")), null);
        PeriodNodeSchemaConfiguration periodNode4 = new PeriodNodeSchemaConfiguration("node4",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field2")), null);
        NodeSchemaConfiguration node1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        NodeSchemaConfiguration node2 = new ObjectNodeSchemaConfiguration("node2", Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                new TestFieldSchemaConfiguration("field2")));
        NodeSchemaConfiguration node3 = new ObjectNodeSchemaConfiguration("node3", Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                new TestFieldSchemaConfiguration("field2"), new TestFieldSchemaConfiguration("field3")));
        NodeSchemaConfiguration node4 = new ObjectNodeSchemaConfiguration("node4", Arrays.asList(new TestFieldSchemaConfiguration("field2")));

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 1000000, 1000, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space2 = new PeriodSpaceSchemaConfiguration("space2", "space2", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 1000000, 1000, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space3 = new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 1000000, 1000, false, null)), 0, 0);
        ObjectSpaceSchemaConfiguration dataSpace1 = new ObjectSpaceSchemaConfiguration("dataSpace1",
                new HashSet(Arrays.asList(node1, node2, node3)), null);
        ObjectSpaceSchemaConfiguration dataSpace2 = new ObjectSpaceSchemaConfiguration("dataSpace2",
                new HashSet(Arrays.asList(node1, node2, node3)), null);
        ObjectSpaceSchemaConfiguration dataSpace3 = new ObjectSpaceSchemaConfiguration("dataSpace3",
                new HashSet(Arrays.asList(node1, node2, node3)), null);

        long creationTime = Times.getCurrentTime();

        final ModuleSchemaConfiguration configuration1 = new ModuleSchemaConfiguration("test1", new Version(1, 0, 0),
                new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(space1,
                        space2, space3, dataSpace1, dataSpace2, dataSpace3))));
        final ModuleSchemaConfiguration configuration2 = new ModuleSchemaConfiguration("test2", new Version(1, 0, 0),
                new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList(space1,
                        space2, space3, dataSpace1, dataSpace2, dataSpace3))));
        final ModuleSchemaConfiguration configuration3 = new ModuleSchemaConfiguration("test3", new Version(1, 0, 0),
                new DomainSchemaConfiguration("test3", new HashSet(Arrays.asList(space1,
                        space2, space3, dataSpace1, dataSpace2, dataSpace3))));

        SchemaSpace schemaSpace = Tests.get(database, "schemaSpace");

        DatabaseSchema schema1 = schemaSpace.getSchemaCache().getCurrentSchema();
        assertThat(schemaSpace.getSchemaCache().getSchemas().size(), is(1));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(0).getConfiguration(), is(
                new DatabaseSchemaConfiguration("dbName", "dbAlias", "dbDescription")));

        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {

                transaction.addModule(configuration1, null);
                transaction.addModule(configuration2, null);
                transaction.addModule(configuration3, null);

                assertThat(transaction.getConfiguration().getModules().size(), is(3));
                assertThat(transaction.getConfiguration().findModule("test1"), is(configuration1));
                assertThat(transaction.getConfiguration().findModule("test2"), is(configuration2));
                assertThat(transaction.getConfiguration().findModule("test3"), is(configuration3));

                assertThat(transaction.getConfiguration().getName(), is("dbName"));
                assertThat(transaction.getConfiguration().getAlias(), is("dbAlias"));
                assertThat(transaction.getConfiguration().getDescription(), is("dbDescription"));
            }
        });

        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {

                transaction.setDatabaseAlias("dbAlias1");
                transaction.setDatabaseDescription("dbDescription1");
                assertThat(transaction.getConfiguration().getName(), is("dbName"));
                assertThat(transaction.getConfiguration().getAlias(), is("dbAlias1"));
                assertThat(transaction.getConfiguration().getDescription(), is("dbDescription1"));
            }
        });

        final SchemaSpace s = schemaSpace;
        database.transactionSync(new com.exametrika.api.exadb.core.Operation() {
            @Override
            public void run(ITransaction transaction) {
                assertThat(s.allocateFile(((Transaction) transaction).getTransaction()), is(203));
            }
        });

        schema1 = schemaSpace.getSchemaCache().getCurrentSchema();
        assertThat(schemaSpace.getSchemaCache().getSchemas().size(), is(3));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(2) == schema1, is(true));

        assertThat(schema1.getConfiguration().getName(), is("dbName"));
        assertThat(schema1.getConfiguration().getAlias(), is("dbAlias1"));
        assertThat(schema1.getConfiguration().getDescription(), is("dbDescription1"));

        assertThat(schema1.getDomains().size(), is(3));
        DomainSchema domainSchema1 = (DomainSchema) schema1.findDomain("test1");
        checkDomainSchema(configuration1.getSchema().getDomains().get(0), domainSchema1, creationTime, 1);
        DomainSchema domainSchema2 = (DomainSchema) schema1.findDomain("test2");
        checkDomainSchema(configuration2.getSchema().getDomains().get(0), domainSchema2, creationTime, 1);
        DomainSchema domainSchema3 = (DomainSchema) schema1.findDomain("test3");
        checkDomainSchema(configuration3.getSchema().getDomains().get(0), domainSchema3, creationTime, 1);

        assertThat(domainSchema1.getSpaces().size(), is(6));
        for (ISpaceSchema space : domainSchema1.getSpaces())
            assertThat(space.getVersion(), is(2));

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        schemaSpace = Tests.get(database, "schemaSpace");

        schema1 = schemaSpace.getSchemaCache().getCurrentSchema();
        assertThat(schemaSpace.getSchemaCache().getSchemas().size(), is(3));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(2) == schema1, is(true));

        assertThat(schema1.getConfiguration().getName(), is("dbName"));
        assertThat(schema1.getConfiguration().getAlias(), is("dbAlias1"));
        assertThat(schema1.getConfiguration().getDescription(), is("dbDescription1"));

        assertThat(schema1.getDomains().size(), is(3));
        domainSchema1 = (DomainSchema) schema1.findDomain("test1");
        checkDomainSchema(configuration1.getSchema().getDomains().get(0), domainSchema1, creationTime, 1);
        domainSchema2 = (DomainSchema) schema1.findDomain("test2");
        checkDomainSchema(configuration2.getSchema().getDomains().get(0), domainSchema2, creationTime, 1);
        domainSchema3 = (DomainSchema) schema1.findDomain("test3");
        checkDomainSchema(configuration3.getSchema().getDomains().get(0), domainSchema3, creationTime, 1);

        assertThat(domainSchema1.getSpaces().size(), is(6));
        for (ISpaceSchema space : domainSchema1.getSpaces())
            assertThat(space.getVersion(), is(2));

        PeriodSpaceSchemaConfiguration space4 = new PeriodSpaceSchemaConfiguration("space4", "space4", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        null, null, 1000000, 1000, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space32 = new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode2, periodNode4)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode2, periodNode4)),
                        null, null, 1000000, 1000, false, null)), 0, 0);
        ObjectSpaceSchemaConfiguration dataSpace4 = new ObjectSpaceSchemaConfiguration("dataSpace4",
                new HashSet(Arrays.asList(node1, node2, node3)), null);
        ObjectSpaceSchemaConfiguration dataSpace32 = new ObjectSpaceSchemaConfiguration("dataSpace3",
                new HashSet(Arrays.asList(node2, node4)), null);

        long creationTime1 = creationTime;
        creationTime = Times.getCurrentTime();

        final ModuleSchemaConfiguration configuration12 = new ModuleSchemaConfiguration("test1", new Version(1, 1, 0),
                new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(space4,
                        space2, space32, dataSpace4, dataSpace2, dataSpace32))));
        final ModuleSchemaConfiguration configuration4 = new ModuleSchemaConfiguration("test4", new Version(1, 0, 0),
                new DomainSchemaConfiguration("test4", new HashSet(Arrays.asList(space4,
                        space2, space32, dataSpace4, dataSpace2, dataSpace32))));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(configuration4, null);
                transaction.addModule(configuration12, null);
                transaction.removeModule("test3");

                assertThat(transaction.getConfiguration().getModules().size(), is(3));
                assertThat(transaction.getConfiguration().findModule("test1"), is(configuration12));
                assertThat(transaction.getConfiguration().findModule("test2"), is(configuration2));
                assertThat(transaction.getConfiguration().findModule("test3"), nullValue());
                assertThat(transaction.getConfiguration().findModule("test4"), is(configuration4));

                assertThat(transaction.getConfiguration().getName(), is("dbName"));
                assertThat(transaction.getConfiguration().getAlias(), is("dbAlias1"));
                assertThat(transaction.getConfiguration().getDescription(), is("dbDescription1"));
            }
        });

        final SchemaSpace s2 = schemaSpace;
        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new com.exametrika.api.exadb.core.Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        assertThat(s2.allocateFile(((Transaction) transaction).getTransaction()), is(413));
                        throw new RawRollbackException(new NonLocalizedMessage("test"));
                    }
                });
            }
        });

        DatabaseSchema schema2 = schemaSpace.getSchemaCache().getCurrentSchema();
        assertThat(schemaSpace.getSchemaCache().getSchemas().size(), is(4));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(2) == schema1, is(true));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(3) == schema2, is(true));

        assertThat(schema2.getConfiguration().getName(), is("dbName"));
        assertThat(schema2.getConfiguration().getAlias(), is("dbAlias1"));
        assertThat(schema2.getConfiguration().getDescription(), is("dbDescription1"));
        assertThat(schema2.getDomains().size(), is(3));

        DomainSchema domainSchema12 = (DomainSchema) schema2.findDomain("test1");
        checkDomainSchema(configuration12.getSchema().getDomains().get(0), domainSchema12, creationTime, 2);
        assertThat(domainSchema12.getSpaces().get(0).getVersion(), is(2));
        assertThat(domainSchema12.getSpaces().get(0).getConfiguration().getName(), is("dataSpace2"));
        assertThat(domainSchema12.getSpaces().get(1).getVersion(), is(4));
        assertThat(domainSchema12.getSpaces().get(2).getVersion(), is(4));
        assertThat(domainSchema12.getSpaces().get(3).getVersion(), is(2));
        assertThat(domainSchema12.getSpaces().get(3).getConfiguration().getName(), is("space2"));
        assertThat(domainSchema12.getSpaces().get(4).getVersion(), is(4));
        assertThat(domainSchema12.getSpaces().get(5).getVersion(), is(4));

        domainSchema2 = (DomainSchema) schema2.findDomain("test2");
        checkDomainSchema(configuration2.getSchema().getDomains().get(0), domainSchema2, creationTime1, 1);
        assertThat(domainSchema2.getCreationTime() <= creationTime, is(true));
        assertThat(schema2.findDomain("test3"), nullValue());
        DomainSchema domainSchema4 = (DomainSchema) schema2.findDomain("test4");
        checkDomainSchema(configuration4.getSchema().getDomains().get(0), domainSchema4, creationTime, 1);

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        schemaSpace = Tests.get(database, "schemaSpace");

        final SchemaSpace s3 = schemaSpace;
        database.transactionSync(new com.exametrika.api.exadb.core.Operation() {
            @Override
            public void run(ITransaction transaction) {
                assertThat(s3.allocateFile(((Transaction) transaction).getTransaction()), is(309));
            }
        });

        schema2 = schemaSpace.getSchemaCache().getCurrentSchema();
        assertThat(schemaSpace.getSchemaCache().getSchemas().size(), is(4));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(2).getConfiguration(), is(schema1.getConfiguration()));
        assertThat(schemaSpace.getSchemaCache().getSchemas().get(3), is(schema2));

        assertThat(schema2.getConfiguration().getName(), is("dbName"));
        assertThat(schema2.getConfiguration().getAlias(), is("dbAlias1"));
        assertThat(schema2.getConfiguration().getDescription(), is("dbDescription1"));
        assertThat(schema2.getDomains().size(), is(3));

        domainSchema12 = (DomainSchema) schema2.findDomain("test1");
        checkDomainSchema(configuration12.getSchema().getDomains().get(0), domainSchema12, creationTime, 2);
        assertThat(domainSchema12.getSpaces().get(0).getVersion(), is(2));
        assertThat(domainSchema12.getSpaces().get(0).getConfiguration().getName(), is("dataSpace2"));
        assertThat(domainSchema12.getSpaces().get(1).getVersion(), is(4));
        assertThat(domainSchema12.getSpaces().get(2).getVersion(), is(4));
        assertThat(domainSchema12.getSpaces().get(3).getVersion(), is(2));
        assertThat(domainSchema12.getSpaces().get(3).getConfiguration().getName(), is("space2"));
        assertThat(domainSchema12.getSpaces().get(4).getVersion(), is(4));
        assertThat(domainSchema12.getSpaces().get(5).getVersion(), is(4));

        domainSchema2 = (DomainSchema) schema2.findDomain("test2");
        checkDomainSchema(configuration2.getSchema().getDomains().get(0), domainSchema2, creationTime1, 1);
        assertThat(domainSchema2.getCreationTime() <= creationTime, is(true));
        assertThat(schema2.findDomain("test3"), nullValue());
        domainSchema4 = (DomainSchema) schema2.findDomain("test4");
        checkDomainSchema(configuration4.getSchema().getDomains().get(0), domainSchema4, creationTime, 1);

        assertThat(schemaSpace.getSchemaCache().getCurrentSchema().findDomain("test1").getSpaces().get(0) ==
                schemaSpace.getSchemaCache().getSchemas().get(2).findDomain("test1").getSpaces().get(1), is(true));
    }

    @Test
    public void testModuleCombining() throws Throwable {
        PeriodNodeSchemaConfiguration periodNode1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodNodeSchemaConfiguration periodNode2 = new PeriodNodeSchemaConfiguration("node2",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                        new TestFieldSchemaConfiguration("field2")), null);
        PeriodNodeSchemaConfiguration periodNode3 = new PeriodNodeSchemaConfiguration("node3",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                        new TestFieldSchemaConfiguration("field2"), new TestFieldSchemaConfiguration("field3")), null);
        NodeSchemaConfiguration node1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        NodeSchemaConfiguration node2 = new ObjectNodeSchemaConfiguration("node2", Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                new TestFieldSchemaConfiguration("field2")));
        NodeSchemaConfiguration node3 = new ObjectNodeSchemaConfiguration("node3", Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                new TestFieldSchemaConfiguration("field2"), new TestFieldSchemaConfiguration("field3")));

        PeriodSpaceSchemaConfiguration periodSpace1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        "node1", "node1", 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2, periodNode3)),
                        "node1", "node1", 1000000, 1000, false, null)), 1, 1);
        PeriodSpaceSchemaConfiguration periodSpace11 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2)),
                        "node1", "node1", 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2)),
                        "node1", "node1", 1000000, 1000, false, null)), 1, 1);
        PeriodSpaceSchemaConfiguration periodSpace12 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode3)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode3)),
                        null, null, 1000000, 1000, false, null)), 0, 0);

        ObjectSpaceSchemaConfiguration objectSpace1 = new ObjectSpaceSchemaConfiguration("dataSpace1", "dataSpace1", "dataSpace1 description",
                new HashSet(Arrays.asList(node1, node2, node3)), "node1", 1, 1);
        ObjectSpaceSchemaConfiguration objectSpace11 = new ObjectSpaceSchemaConfiguration("dataSpace1", "dataSpace1", "dataSpace1 description",
                new HashSet(Arrays.asList(node1, node2)), "node1", 1, 1);
        ObjectSpaceSchemaConfiguration objectSpace12 = new ObjectSpaceSchemaConfiguration("dataSpace1",
                new HashSet(Arrays.asList(node3)), null);
        ObjectSpaceSchemaConfiguration objectSpace2 = new ObjectSpaceSchemaConfiguration("dataSpace2",
                new HashSet(Arrays.asList(node1)), null);
        ObjectSpaceSchemaConfiguration objectSpace3 = new ObjectSpaceSchemaConfiguration("dataSpace3",
                new HashSet(Arrays.asList(node1)), null);

        final ModuleSchemaConfiguration module1 = new ModuleSchemaConfiguration("test1", new Version(1, 0, 0),
                new DatabaseSchemaConfiguration("db1", "db1", null, com.exametrika.common.utils.Collections.asSet(
                        new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(objectSpace11, periodSpace11, objectSpace2)),
                                new HashSet(Arrays.asList(new TestDomainServiceSchemaConfiguration("service1", "value1"),
                                        new TestDomainServiceSchemaConfiguration("service2", "value2")))),
                        new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList()), new HashSet(Arrays.asList()))),
                        Collections.<DatabaseExtensionSchemaConfiguration>singleton(
                                new PeriodDatabaseExtensionSchemaConfiguration(new NameSpaceSchemaConfiguration()))));

        final ModuleSchemaConfiguration module2 = new ModuleSchemaConfiguration("test2", new Version(1, 0, 0),
                new DatabaseSchemaConfiguration("db1", "db1", null, com.exametrika.common.utils.Collections.asSet(
                        new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(objectSpace12, periodSpace12, objectSpace3)),
                                new HashSet(Arrays.asList(new TestDomainServiceSchemaConfiguration("service3", "value3")))),
                        new DomainSchemaConfiguration("test3", new HashSet(Arrays.asList()), new HashSet(Arrays.asList()))),
                        Collections.<DatabaseExtensionSchemaConfiguration>emptySet()));

        ModularDatabaseSchemaConfiguration modular = new ModularDatabaseSchemaConfiguration("db", com.exametrika.common.utils.Collections.asSet(module1, module2));
        DatabaseSchemaConfiguration combinedDb = new DatabaseSchemaConfiguration("db", "db", null, com.exametrika.common.utils.Collections.asSet(
                new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(objectSpace1, periodSpace1, objectSpace2, objectSpace3)),
                        new HashSet(Arrays.asList(new TestDomainServiceSchemaConfiguration("service1", "value1"),
                                new TestDomainServiceSchemaConfiguration("service2", "value2"), new TestDomainServiceSchemaConfiguration("service3", "value3")))),
                new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList()), new HashSet(Arrays.asList())),
                new DomainSchemaConfiguration("test3", new HashSet(Arrays.asList()), new HashSet(Arrays.asList()))),
                Collections.<DatabaseExtensionSchemaConfiguration>singleton(
                        new PeriodDatabaseExtensionSchemaConfiguration(new NameSpaceSchemaConfiguration())));
        assertThat(modular.getCombinedSchema(), is(combinedDb));
    }

    @Test
    public void testSchemaObjects() throws Throwable {
        PeriodNodeSchemaConfiguration periodNode1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodNodeSchemaConfiguration periodNode2 = new PeriodNodeSchemaConfiguration("node2",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                        new TestFieldSchemaConfiguration("field2")), null);
        PeriodNodeSchemaConfiguration periodNode3 = new PeriodNodeSchemaConfiguration("node3",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                        new TestFieldSchemaConfiguration("field2"), new TestFieldSchemaConfiguration("field3")), null);
        NodeSchemaConfiguration node1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        NodeSchemaConfiguration node2 = new ObjectNodeSchemaConfiguration("node2", Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                new TestFieldSchemaConfiguration("field2")));
        NodeSchemaConfiguration node3 = new ObjectNodeSchemaConfiguration("node3", Arrays.asList(new TestFieldSchemaConfiguration("field1"),
                new TestFieldSchemaConfiguration("field2"), new TestFieldSchemaConfiguration("field3")));

        PeriodSpaceSchemaConfiguration periodSpace11 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode1, periodNode2)),
                        "node1", "node1", 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode1, periodNode2)),
                        "node1", "node1", 1000000, 1000, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration periodSpace12 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(periodNode3)),
                        null, null, 100000, 100, false, null), new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(periodNode3)),
                        null, null, 1000000, 1000, false, null)), 0, 0);

        ObjectSpaceSchemaConfiguration objectSpace11 = new ObjectSpaceSchemaConfiguration("dataSpace1", "dataSpace1", "dataSpace1 description",
                new HashSet(Arrays.asList(node1, node2)), "node1", 0, 0);
        ObjectSpaceSchemaConfiguration objectSpace12 = new ObjectSpaceSchemaConfiguration("dataSpace1",
                new HashSet(Arrays.asList(node3)), null);
        ObjectSpaceSchemaConfiguration objectSpace2 = new ObjectSpaceSchemaConfiguration("dataSpace2",
                new HashSet(Arrays.asList(node1)), null);
        ObjectSpaceSchemaConfiguration objectSpace3 = new ObjectSpaceSchemaConfiguration("dataSpace3",
                new HashSet(Arrays.asList(node1)), null);

        final ModuleSchemaConfiguration module1 = new ModuleSchemaConfiguration("test1", new Version(1, 0, 0),
                new DatabaseSchemaConfiguration("db1", "db1", null, com.exametrika.common.utils.Collections.asSet(
                        new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(objectSpace11, periodSpace11, objectSpace2)),
                                new HashSet(Arrays.asList(new TestDomainServiceSchemaConfiguration("service1", "value1"),
                                        new TestDomainServiceSchemaConfiguration("service2", "value2")))),
                        new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList()), new HashSet(Arrays.asList()))),
                        Collections.<DatabaseExtensionSchemaConfiguration>emptySet()));

        final ModuleSchemaConfiguration module2 = new ModuleSchemaConfiguration("test2", new Version(1, 0, 0),
                new DatabaseSchemaConfiguration("db1", "db1", null, com.exametrika.common.utils.Collections.asSet(
                        new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(objectSpace12, periodSpace12, objectSpace3)),
                                new HashSet(Arrays.asList(new TestDomainServiceSchemaConfiguration("service3", "value3")))),
                        new DomainSchemaConfiguration("test3", new HashSet(Arrays.asList()), new HashSet(Arrays.asList()))),
                        Collections.<DatabaseExtensionSchemaConfiguration>emptySet()));

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(module1, null);
                transaction.addModule(module2, null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                assertThat(schema.findSchemaById("database") != null, is(true));
                assertThat(schema.findSchemaById("domain:test1") != null, is(true));
                assertThat(schema.findSchemaById("domain:test2") != null, is(true));
                assertThat(schema.findSchemaById("space:test1.space1") != null, is(true));
                assertThat(schema.findSchemaById("space:test1.dataSpace1") != null, is(true));
                assertThat(schema.findSchemaById("domainService:test1.service1") != null, is(true));
                assertThat(schema.findSchemaById("period:test1.space1.p1") != null, is(true));
                assertThat(schema.findSchemaById("node:test1.space1.p1.node2") != null, is(true));
                assertThat(schema.findSchemaById("node:test1.dataSpace1.node1") != null, is(true));
                assertThat(schema.findSchemaById("field:test1.space1.p2.node2.field") != null, is(true));
                assertThat(schema.findSchemaById("field:test1.dataSpace1.node1.field1") != null, is(true));

                IFieldSchema fieldSchema = schema.findSchemaById("field:test1.dataSpace1.node1.field1");
                assertThat(fieldSchema.getId(), is("field:test1.dataSpace1.node1.field1"));
                assertThat(fieldSchema.getQualifiedName(), is("test1.dataSpace1.node1.field1"));
                assertThat(fieldSchema.getQualifiedAlias(), is("test1.dataSpace1.node1.field1"));
                assertThat(fieldSchema.getRoot() == schema, is(true));
                assertThat(fieldSchema.getType(), is("field"));

                assertThat(com.exametrika.common.utils.Collections.toList(schema.getChildren().iterator()).size(), is(3));
                assertThat(com.exametrika.common.utils.Collections.toList(schema.getChildren("domain").iterator()).size(), is(3));
                assertThat(schema.findChild("domain", "test1") == schema.findSchemaById("domain:test1"), is(true));

                IDomainSchema domainSchema = schema.findSchemaById("domain:test1");
                assertThat(com.exametrika.common.utils.Collections.toList(domainSchema.getChildren().iterator()).size(), is(7));
                assertThat(com.exametrika.common.utils.Collections.toList(domainSchema.getChildren("space").iterator()).size(), is(4));
                assertThat(com.exametrika.common.utils.Collections.toList(domainSchema.getChildren("domainService").iterator()).size(), is(3));
                assertThat(domainSchema.findChild("space", "space1") == schema.findSchemaById("space:test1.space1"), is(true));
                assertThat(domainSchema.findChild("space", "service1") == schema.findSchemaById("space:test1.service1"), is(true));

                IObjectSpaceSchema objectSchema = schema.findSchemaById("space:test1.dataSpace1");
                assertThat(com.exametrika.common.utils.Collections.toList(objectSchema.getChildren().iterator()).size(), is(3));
                assertThat(com.exametrika.common.utils.Collections.toList(objectSchema.getChildren("node").iterator()).size(), is(3));
                assertThat(objectSchema.findChild("node", "node1") == schema.findSchemaById("node:test1.dataSpace1.node1"), is(true));

                IPeriodSpaceSchema periodSchema = schema.findSchemaById("space:test1.space1");
                ICycleSchema cycleSchema = schema.findSchemaById("period:test1.space1.p1");
                assertThat(com.exametrika.common.utils.Collections.toList(periodSchema.getChildren().iterator()).size(), is(2));
                assertThat(com.exametrika.common.utils.Collections.toList(cycleSchema.getChildren("node").iterator()).size(), is(3));
                assertThat(com.exametrika.common.utils.Collections.toList(periodSchema.getChildren("period").iterator()).size(), is(2));
                assertThat(cycleSchema.findChild("node", "node1") == schema.findSchemaById("node:test1.space1.p1.node1"), is(true));
                assertThat(periodSchema.findChild("period", "p1") == schema.findSchemaById("period:test1.space1.p1"), is(true));

                INodeSchema nodeSchema = schema.findSchemaById("node:test1.dataSpace1.node1");
                assertThat(com.exametrika.common.utils.Collections.toList(nodeSchema.getChildren().iterator()).size(), is(1));
                assertThat(com.exametrika.common.utils.Collections.toList(nodeSchema.getChildren("field").iterator()).size(), is(1));
                assertThat(nodeSchema.findChild("field", "field1") == schema.findSchemaById("field:test1.dataSpace1.node1.field1"), is(true));
            }
        });
    }

    private void checkDomainSchema(DomainSchemaConfiguration configuration, DomainSchema schema, long creationTime, int version) throws Throwable {
        assertThat(schema.getCreationTime() >= creationTime, is(true));
        assertThat(schema.getVersion(), is(version));
        assertThat(schema.getConfiguration(), is(configuration));

        assertThat(configuration.getSpaces().size(), is(schema.getSpaces().size()));

        int i = 0;
        for (SpaceSchemaConfiguration spaceSchemaConfiguration : configuration.getSpaces()) {
            assertThat(configuration.findSpace(spaceSchemaConfiguration.getName()) == spaceSchemaConfiguration, is(true));
            ISpaceSchema spaceSchema = schema.getSpaces().get(i);
            assertThat(spaceSchema.getConfiguration(), is(spaceSchemaConfiguration));

            if (spaceSchemaConfiguration instanceof NodeSpaceSchemaConfiguration) {
                NodeSpaceSchemaConfiguration nodeSpaceSchemaConfiguration = (NodeSpaceSchemaConfiguration) spaceSchemaConfiguration;

                NodeSpaceSchema nodeSpaceSchema = (NodeSpaceSchema) spaceSchema;

                assertThat(nodeSpaceSchemaConfiguration.getNodes().size(), is(nodeSpaceSchema.getNodes().size()));

                int k = 0;
                for (NodeSchemaConfiguration nodeSchemaConfiguration : nodeSpaceSchemaConfiguration.getNodes()) {
                    NodeSchema nodeSchema = (NodeSchema) nodeSpaceSchema.getNodes().get(k++);
                    assertThat(nodeSchema.getConfiguration(), is(nodeSchemaConfiguration));

                    assertThat(nodeSchemaConfiguration.getFields().size(), is(nodeSchema.getFields().size()));

                    int increment = nodeSchemaConfiguration instanceof PeriodNodeSchemaConfiguration ? 1 : 0;
                    for (int m = 0; m < nodeSchemaConfiguration.getFields().size() - increment; m++) {
                        TestFieldSchemaConfiguration fieldSchemaConfiguration = (TestFieldSchemaConfiguration) nodeSchemaConfiguration.getFields().get(m + increment);
                        TestFieldSchema fieldSchema = (TestFieldSchema) nodeSchema.getFields().get(m + increment);

                        assertThat(fieldSchema.getConfiguration(), is(fieldSchemaConfiguration));
                    }
                }

                ObjectSpaceSchema dataSpaceSchema = (ObjectSpaceSchema) nodeSpaceSchema;
                long offset = Tests.get(dataSpaceSchema, "spaceFileIndexFileOffset");
                assertThat(offset != 0, is(true));
            } else if (spaceSchemaConfiguration instanceof PeriodSpaceSchemaConfiguration) {
                PeriodSpaceSchema periodSpaceSchema = (PeriodSpaceSchema) schema.getSpaces().get(i);
                assertThat(((PeriodSpaceSchemaConfiguration) spaceSchemaConfiguration).getPeriods().size(), is(periodSpaceSchema.getCycles().size()));

                int k = 0;
                for (PeriodSchemaConfiguration periodSchemaConfiguration : ((PeriodSpaceSchemaConfiguration) spaceSchemaConfiguration).getPeriods()) {
                    CycleSchema cycle = (CycleSchema) periodSpaceSchema.getCycles().get(k++);
                    assertThat(cycle.getConfiguration(), is(periodSchemaConfiguration));
                    assertThat(cycle.getParent() == periodSpaceSchema, is(true));
                    long offset = Tests.get(cycle, "dataFileOffset");
                    assertThat(offset != 0, is(true));

                    assertThat(periodSchemaConfiguration.getNodes().size(), is(cycle.getNodes().size()));

                    int n = 0;
                    for (NodeSchemaConfiguration nodeSchemaConfiguration : periodSchemaConfiguration.getNodes()) {
                        NodeSchema nodeSchema = (NodeSchema) cycle.getNodes().get(n++);
                        assertThat(nodeSchema.getConfiguration(), is(nodeSchemaConfiguration));

                        assertThat(nodeSchemaConfiguration.getFields().size(), is(nodeSchema.getFields().size()));

                        int increment = nodeSchemaConfiguration instanceof PeriodNodeSchemaConfiguration ? 1 : 0;
                        for (int m = 0; m < nodeSchemaConfiguration.getFields().size() - increment; m++) {
                            TestFieldSchemaConfiguration fieldSchemaConfiguration = (TestFieldSchemaConfiguration) nodeSchemaConfiguration.getFields().get(m + increment);
                            TestFieldSchema fieldSchema = (TestFieldSchema) nodeSchema.getFields().get(m + increment);

                            assertThat(fieldSchema.getConfiguration(), is(fieldSchemaConfiguration));
                        }
                    }
                }
            } else
                Assert.error();

            i++;
        }
    }

    public static class TestFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
        public TestFieldSchemaConfiguration(String name) {
            super(name, name, null, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, 0);
        }

        @Override
        public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
            return new TestFieldSchema(this, index, offset);
        }

        @Override
        public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
            return true;
        }

        @Override
        public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
            return new TestFieldConverter();
        }

        @Override
        public Object createInitializer() {
            return null;
        }
    }

    public static class TestFieldConverter implements IFieldConverter {
        @Override
        public void convert(IField oldField, IField newField, IFieldMigrationSchema migrationSchema) {
        }
    }

    public static class TestFieldSchema extends FieldSchema implements IFieldSchema {
        public TestFieldSchema(TestFieldSchemaConfiguration configuration, int index, int offset) {
            super(configuration, index, offset);
        }

        @Override
        public TestFieldSchemaConfiguration getConfiguration() {
            return (TestFieldSchemaConfiguration) super.getConfiguration();
        }

        @Override
        public IFieldObject createField(IField field) {
            return (IFieldObject) field;
        }

        @Override
        public void resolveDependencies() {
        }

        @Override
        public void validate(IField field) {
        }

        @Override
        public int getIndexTotalIndex() {
            return -1;
        }
    }

    public static class TestOperation extends RawOperation {
        RuntimeException exception;

        public TestOperation() {
        }

        public TestOperation(boolean readOnly) {
            super(readOnly);
        }

        public TestOperation(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public void run(IRawTransaction transaction) {
            if (exception != null)
                throw exception;

        }
    }

    public static class TestTransactionProvider implements ITransactionProvider {
        private final IRawTransaction transaction;

        public TestTransactionProvider(IRawTransaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public IRawTransaction getRawTransaction() {
            return transaction;
        }

        @Override
        public ITransaction getTransaction() {
            return null;
        }
    }
}
