/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodNode;
import com.exametrika.impl.aggregator.nodes.PeriodNodeObject;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.schema.BlobFieldSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link BlobTests} are tests for blob field.
 *
 * @author Medvedev-A
 */
public class BlobTests {
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
    public void testPeriodBlobFields() {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field1"),
                Arrays.asList(new BlobStoreFieldSchemaConfiguration("field2")), null);
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodNodeSchemaConfiguration("node2",
                new IndexedLocationFieldSchemaConfiguration("field1"),
                Arrays.asList(new TestBlobFieldSchemaConfiguration("field3", "node1", "field2")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)),
                        null, null, 1000000, 100, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new ISchemaOperation() {
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
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node1 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(1, 1), 0)).getNode();
                IBlobStoreField field1 = node1.getField(1);
                ids[0] = node1.getId();

                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(2, 2), 1)).getNode();
                IBlobField field2 = node2.getField(1);
                assertThat(field2.get(), nullValue());
                field2.set(field1.createBlob());
                ids[1] = node2.getId();

                PeriodNode node3 = (PeriodNode) ((PeriodNodeObject) period.addNode(new Location(3, 3), 1)).getNode();
                IBlobField field3 = node3.getField(1);
                assertThat(field3.get(), nullValue());
                field3.set(field1.createBlob());
                ids[2] = node3.getId();
            }
        });

        final long[] ixs = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node1 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[0])).getNode();
                assertThat(node1.getLocation(), is(new Location(1, 1)));

                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[1])).getNode();
                IBlobField field2 = node2.getField(1);
                assertThat(field2.get() != null, is(true));

                PeriodNode node3 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[2])).getNode();
                IBlobField field3 = node3.getField(1);
                assertThat(field3.get() != null, is(true));

                IBlobSerialization serialization2 = field2.get().createSerialization();
                IBlobSerialization serialization3 = field3.get().createSerialization();

                long position = 0;
                for (int i = 0; i < 100; i++) {
                    if (i == 50)
                        position = serialization2.getPosition();

                    serialization2.writeByteArray(createBuffer(12345, 567 + i));
                    serialization3.writeByteArray(createBuffer(17171, 123 + i));
                }

                serialization2.setPosition(serialization2.getBeginPosition());
                serialization3.setPosition(serialization3.getBeginPosition());

                for (int i = 0; i < 100; i++) {
                    assertThat(serialization2.readByteArray(), is(createBuffer(12345, 567 + i)));
                    assertThat(serialization3.readByteArray(), is(createBuffer(17171, 123 + i)));
                }

                serialization2.setPosition(position);
                assertThat(serialization2.readByteArray(), is(createBuffer(12345, 567 + 50)));
                ixs[0] = position;
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                PeriodNode node2 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[1])).getNode();
                IBlobField field2 = node2.getField(1);
                assertThat(field2.get() != null, is(true));

                PeriodNode node3 = (PeriodNode) ((PeriodNodeObject) period.findNodeById(ids[2])).getNode();
                IBlobField field3 = node3.getField(1);
                assertThat(field3.get() != null, is(true));

                IBlobDeserialization deserialization2 = field2.get().createDeserialization();
                IBlobDeserialization deserialization3 = field3.get().createDeserialization();

                for (int i = 0; i < 100; i++) {
                    assertThat(deserialization2.readByteArray(), is(createBuffer(12345, 567 + i)));
                    assertThat(deserialization3.readByteArray(), is(createBuffer(17171, 123 + i)));
                }

                deserialization2.setPosition(ixs[0]);
                assertThat(deserialization2.readByteArray(), is(createBuffer(12345, 567 + 50)));
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
            b[i] = (char) (i + base);

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
}
