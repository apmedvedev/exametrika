/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectOperationManager;
import com.exametrika.api.exadb.objectdb.config.schema.BinaryFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IBinaryField;
import com.exametrika.api.exadb.objectdb.fields.ITextField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.schema.BlobFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;


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
    public void testDataBlobFields() {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null, PageType.NORMAL,
                                false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TestBlobFieldSchemaConfiguration("field3", "node1", "field2")));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(1, 0);
                IBlobStoreField field1 = node1.node.getField(1);
                ids[0] = node1.node.getId();

                TestNode node2 = space.addNode(2, 1, node1);
                IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get() != null, is(true));
                ids[1] = node2.node.getId();

                TestNode node3 = space.addNode(3, 1);
                IBlobField field3 = node3.node.getField(1);
                assertThat(field3.get(), nullValue());
                field3.set(field1.createBlob());
                ids[2] = node3.node.getId();
            }
        });

        final long[] ixs = new long[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                assertThat(node1 != null, is(true));

                TestNode node2 = space.findNodeById(ids[1]);
                IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get() != null, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                IBlobField field3 = node3.node.getField(1);
                assertThat(field3.get() != null, is(true));

                IBlobSerialization serialization2 = field2.get().createSerialization();
                IBlobSerialization serialization3 = field3.get().createSerialization();

                long position = 0;
                for (int i = 0; i < 50; i++) {
                    serialization2.writeByteArray(createBuffer(12345, 567 + i));
                    serialization3.writeByteArray(createBuffer(17171, 123 + i));
                }

                serialization2.updateEndPosition();
                serialization3.updateEndPosition();
                serialization2 = field2.get().createSerialization();
                serialization3 = field3.get().createSerialization();

                for (int i = 50; i < 100; i++) {
                    if (i == 50)
                        position = serialization2.getPosition();

                    serialization2.writeByteArray(createBuffer(12345, 567 + i));
                    serialization3.writeByteArray(createBuffer(17171, 123 + i));
                }

                serialization2.updateEndPosition();
                serialization3.updateEndPosition();

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

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get() != null, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                IBlobField field3 = node3.node.getField(1);
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

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                final IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get() != null, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                IBlobField field3 = node3.node.getField(1);
                assertThat(field3.get() != null, is(true));

                final IBlobSerialization serialization2 = field2.get().createSerialization();
                field3.get().delete();
                field3.set(null);

                final ByteArray buf1 = createBuffer(12345, 567);
                serialization2.setPosition(serialization2.getBeginPosition());
                serialization2.writeByteArray(buf1);
                serialization2.removeRest();

                try {
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            IBlobSerialization serialization = field2.get().createSerialization();
                            serialization2.setPosition(serialization2.getBeginPosition());
                            assertThat(serialization.readByteArray(), is(buf1));

                            for (int i = 0; i < 16000; i++)
                                serialization.readByte();

                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                final IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get() != null, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                IBlobField field3 = node3.node.getField(1);
                assertThat(field3.get(), nullValue());

                final ByteArray buf1 = createBuffer(12345, 567);

                try {
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            IBlobDeserialization deserialization = field2.get().createDeserialization();
                            assertThat(deserialization.readByteArray(), is(buf1));

                            for (int i = 0; i < 17000; i++)
                                deserialization.readByte();

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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                node1.node.delete();

                TestNode node2 = space.findNodeById(ids[1]);
                final IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get(), nullValue());
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                final IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get(), nullValue());
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                final IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get(), nullValue());

                TestNode node1 = space.addNode(10, 0);
                IBlobStoreField field1 = node1.node.getField(1);
                field2.set(field1.createBlob());

                IBlobSerialization serialization2 = field2.get().createSerialization();
                serialization2.setPosition(serialization2.getBeginPosition());

                long position = 0;
                for (int i = 0; i < 100; i++) {
                    if (i == 50)
                        position = serialization2.getPosition();
                    serialization2.writeByteArray(createBuffer(12345, 567 + i));
                }

                serialization2.setPosition(serialization2.getBeginPosition());

                for (int i = 0; i < 100; i++) {
                    assertThat(serialization2.readByteArray(), is(createBuffer(12345, 567 + i)));
                }

                serialization2.setPosition(position);
                assertThat(serialization2.readByteArray(), is(createBuffer(12345, 567 + 50)));
                ixs[0] = position;
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                IBlobField field2 = node2.node.getField(1);
                assertThat(field2.get() != null, is(true));

                IBlobDeserialization deserialization2 = field2.get().createDeserialization();

                for (int i = 0; i < 100; i++) {
                    assertThat(deserialization2.readByteArray(), is(createBuffer(12345, 567 + i)));
                }

                deserialization2.setPosition(ixs[0]);
                assertThat(deserialization2.readByteArray(), is(createBuffer(12345, 567 + 50)));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                IBlobField field2 = node2.node.getField(1);
                IBlob blob = field2.get();
                node2.node.delete();
                try {
                    assertThat((Boolean) Tests.get(blob, "deleted"), is(true));
                } catch (Exception e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testStreamBlobFields() {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TextFieldSchemaConfiguration("field3", "node1", "field2"),
                        new BinaryFieldSchemaConfiguration("field4", "node1", "field2")));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

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

        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(1, 0);
                ids[0] = node1.node.getId();

                TestNode node2 = space.addNode(2, 1, node1);
                ids[1] = node2.node.getId();
                ITextField field21 = node2.node.getField(1);
                assertThat(field21.getStore() == node1, is(true));
                IBinaryField field22 = node2.node.getField(2);
                assertThat(field22.getStore() == node1, is(true));

                TestNode node3 = space.addNode(3, 1);
                ids[2] = node3.node.getId();
                ITextField field31 = node3.node.getField(1);
                assertThat(field31.getStore(), nullValue());
                field31.setStore(node1);
                assertThat(field31.getStore() == node1, is(true));

                IBinaryField field32 = node3.node.getField(2);
                assertThat(field32.getStore(), nullValue());
                field32.setStore(node1);
                assertThat(field32.getStore() == node1, is(true));

                TestNode node4 = space.addNode(4, 1);
                ids[3] = node4.node.getId();

                IBinaryField field42 = node4.node.getField(2);
                assertThat(field42.getStore(), nullValue());
                field42.setStore(node1);
                assertThat(field42.getStore() == node1, is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                ids[0] = node1.node.getId();

                TestNode node2 = space.findNodeById(ids[1]);
                ids[1] = node2.node.getId();
                ITextField field21 = node2.node.getField(1);
                assertThat(field21.getStore() == node1, is(true));
                IBinaryField field22 = node2.node.getField(2);
                assertThat(field22.getStore() == node1, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                ids[2] = node3.node.getId();
                ITextField field31 = node3.node.getField(1);
                assertThat(field31.getStore() == node1, is(true));
                IBinaryField field32 = node3.node.getField(2);
                assertThat(field32.getStore() == node1, is(true));

                TestNode node4 = space.findNodeById(ids[3]);
                ids[3] = node4.node.getId();
                IBinaryField field42 = node4.node.getField(2);
                assertThat(field42.getStore() == node1, is(true));

                Writer writer1 = field21.createWriter();
                Writer writer2 = field31.createWriter();
                OutputStream out1 = field22.createOutputStream();
                OutputStream out2 = field32.createOutputStream();

                try {
                    for (int i = 0; i < 100; i++) {
                        ByteArray buf1 = createBuffer(12345, 567 + i);
                        ByteArray buf2 = createBuffer(17171, 123 + i);
                        String str1 = createString(12345, 567 + i);
                        String str2 = createString(17171, 123 + i);
                        out1.write(buf1.getBuffer(), buf1.getOffset(), buf1.getLength());
                        out2.write(buf2.getBuffer(), buf2.getOffset(), buf2.getLength());

                        writer1.write(str1);
                        writer2.write(str2);
                    }

                    writer1.close();
                    writer2.close();
                    out1.close();
                    out2.close();

                    Reader reader1 = field21.createReader();
                    Reader reader2 = field31.createReader();
                    InputStream in1 = field22.createInputStream();
                    InputStream in2 = field32.createInputStream();

                    for (int i = 0; i < 100; i++) {
                        ByteArray buf1 = createBuffer(12345, 567 + i);
                        ByteArray buf2 = createBuffer(17171, 123 + i);
                        String str1 = createString(12345, 567 + i);
                        String str2 = createString(17171, 123 + i);

                        read(reader1, str1);
                        read(reader2, str2);
                        read(in1, buf1);
                        read(in2, buf2);
                    }
                    assertThat(reader1.read(new char[100]), is(-1));
                    assertThat(reader2.read(new char[100]), is(-1));

                    assertThat(in1.read(new byte[100]), is(-1));
                    assertThat(in2.read(new byte[100]), is(-1));

                    ByteArray buf = createBuffer(17171, 123);
                    field42.write(buf);
                    assertThat(field42.<ByteArray>read(), is(buf));
                } catch (IOException e) {
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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);

                TestNode node2 = space.findNodeById(ids[1]);
                ITextField field21 = node2.node.getField(1);
                assertThat(field21.getStore() == node1, is(true));
                IBinaryField field22 = node2.node.getField(2);
                assertThat(field22.getStore() == node1, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                ITextField field31 = node3.node.getField(1);
                assertThat(field31.getStore() == node1, is(true));
                IBinaryField field32 = node3.node.getField(2);
                assertThat(field32.getStore() == node1, is(true));

                TestNode node4 = space.findNodeById(ids[3]);
                IBinaryField field42 = node4.node.getField(2);
                assertThat(field42.getStore() == node1, is(true));

                Reader reader1 = field21.createReader();
                Reader reader2 = field31.createReader();
                InputStream in1 = field22.createInputStream();
                InputStream in2 = field32.createInputStream();

                try {
                    for (int i = 0; i < 100; i++) {
                        ByteArray buf1 = createBuffer(12345, 567 + i);
                        ByteArray buf2 = createBuffer(17171, 123 + i);
                        String str1 = createString(12345, 567 + i);
                        String str2 = createString(17171, 123 + i);

                        read(reader1, str1);
                        read(reader2, str2);
                        read(in1, buf1);
                        read(in2, buf2);
                    }

                    assertThat(reader1.read(new char[100]), is(-1));
                    assertThat(reader2.read(new char[100]), is(-1));

                    assertThat(in1.read(new byte[100]), is(-1));
                    assertThat(in2.read(new byte[100]), is(-1));

                    ByteArray buf = createBuffer(17171, 123);
                    assertThat(field42.<ByteArray>read(), is(buf));
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node2 = space.findNodeById(ids[1]);
                ITextField field21 = node2.node.getField(1);
                field21.clear();
                IBinaryField field22 = node2.node.getField(2);
                field22.clear();

                Reader reader1 = field21.createReader();
                InputStream in1 = field22.createInputStream();

                try {
                    assertThat(reader1.read(new char[1]), is(-1));
                    assertThat(in1.read(new byte[1]), is(-1));
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }

                TestNode node4 = space.findNodeById(ids[3]);
                IBinaryField field42 = node4.node.getField(2);
                field42.clear();
                assertThat(field42.read(), nullValue());
            }
        });
    }

    @Test
    public void testCompaction() {
        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new BlobStoreFieldSchemaConfiguration("field2", "fiel2", null, 0, Integer.MAX_VALUE, null,
                                PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field1", DataType.INT),
                        new TextFieldSchemaConfiguration("field3", "node1", "field2"),
                        new BinaryFieldSchemaConfiguration("field4", "node1", "field2")));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "", new HashSet(Arrays.asList(nodeConfiguration1,
                nodeConfiguration2)), null, 0, 0);

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
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.addNode(1, 0);
                ids[0] = node1.node.getId();

                TestNode node2 = space.addNode(2, 1, node1);
                ids[1] = node2.node.getId();
                ITextField field21 = node2.node.getField(1);
                assertThat(field21.getStore() == node1, is(true));
                IBinaryField field22 = node2.node.getField(2);
                assertThat(field22.getStore() == node1, is(true));

                TestNode node3 = space.addNode(3, 1);
                ids[2] = node3.node.getId();
                ITextField field31 = node3.node.getField(1);
                assertThat(field31.getStore(), nullValue());
                field31.setStore(node1);
                assertThat(field31.getStore() == node1, is(true));

                IBinaryField field32 = node3.node.getField(2);
                assertThat(field32.getStore(), nullValue());
                field32.setStore(node1);
                assertThat(field32.getStore() == node1, is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();

                TestNode node1 = space.findNodeById(ids[0]);
                ids[0] = node1.node.getId();

                TestNode node2 = space.findNodeById(ids[1]);
                ids[1] = node2.node.getId();
                ITextField field21 = node2.node.getField(1);
                assertThat(field21.getStore() == node1, is(true));
                IBinaryField field22 = node2.node.getField(2);
                assertThat(field22.getStore() == node1, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                ids[2] = node3.node.getId();
                ITextField field31 = node3.node.getField(1);
                assertThat(field31.getStore() == node1, is(true));
                IBinaryField field32 = node3.node.getField(2);
                assertThat(field32.getStore() == node1, is(true));

                Writer writer1 = field21.createWriter();
                Writer writer2 = field31.createWriter();
                OutputStream out1 = field22.createOutputStream();
                OutputStream out2 = field32.createOutputStream();

                try {
                    for (int i = 0; i < 100; i++) {
                        ByteArray buf1 = createBuffer(12345, 567 + i);
                        ByteArray buf2 = createBuffer(17171, 123 + i);
                        String str1 = createString(12345, 567 + i);
                        String str2 = createString(17171, 123 + i);
                        out1.write(buf1.getBuffer(), buf1.getOffset(), buf1.getLength());
                        out2.write(buf2.getBuffer(), buf2.getOffset(), buf2.getLength());

                        writer1.write(str1);
                        writer2.write(str2);
                    }

                    writer1.close();
                    writer2.close();
                    out1.close();
                    out2.close();

                    Reader reader1 = field21.createReader();
                    Reader reader2 = field31.createReader();
                    InputStream in1 = field22.createInputStream();
                    InputStream in2 = field32.createInputStream();

                    for (int i = 0; i < 100; i++) {
                        ByteArray buf1 = createBuffer(12345, 567 + i);
                        ByteArray buf2 = createBuffer(17171, 123 + i);
                        String str1 = createString(12345, 567 + i);
                        String str2 = createString(17171, 123 + i);

                        read(reader1, str1);
                        read(reader2, str2);
                        read(in1, buf1);
                        read(in2, buf2);
                    }
                    assertThat(reader1.read(new char[100]), is(-1));
                    assertThat(reader2.read(new char[100]), is(-1));

                    assertThat(in1.read(new byte[100]), is(-1));
                    assertThat(in2.read(new byte[100]), is(-1));
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        ((IObjectOperationManager) database.findExtension(IObjectOperationManager.NAME)).compact(null);

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                ObjectSpace space = (ObjectSpace) dataSchema.getSpace();
                INodeIndex<Integer, TestNode> index1 = space.getIndex(dataSchema.findNode("node1").findField("field1"));
                INodeIndex<Integer, TestNode> index2 = space.getIndex(dataSchema.findNode("node2").findField("field1"));

                ids[0] = index1.find(1).node.getId();
                ids[1] = index2.find(2).node.getId();
                ids[2] = index2.find(3).node.getId();
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

                TestNode node2 = space.findNodeById(ids[1]);
                ITextField field21 = node2.node.getField(1);
                assertThat(field21.getStore() == node1, is(true));
                IBinaryField field22 = node2.node.getField(2);
                assertThat(field22.getStore() == node1, is(true));

                TestNode node3 = space.findNodeById(ids[2]);
                ITextField field31 = node3.node.getField(1);
                assertThat(field31.getStore() == node1, is(true));
                IBinaryField field32 = node3.node.getField(2);
                assertThat(field32.getStore() == node1, is(true));

                Reader reader1 = field21.createReader();
                Reader reader2 = field31.createReader();
                InputStream in1 = field22.createInputStream();
                InputStream in2 = field32.createInputStream();

                try {
                    for (int i = 0; i < 100; i++) {
                        ByteArray buf1 = createBuffer(12345, 567 + i);
                        ByteArray buf2 = createBuffer(17171, 123 + i);
                        String str1 = createString(12345, 567 + i);
                        String str2 = createString(17171, 123 + i);

                        read(reader1, str1);
                        read(reader2, str2);
                        read(in1, buf1);
                        read(in2, buf2);
                    }

                    assertThat(reader1.read(new char[100]), is(-1));
                    assertThat(reader2.read(new char[100]), is(-1));

                    assertThat(in1.read(new byte[100]), is(-1));
                    assertThat(in2.read(new byte[100]), is(-1));
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
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
