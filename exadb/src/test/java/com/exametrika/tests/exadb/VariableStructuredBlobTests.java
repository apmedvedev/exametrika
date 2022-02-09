/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.VariableStructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField.IElementIterator;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField.IRecordIterator;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


/**
 * The {@link VariableStructuredBlobTests} are tests for variable structured blob field.
 *
 * @author Medvedev-A
 */
public class VariableStructuredBlobTests {
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
    public void testObjectVariableStructuredBlobField() {
        testObjectVariableStructuredBlobField(false, 0);
    }

    @Test
    public void testObjectFixedVariableStructuredBlobField() {
        testObjectVariableStructuredBlobField(true, 200);
    }

    private void testObjectVariableStructuredBlobField(boolean fixedRecord, int fixedRecordSize) {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1",
                                PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "field2", null, 0, Integer.MAX_VALUE, null, PageType.NORMAL,
                                false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new VariableStructuredBlobFieldSchemaConfiguration("field3", "field3", null, "node1", "field2", true, !fixedRecord,
                                Collections.singleton(TestRecord.class.getName()), fixedRecord, fixedRecordSize)));
        NodeSchemaConfiguration nodeConfiguration3 = new TestNodeSchemaConfiguration("node3",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field", PrimitiveFieldSchemaConfiguration.DataType.INT),
                        new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 256, null, null, null, 0, null, false, false, false,
                                false, false, null, true, false, null, null)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2, nodeConfiguration3)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        final long[] rids2 = new long[10];
        final long[] rids3 = new long[10];
        final long[] eids2 = new long[100];
        final long[] eids3 = new long[100];
        final long[] nids = new long[10];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(1, 0);
                ids[0] = node1.node.getId();

                TestNode node2 = space.addNode(2, 1, node1);
                IVariableStructuredBlobField<TestRecord> field2 = node2.node.getField(1);
                assertThat(field2.getStore() == node1, is(true));
                ids[1] = node2.node.getId();

                TestNode node3 = space.addNode(3, 1);
                IVariableStructuredBlobField<TestRecord> field3 = node3.node.getField(1);
                assertThat(field3.getStore(), nullValue());
                field3.setStore(node1);
                assertThat(field3.getStore() == node1, is(true));
                ids[2] = node3.node.getId();

                for (int i = 0; i < 10; i++) {
                    rids2[i] = field2.addRecord();
                    rids3[i] = field3.addRecord();

                    for (int k = 0; k < 10; k++) {
                        int n = i * 10 + k;
                        eids2[n] = field2.addElement(rids2[i], new TestRecord("test-" + n, n));
                        eids3[n] = field3.addElement(rids3[i], new TestRecord("test2-" + 2 * n, 2 * n));
                    }

                    TestNode node = space.addNode(i, 2);
                    IStringField field = node.node.getField(1);
                    field.set("node-" + i);
                    nids[i] = node.node.getId();
                }

                for (int i = 0; i < 10; i++) {
                    for (int k = 0; k < 10; k++) {
                        int n = i * 10 + k;
                        assertThat(field2.getElement(eids2[n]), is(new TestRecord("test-" + n, n)));
                        assertThat(field3.getElement(eids3[n]), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                    }

                    int k = 0;
                    IElementIterator<TestRecord> it2 = field2.getElements(rids2[i]).iterator();
                    while (it2.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it2.next(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.get(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.getId(), is(eids2[n]));
                        k++;
                    }

                    it2.setNext(eids2[i * 10 + 5]);
                    assertThat(it2.next(), is(new TestRecord("test-" + (i * 10 + 5), (i * 10 + 5))));

                    k = 0;
                    IElementIterator<TestRecord> it3 = field3.getElements(rids3[i]).iterator();
                    while (it3.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it3.next(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.get(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.getId(), is(eids3[n]));
                        k++;
                    }
                }

                int i = 0;
                IRecordIterator<TestRecord> itRecord2 = field2.getRecords().iterator();
                while (itRecord2.hasNext()) {
                    int k = 0;
                    IElementIterator<TestRecord> it2 = itRecord2.next().iterator();
                    assertThat(itRecord2.getId(), is(rids2[i]));
                    while (it2.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it2.next(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.get(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.getId(), is(eids2[n]));
                        k++;
                    }
                    i++;
                }

                itRecord2.setNext(rids2[5]);
                assertThat(itRecord2.next().iterator().next(), is(new TestRecord("test-" + (5 * 10 + 0), 5 * 10 + 0)));

                i = 0;
                IRecordIterator<TestRecord> itRecord3 = field3.getRecords().iterator();
                while (itRecord3.hasNext()) {
                    int k = 0;
                    IElementIterator<TestRecord> it3 = itRecord3.next().iterator();
                    assertThat(itRecord3.getId(), is(rids3[i]));
                    while (it3.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it3.next(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.get(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.getId(), is(eids3[n]));
                        k++;
                    }
                    i++;
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

                TestNode node2 = space.findNodeById(ids[1]);
                IVariableStructuredBlobField<TestRecord> field2 = node2.node.getField(1);
                assertThat(field2.getStore() == node1, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                IVariableStructuredBlobField<TestRecord> field3 = node3.node.getField(1);
                assertThat(field3.getStore() == node1, is(true));

                for (int i = 0; i < 10; i++) {
                    for (int k = 0; k < 10; k++) {
                        int n = i * 10 + k;
                        assertThat(field2.getElement(eids2[n]), is(new TestRecord("test-" + n, n)));
                        assertThat(field3.getElement(eids3[n]), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                    }

                    int k = 0;
                    IElementIterator<TestRecord> it2 = field2.getElements(rids2[i]).iterator();
                    while (it2.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it2.next(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.get(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.getId(), is(eids2[n]));
                        k++;
                    }

                    it2.setNext(eids2[i * 10 + 5]);
                    assertThat(it2.next(), is(new TestRecord("test-" + (i * 10 + 5), (i * 10 + 5))));

                    k = 0;
                    IElementIterator<TestRecord> it3 = field3.getElements(rids3[i]).iterator();
                    while (it3.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it3.next(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.get(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.getId(), is(eids3[n]));
                        k++;
                    }
                }

                int i = 0;
                IRecordIterator<TestRecord> itRecord2 = field2.getRecords().iterator();
                while (itRecord2.hasNext()) {
                    int k = 0;
                    IElementIterator<TestRecord> it2 = itRecord2.next().iterator();
                    assertThat(itRecord2.getId(), is(rids2[i]));
                    while (it2.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it2.next(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.get(), is(new TestRecord("test-" + n, n)));
                        assertThat(it2.getId(), is(eids2[n]));
                        k++;
                    }
                    i++;
                }

                itRecord2.setNext(rids2[5]);
                assertThat(itRecord2.next().iterator().next(), is(new TestRecord("test-" + (5 * 10 + 0), 5 * 10 + 0)));

                i = 0;
                IRecordIterator<TestRecord> itRecord3 = field3.getRecords().iterator();
                while (itRecord3.hasNext()) {
                    int k = 0;
                    IElementIterator<TestRecord> it3 = itRecord3.next().iterator();
                    assertThat(itRecord3.getId(), is(rids3[i]));
                    while (it3.hasNext()) {
                        int n = i * 10 + k;
                        assertThat(it3.next(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.get(), is(new TestRecord("test2-" + 2 * n, 2 * n)));
                        assertThat(it3.getId(), is(eids3[n]));
                        k++;
                    }
                    i++;
                }
            }
        });

        if (fixedRecord) {
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node2 = space.findNodeById(ids[1]);
                    IVariableStructuredBlobField<TestRecord> field2 = node2.node.getField(1);
                    TestNode node3 = space.findNodeById(ids[2]);
                    IVariableStructuredBlobField<TestRecord> field3 = node3.node.getField(1);

                    for (int i = 0; i < 10; i++) {
                        for (int k = 0; k < 10; k++) {
                            int n = i * 10 + k;
                            field2.setElement(eids2[n], new TestRecord("test-" + 3 * n, 3 * n));
                            field3.setElement(eids3[n], new TestRecord("test2-" + 6 * n, 6 * n));
                        }
                    }
                }
            });

            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                    ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                    TestNode node2 = space.findNodeById(ids[1]);
                    IVariableStructuredBlobField<TestRecord> field2 = node2.node.getField(1);
                    TestNode node3 = space.findNodeById(ids[2]);
                    IVariableStructuredBlobField<TestRecord> field3 = node3.node.getField(1);

                    for (int i = 0; i < 10; i++) {
                        for (int k = 0; k < 10; k++) {
                            int n = i * 10 + k;
                            assertThat(field2.getElement(eids2[n]), is(new TestRecord("test-" + 3 * n, 3 * n)));
                            assertThat(field3.getElement(eids3[n]), is(new TestRecord("test2-" + 6 * n, 6 * n)));
                        }
                    }
                }
            });
        }
    }

    private static class TestRecord implements Serializable {
        private final String field1;
        private final long field2;

        public TestRecord(String field1, long field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRecord))
                return false;

            TestRecord record = (TestRecord) o;
            return field1.equals(record.field1) && field2 == record.field2;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(field1, field2);
        }

        @Override
        public String toString() {
            return field1 + ":" + field2;
        }
    }
}
