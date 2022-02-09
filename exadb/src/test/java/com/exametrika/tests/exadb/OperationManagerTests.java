/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
import com.exametrika.api.exadb.core.config.schema.FileArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
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
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.OperationManager;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


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
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), "node2", 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "",
                new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)), "node2", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1,
                space2)));
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
}
