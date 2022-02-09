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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.index.HashIndexStatistics;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.TreeIndexStatistics;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.index.AbstractIndexSpace;
import com.exametrika.impl.exadb.index.NonUniqueSortedIndex;
import com.exametrika.impl.exadb.index.btree.BTreeIndexSpace;
import com.exametrika.impl.exadb.index.memory.HashIndexSpace;
import com.exametrika.impl.exadb.index.memory.HashIndexSpace.CompactionInfo;
import com.exametrika.impl.exadb.index.memory.TreeIndexSpace;
import com.exametrika.spi.exadb.index.IIndexDatabaseExtension;


/**
 * The {@link IndexTests} are tests for various tree index implementations.
 *
 * @author Medvedev-A
 */
public class IndexTests {
    private Database database;
    private DatabaseConfiguration configuration;
    private IDatabaseFactory.Parameters parameters;
    private DatabaseConfigurationBuilder builder;
    private IIndexManager indexManager;

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

        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testTreeIndexTransactions() {
        testIndexTransactions(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            256, false, 256, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testHashIndexTransactions() {
        testIndexTransactions(new ITestIndexFactory() {
            @Override
            public IUniqueIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new HashIndexSchemaConfiguration("hash", 0, false,
                            256, false, 256, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testBTreeIndexAddVariableKeyFixedValue() {
        testAddVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testBTreeIndexAddVariableKeyVariableValue() {
        testAddVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, false, (14), new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testBTreeIndexReplaceValue() {
        testReplaceValue(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testTreeIndexAddVariableKeyFixedValue() {
        testAddVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testTreeIndexAddVariableKeyVariableValue() {
        testAddVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testTreeIndexReplaceValue() {
        testReplaceValue(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testTreeIndexCompaction() {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                TreeIndexSpace<String, ByteArray> space = indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                        16, false, 16, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        true, true, java.util.Collections.<String, String>emptyMap()));

                TestBatchControl batchControl = new TestBatchControl();
                assertThat(space.compact(batchControl, null, 1, false), nullValue());

                for (int i = 0; i < 100; i++)
                    space.add("test" + i, createBuffer(i, 4));
                for (int i = 0; i < 50; i++)
                    space.remove("test" + i);

                space.onTransactionCommitted();

                batchControl = new TestBatchControl();

                Pair<ByteArray, TreeIndexSpace<String, ByteArray>> start = null;
                int k = 0;
                while (true) {
                    start = space.compact(batchControl, start, 60, false);
                    k++;
                    if (start.getKey() == null)
                        break;
                }

                assertThat(k, is(25));
                assertThat(start.getValue().getCount(), is(50l));

                for (int i = 50; i < 100; i++)
                    assertThat(start.getValue().find("test" + i), is(createBuffer(i, 4)));
            }
        });
    }

    @Test
    public void testHashIndexCompaction() {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                HashIndexSpace<String, ByteArray> space = indexManager.createIndex("test", new HashIndexSchemaConfiguration("hash", 0, false,
                        16, false, 16, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        java.util.Collections.<String, String>emptyMap()));
                ;

                TestBatchControl batchControl = new TestBatchControl();
                assertThat(space.compact(batchControl, null, 1, false), nullValue());

                for (int i = 0; i < 100; i++)
                    space.add("test" + i, createBuffer(i, 4));
                for (int i = 0; i < 50; i++)
                    space.remove("test" + i);

                space.onTransactionCommitted();

                batchControl = new TestBatchControl();

                CompactionInfo start = null;
                int k = 0;
                while (true) {
                    start = space.compact(batchControl, start, 60, false);
                    k++;
                    if (k == 10) {
                        start.index.unload(true);
                        start = new CompactionInfo(null, start.fileIndex, start.key, null);
                    }

                    if (start.key == null)
                        break;
                }

                assertThat(k, is(17));
                assertThat(start.index.getCount(), is(50l));

                for (int i = 50; i < 100; i++)
                    assertThat(start.index.find("test" + i), is((Object) createBuffer(i, 4)));
            }
        });
    }

    @Test
    public void testBTreePageAllocation() {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                        16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()));
                ;

                IRawPage page1 = space.allocatePage();
                IRawPage page2 = space.allocatePage();
                assertThat(page1.getIndex(), is(1l));
                assertThat(page2.getIndex(), is(2l));
                space.freePage(page1);
                space.freePage(page2);

                page1 = space.allocatePage();
                page2 = space.allocatePage();
                assertThat(page1.getIndex(), is(2l));
                assertThat(page2.getIndex(), is(1l));

                assertThat(space.allocatePage().getIndex(), is(3l));
            }
        });
    }

    @Test
    public void testBTreeIndexRemoveVariableKeyFixedValue() {
        testRemoveVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testBTreeIndexRemoveVariableKeyVariableValue() {
        testRemoveVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testTreeIndexRemoveVariableKeyFixedValue() {
        testRemoveVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testTreeIndexRemoveVariableKeyVAriableValue() {
        testRemoveVariableKeyIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testAddFixedKeyIndex() {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, true,
                        10, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()));
                ;
                space.assertValid();

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
                assertThat(space.find(createBuffer(0, 100)), nullValue());
                assertThat(space.find(createBuffer(0, 100), true, createBuffer(1, 100), true).iterator().hasNext(), is(false));

                for (int i = 99; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 10);
                    ByteArray value = createBuffer(2 * i, 4);
                    space.add(key, value);
                    space.assertValid();

                    assertThat(space.find(key), is(value));
                }

                for (int i = 0; i < 100; i++) {
                    ByteArray key = createBuffer(i, 10);
                    ByteArray value = createBuffer(2 * i, 4);
                    assertThat(space.find(key), is(value));
                }

                checkFixedElement(space.findFirst(), 0);
                checkFixedElement(space.findLast(), 99);

                int i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                    checkFixedElement(pair, i++);
                assertThat(i, is(100));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, false, null, false))
                    checkFixedElement(pair, 1 + i++);
                assertThat(i, is(98));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 10), true, createBuffer(14, 10), true))
                    checkFixedElement(pair, 7 + i++);
                assertThat(i, is(8));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 10), false, createBuffer(14, 10), false))
                    checkFixedElement(pair, 8 + i++);
                assertThat(i, is(6));

                byte[] buffer1 = new byte[10];
                buffer1[0] = 7;
                byte[] buffer2 = new byte[10];
                buffer2[0] = 14;

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), true, new ByteArray(buffer2), true))
                    checkFixedElement(pair, 7 + i++);
                assertThat(i, is(7));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), false, new ByteArray(buffer2), false))
                    checkFixedElement(pair, 7 + i++);
                assertThat(i, is(7));

                Pair<ByteArray, ByteArray> pair = space.findCeiling(null, true);
                checkFixedElement(pair, 0);
                assertThat(space.findCeilingValue(null, true), is(pair.getValue()));

                pair = space.findCeiling(null, false);
                checkFixedElement(pair, 1);
                assertThat(space.findCeilingValue(null, false), is(pair.getValue()));

                pair = space.findCeiling(createBuffer(7, 8 + (7 % 5)), true);
                checkFixedElement(pair, 7);
                assertThat(space.findCeilingValue(createBuffer(7, 8 + (7 % 5)), true), is(pair.getValue()));

                pair = space.findCeiling(createBuffer(7, 8 + (7 % 5)), false);
                checkFixedElement(pair, 8);
                assertThat(space.findCeilingValue(createBuffer(7, 8 + (7 % 5)), false), is(pair.getValue()));

                buffer1[0] = 0;
                pair = space.findCeiling(new ByteArray(buffer1), true);
                checkFixedElement(pair, 0);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findCeiling(new ByteArray(buffer1), false);
                checkFixedElement(pair, 0);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = 7;
                pair = space.findCeiling(new ByteArray(buffer1), true);
                checkFixedElement(pair, 7);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findCeiling(new ByteArray(buffer1), false);
                checkFixedElement(pair, 7);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = (byte) 255;
                assertThat(space.findCeiling(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), nullValue());

                assertThat(space.findCeiling(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), nullValue());

                pair = space.findFloor(null, true);
                checkFixedElement(pair, 99);
                assertThat(space.findFloorValue(null, true), is(pair.getValue()));

                pair = space.findFloor(null, false);
                checkFixedElement(pair, 98);
                assertThat(space.findFloorValue(null, false), is(pair.getValue()));

                pair = space.findFloor(createBuffer(7, 8 + (7 % 5)), true);
                checkFixedElement(pair, 7);
                assertThat(space.findFloorValue(createBuffer(7, 8 + (7 % 5)), true), is(pair.getValue()));

                pair = space.findFloor(createBuffer(7, 8 + (7 % 5)), false);
                checkFixedElement(pair, 6);
                assertThat(space.findFloorValue(createBuffer(7, 8 + (7 % 5)), false), is(pair.getValue()));

                buffer1[0] = (byte) 255;
                pair = space.findFloor(new ByteArray(buffer1), true);
                checkFixedElement(pair, 99);
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findFloor(new ByteArray(buffer1), false);
                checkFixedElement(pair, 99);
                assertThat(space.findFloorValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = 8;
                pair = space.findFloor(new ByteArray(buffer1), true);
                checkFixedElement(pair, 7);
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findFloor(new ByteArray(buffer1), false);
                checkFixedElement(pair, 7);
                assertThat(space.findFloorValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = (byte) 0;
                assertThat(space.findFloor(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), nullValue());

                assertThat(space.findFloor(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), nullValue());
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);
                space.assertValid();

                for (int i = 0; i < 100; i++) {
                    ByteArray key = createBuffer(i, 10);
                    ByteArray value = createBuffer(2 * i, 4);
                    assertThat(space.find(key), is(value));
                }

                checkFixedElement(space.findFirst(), 0);
                checkFixedElement(space.findLast(), 99);

                int i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                    checkFixedElement(pair, i++);
                assertThat(i, is(100));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, false, null, false))
                    checkFixedElement(pair, 1 + i++);
                assertThat(i, is(98));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 10), true, createBuffer(14, 10), true))
                    checkFixedElement(pair, 7 + i++);
                assertThat(i, is(8));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 10), false, createBuffer(14, 10), false))
                    checkFixedElement(pair, 8 + i++);
                assertThat(i, is(6));

                byte[] buffer1 = new byte[10];
                buffer1[0] = 7;
                byte[] buffer2 = new byte[10];
                buffer2[0] = 14;

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), true, new ByteArray(buffer2), true))
                    checkFixedElement(pair, 7 + i++);
                assertThat(i, is(7));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), false, new ByteArray(buffer2), false))
                    checkFixedElement(pair, 7 + i++);
                assertThat(i, is(7));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);
                space.clear();
                space.assertValid();

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
                assertThat(space.find(createBuffer(0, 100)), nullValue());
                assertThat(space.find(createBuffer(0, 100), true, createBuffer(1, 100), true).iterator().hasNext(), is(false));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);

                IRawPage page1 = space.allocatePage();
                IRawPage page2 = space.allocatePage();
                assertThat(page1.getIndex(), is(1l));
                assertThat(page2.getIndex(), is(2l));
                space.freePage(page1);
                space.freePage(page2);

                page1 = space.allocatePage();
                page2 = space.allocatePage();
                assertThat(page1.getIndex(), is(2l));
                assertThat(page2.getIndex(), is(1l));

                assertThat(space.allocatePage().getIndex(), is(3l));
            }
        });
    }

    @Test
    public void testRemoveFixedKeyIndex() {
        final int COUNT = 100;
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, true,
                        10, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()));

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 10);
                    ByteArray value = createBuffer(2 * i, 4);
                    space.add(key, value);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);
                space.assertValid();

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 10);
                    space.remove(key);
                    space.assertValid();

                    int k = 0;
                    for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                        checkFixedElement(pair, k++);
                    assertThat(k, is(i));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);
                space.assertValid();

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 10);
                    ByteArray value = createBuffer(2 * i, 4);
                    space.add(key, value);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);
                space.assertValid();

                for (int i = 0; i < COUNT; i++) {
                    ByteArray key = createBuffer(i, 10);
                    space.remove(key);
                    space.assertValid();

                    int k = i + 1;
                    for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                        checkFixedElement(pair, k++);
                    assertThat(k, is(COUNT));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
            }
        });
    }

    @Test
    public void testBTreeIndexAddVariableKeyFixedValueRandom() {
        testAddVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testBTreeIndexAddVariableKeyVariableValueRandom() {
        testAddVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testBTreeIndexReplaceValueRandom() {
        testReplaceRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testTreeIndexAddVariableKeyFixedValueRandom() {
        testAddVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testTreeIndexAddVariableKeyVariableValueRandom() {
        testAddVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testBTreeIndexBulkAddVariableRandom() {
        testBulkAddVariableRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testTreeIndexBulkAddVariableRandom() {
        testBulkAddVariableRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testBTreeIndexRemoveVariableKeyFixedValueRandom() {
        testRemoveVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testBTreeIndexRemoveVariableKeyVariableValueRandom() {
        testRemoveVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testTreeIndexRemoveVariableKeyFixedValueRandom() {
        testRemoveVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, true);
    }

    @Test
    public void testTreeIndexRemoveVariableKeyVariableValueRandom() {
        testRemoveVariableKeyRandomIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        }, false);
    }

    @Test
    public void testAddFixedRandomIndex() {
        Random random = new TestRandom();
        final Map<ByteArray, Integer> buffers = new HashMap<ByteArray, Integer>();
        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[10];
            random.nextBytes(buffer);
            buffers.put(new ByteArray(buffer), i);
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, true,
                        10, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()));

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    space.add(entry.getKey(), value);
                    space.assertValid();
                }

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    assertThat(space.find(entry.getKey()), is(value));
                }

                int i = 0;
                ByteArray prev = null;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                    if (prev != null)
                        assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                    i++;
                    prev = pair.getKey();
                }
                assertThat(i, is(buffers.size()));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.getIndex(10);
                space.assertValid();

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    assertThat(space.find(entry.getKey()), is(value));
                }

                int i = 0;
                ByteArray prev = null;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                    if (prev != null)
                        assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                    i++;
                    prev = pair.getKey();
                }
                assertThat(i, is(buffers.size()));
            }
        });
    }

    @Test
    public void testRemoveFixedRandomIndex() {
        final Random random = new TestRandom();
        final Map<ByteArray, Integer> buffers = new HashMap<ByteArray, Integer>();
        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[10];
            random.nextBytes(buffer);
            buffers.put(new ByteArray(buffer), i);
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, true,
                        10, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()));
                ;

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    space.add(entry.getKey(), value);
                }

                space.assertValid();

                List<ByteArray> list = new LinkedList<ByteArray>(buffers.keySet());
                while (!list.isEmpty()) {
                    ByteArray key = list.remove(random.nextInt(list.size()));
                    space.remove(key);
                    space.assertValid();
                    assertThat(space.find(key), nullValue());

                    int i = 0;
                    ByteArray prev = null;
                    for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                        if (prev != null)
                            assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                        i++;
                        prev = pair.getKey();
                    }
                    assertThat(i, is(list.size()));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
            }
        });
    }

    @Test
    public void testBTreeIndexNonUnique() {
        testNonUniqueIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            256, true, 4, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            false, false, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testTreeIndexNonUnique() {
        testNonUniqueIndex(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            256, true, 4, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, false, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testBTreeIndexStatistics() {
        testStatistics(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testTreeIndexStatistics() {
        testStatistics(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, false, 16, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, true, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testNonUniqueBTreeIndexStatistics() {
        testNonUniqueIndexStatistics(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                            16, true, 4, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, false, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }

    @Test
    public void testNonUniqueTreeIndexStatistics() {
        testNonUniqueIndexStatistics(new ITestIndexFactory() {
            @Override
            public ISortedIndex create(ITransaction transaction, boolean open) {
                if (!open)
                    return indexManager.createIndex("test", new TreeIndexSchemaConfiguration("tree", 0, false,
                            16, true, 4, new StringKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                            true, false, java.util.Collections.<String, String>emptyMap()));
                else
                    return indexManager.getIndex(10);
            }
        });
    }


    private void testRemoveVariableKeyIndex(final ITestIndexFactory factory, final boolean fixedValue) {
        final int COUNT = 100;
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value;
                    if (fixedValue)
                        value = createBuffer(2 * i, 4);
                    else
                        value = createBuffer(i, 7 + (i % 4));
                    space.add(key, value);
                }

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                ((AbstractIndexSpace) space).assertValid();

                assertThat(space.isEmpty(), is(false));
                assertThat((int) space.getCount(), is(COUNT));

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    space.remove(key);
                    ((AbstractIndexSpace) space).assertValid();

                    int k = 0;
                    for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                        checkVarElement(pair, k++, fixedValue);
                    assertThat(k, is(i));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());

                assertThat(space.isEmpty(), is(true));
                assertThat(space.getCount(), is(0l));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                ((AbstractIndexSpace) space).assertValid();

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value;
                    if (fixedValue)
                        value = createBuffer(2 * i, 4);
                    else
                        value = createBuffer(i, 7 + (i % 4));
                    space.add(key, value);
                }

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                ((AbstractIndexSpace) space).assertValid();

                for (int i = 0; i < COUNT; i++) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    space.remove(key);
                    ((AbstractIndexSpace) space).assertValid();

                    int k = i + 1;
                    for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                        checkVarElement(pair, k++, fixedValue);
                    assertThat(k, is(COUNT));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });
    }

    private void testAddVariableKeyIndex(final ITestIndexFactory factory, final boolean fixedValue) {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                ((AbstractIndexSpace) space).assertValid();
                assertThat(space.isEmpty(), is(true));
                assertThat(space.getCount(), is(0l));
                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
                assertThat(space.find(createBuffer(0, 100)), nullValue());
                assertThat(space.find(createBuffer(0, 100), true, createBuffer(1, 100), true).iterator().hasNext(), is(false));

                for (int i = 99; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value;
                    if (fixedValue)
                        value = createBuffer(2 * i, 4);
                    else
                        value = createBuffer(i, 7 + (i % 4));
                    space.add(key, value);
                    ((AbstractIndexSpace) space).assertValid();

                    assertThat(space.find(key), is(value));
                }

                assertThat(space.isEmpty(), is(false));
                assertThat(space.getCount(), is(100l));

                for (int i = 0; i < 100; i++) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value;
                    if (fixedValue)
                        value = createBuffer(2 * i, 4);
                    else
                        value = createBuffer(i, 7 + (i % 4));
                    assertThat(space.find(key), is(value));
                }

                checkVarElement(space.findFirst(), 0, fixedValue);
                checkVarElement(space.findLast(), 99, fixedValue);

                int i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                    checkVarElement(pair, i++, fixedValue);
                assertThat(i, is(100));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, false, null, false))
                    checkVarElement(pair, 1 + i++, fixedValue);
                assertThat(i, is(98));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 8 + (7 % 5)), true, createBuffer(14, 8 + (14 % 5)), true))
                    checkVarElement(pair, 7 + i++, fixedValue);
                assertThat(i, is(8));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 8 + (7 % 5)), false, createBuffer(14, 8 + (14 % 5)), false))
                    checkVarElement(pair, 8 + i++, fixedValue);
                assertThat(i, is(6));

                byte[] buffer1 = new byte[100];
                buffer1[0] = 7;
                byte[] buffer2 = new byte[100];
                buffer2[0] = 14;

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), true, new ByteArray(buffer2), true))
                    checkVarElement(pair, 7 + i++, fixedValue);
                assertThat(i, is(7));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), false, new ByteArray(buffer2), false))
                    checkVarElement(pair, 7 + i++, fixedValue);
                assertThat(i, is(7));

                Pair<ByteArray, ByteArray> pair = space.findCeiling(null, true);
                checkVarElement(pair, 0, fixedValue);
                assertThat(space.findCeilingValue(null, true), is(pair.getValue()));

                pair = space.findCeiling(null, false);
                checkVarElement(pair, 1, fixedValue);
                assertThat(space.findCeilingValue(null, false), is(pair.getValue()));

                pair = space.findCeiling(createBuffer(7, 8 + (7 % 5)), true);
                checkVarElement(pair, 7, fixedValue);
                assertThat(space.findCeilingValue(createBuffer(7, 8 + (7 % 5)), true), is(pair.getValue()));

                pair = space.findCeiling(createBuffer(7, 8 + (7 % 5)), false);
                checkVarElement(pair, 8, fixedValue);
                assertThat(space.findCeilingValue(createBuffer(7, 8 + (7 % 5)), false), is(pair.getValue()));

                buffer1[0] = 0;
                pair = space.findCeiling(new ByteArray(buffer1), true);
                checkVarElement(pair, 0, fixedValue);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findCeiling(new ByteArray(buffer1), false);
                checkVarElement(pair, 0, fixedValue);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = 7;
                pair = space.findCeiling(new ByteArray(buffer1), true);
                checkVarElement(pair, 7, fixedValue);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findCeiling(new ByteArray(buffer1), false);
                checkVarElement(pair, 7, fixedValue);
                assertThat(space.findCeilingValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = (byte) 255;
                assertThat(space.findCeiling(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), nullValue());

                assertThat(space.findCeiling(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findCeilingValue(new ByteArray(buffer1), true), nullValue());

                pair = space.findFloor(null, true);
                checkVarElement(pair, 99, fixedValue);
                assertThat(space.findFloorValue(null, true), is(pair.getValue()));

                pair = space.findFloor(null, false);
                checkVarElement(pair, 98, fixedValue);
                assertThat(space.findFloorValue(null, false), is(pair.getValue()));

                pair = space.findFloor(createBuffer(7, 8 + (7 % 5)), true);
                checkVarElement(pair, 7, fixedValue);
                assertThat(space.findFloorValue(createBuffer(7, 8 + (7 % 5)), true), is(pair.getValue()));

                pair = space.findFloor(createBuffer(7, 8 + (7 % 5)), false);
                checkVarElement(pair, 6, fixedValue);
                assertThat(space.findFloorValue(createBuffer(7, 8 + (7 % 5)), false), is(pair.getValue()));

                buffer1[0] = (byte) 255;
                pair = space.findFloor(new ByteArray(buffer1), true);
                checkVarElement(pair, 99, fixedValue);
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findFloor(new ByteArray(buffer1), false);
                checkVarElement(pair, 99, fixedValue);
                assertThat(space.findFloorValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = 8;
                pair = space.findFloor(new ByteArray(buffer1), true);
                checkVarElement(pair, 7, fixedValue);
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), is(pair.getValue()));

                pair = space.findFloor(new ByteArray(buffer1), false);
                checkVarElement(pair, 7, fixedValue);
                assertThat(space.findFloorValue(new ByteArray(buffer1), false), is(pair.getValue()));

                buffer1[0] = (byte) 0;
                assertThat(space.findFloor(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), nullValue());

                assertThat(space.findFloor(new ByteArray(buffer1), true), nullValue());
                assertThat(space.findFloorValue(new ByteArray(buffer1), true), nullValue());

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                ((AbstractIndexSpace) space).assertValid();

                for (int i = 0; i < 100; i++) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value;
                    if (fixedValue)
                        value = createBuffer(2 * i, 4);
                    else
                        value = createBuffer(i, 7 + (i % 4));
                    assertThat(space.find(key), is(value));
                }

                checkVarElement(space.findFirst(), 0, fixedValue);
                checkVarElement(space.findLast(), 99, fixedValue);

                int i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                    checkVarElement(pair, i++, fixedValue);
                assertThat(i, is(100));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, false, null, false))
                    checkVarElement(pair, 1 + i++, fixedValue);
                assertThat(i, is(98));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 8 + (7 % 5)), true, createBuffer(14, 8 + (14 % 5)), true))
                    checkVarElement(pair, 7 + i++, fixedValue);
                assertThat(i, is(8));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(createBuffer(7, 8 + (7 % 5)), false, createBuffer(14, 8 + (14 % 5)), false))
                    checkVarElement(pair, 8 + i++, fixedValue);
                assertThat(i, is(6));

                byte[] buffer1 = new byte[100];
                buffer1[0] = 7;
                byte[] buffer2 = new byte[100];
                buffer2[0] = 14;

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), true, new ByteArray(buffer2), true))
                    checkVarElement(pair, 7 + i++, fixedValue);
                assertThat(i, is(7));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(new ByteArray(buffer1), false, new ByteArray(buffer2), false))
                    checkVarElement(pair, 7 + i++, fixedValue);
                assertThat(i, is(7));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                space.clear();
                ((AbstractIndexSpace) space).assertValid();

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
                assertThat(space.find(createBuffer(0, 100)), nullValue());
                assertThat(space.find(createBuffer(0, 100), true, createBuffer(1, 100), true).iterator().hasNext(), is(false));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });
    }

    private void testReplaceValue(final ITestIndexFactory factory) {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                for (int i = 99; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value = createBuffer(2 * i, 4);
                    space.add(key, value);
                    ((AbstractIndexSpace) space).assertValid();

                    assertThat(space.find(key), is(value));
                }

                for (int i = 99; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value = createBuffer(2 * i, 12);
                    space.add(key, value);
                    ((AbstractIndexSpace) space).assertValid();

                    assertThat(space.find(key), is(value));
                }

                for (int i = 99; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value = createBuffer(2 * i, 0);
                    space.add(key, value);
                    ((AbstractIndexSpace) space).assertValid();

                    assertThat(space.find(key), is(value));
                }
            }
        });
    }

    private void testNonUniqueIndexStatistics(final ITestIndexFactory factory) {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                NonUniqueSortedIndex<String, ByteArray> space = factory.create(transaction, false);

                List<Pair<String, ByteArray>> elements = new ArrayList<Pair<String, ByteArray>>();
                for (int i = 0; i < 99; i++) {
                    String key = createString(i / 3, 1 + ((i / 3) % 5));
                    ByteArray value = createBuffer(i, 4);
                    elements.add(new Pair<String, ByteArray>(key, value));
                }

                java.util.Collections.sort(elements, new Comparator<Pair<String, ByteArray>>() {
                    @Override
                    public int compare(Pair<String, ByteArray> o1, Pair<String, ByteArray> o2) {
                        int res = o1.getKey().compareTo(o2.getKey());
                        if (res != 0)
                            return res;
                        return o1.getValue().compareTo(o2.getValue());
                    }
                });

                for (int i = elements.size() - 1; i >= 0; i--) {
                    Pair<String, ByteArray> pair = elements.get(i);
                    space.add(pair.getKey(), pair.getValue());
                    ((AbstractIndexSpace) space.getIndex()).assertValid();
                }

                ((AbstractIndexSpace) space).onTransactionCommitted();

                assertThat(space.estimate(null, true, null, true), is(0l));

                TestBatchControl batchControl = new TestBatchControl();

                Pair<ByteArray, Long> startBin = null;

                int i = 0;
                while (true) {
                    startBin = space.rebuildStatistics(batchControl, startBin, 10, 1, false);
                    i++;
                    if (startBin == null)
                        break;
                }

                assertThat(i, is(5));

                assertThat((int) space.estimate(null, true, null, true), is(98));

                batchControl.count = 2;
                assertThat(space.rebuildStatistics(batchControl, startBin, 10, 1, false), nullValue());
                i = 0;
                while (true) {
                    startBin = space.rebuildStatistics(batchControl, startBin, 10, 1, i == 0);
                    i++;
                    if (startBin == null)
                        break;
                }

                assertThat(i, is(5));

                i = 100;
                String key = createString(i / 3, 1 + ((i / 3) % 5));
                ByteArray value = createBuffer(i, 4);
                space.add(key, value);

                ((AbstractIndexSpace) space).onTransactionCommitted();

                startBin = null;
                batchControl.count = 2;
                i = 0;
                while (true) {
                    startBin = space.rebuildStatistics(batchControl, startBin, 10, 1, false);
                    i++;
                    if (startBin == null)
                        break;
                }

                assertThat(i, is(5));

                assertThat((int) space.estimate(null, true, null, true), is(99));
                assertThat((int) space.estimate(null, false, null, false), is(80));
                assertThat((int) space.estimate("", true, "\u65535", true), is(99));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });
    }

    private void testAddVariableKeyRandomIndex(final ITestIndexFactory factory, final boolean fixedValue) {
        Random random = new TestRandom();
        final Map<ByteArray, Pair<Integer, Integer>> buffers = new HashMap<ByteArray, Pair<Integer, Integer>>();
        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[random.nextInt(15) + 1];
            random.nextBytes(buffer);
            int size = fixedValue ? 4 : random.nextInt(10);
            buffers.put(new ByteArray(buffer), new Pair<Integer, Integer>(i, size));
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    space.add(entry.getKey(), value);
                    ((AbstractIndexSpace) space).assertValid();
                }

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    assertThat(space.find(entry.getKey()), is(value));
                }

                int i = 0;
                ByteArray prev = null;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                    if (prev != null)
                        assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                    i++;
                    prev = pair.getKey();
                }
                assertThat(i, is(buffers.size()));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                ((AbstractIndexSpace) space).assertValid();

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    assertThat(space.find(entry.getKey()), is(value));
                }

                int i = 0;
                ByteArray prev = null;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                    if (prev != null)
                        assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                    i++;
                    prev = pair.getKey();
                }
                assertThat(i, is(buffers.size()));
            }
        });
    }

    private void testReplaceRandomIndex(final ITestIndexFactory factory) {
        Random random = new TestRandom();
        final Map<ByteArray, Pair<Integer, Integer>> buffers1 = new HashMap<ByteArray, Pair<Integer, Integer>>();
        final Map<ByteArray, Pair<Integer, Integer>> buffers2 = new HashMap<ByteArray, Pair<Integer, Integer>>();
        final Map<ByteArray, Pair<Integer, Integer>> buffers3 = new HashMap<ByteArray, Pair<Integer, Integer>>();
        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[random.nextInt(15) + 1];
            random.nextBytes(buffer);
            int size = random.nextInt(10);
            buffers1.put(new ByteArray(buffer), new Pair<Integer, Integer>(i, size));
        }

        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[random.nextInt(15) + 1];
            random.nextBytes(buffer);
            int size = random.nextInt(10);
            buffers2.put(new ByteArray(buffer), new Pair<Integer, Integer>(i, size));
        }

        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[random.nextInt(15) + 1];
            random.nextBytes(buffer);
            int size = random.nextInt(10);
            buffers3.put(new ByteArray(buffer), new Pair<Integer, Integer>(i, size));
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers1.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    space.add(entry.getKey(), value);
                    ((AbstractIndexSpace) space).assertValid();
                }

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers1.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    assertThat(space.find(entry.getKey()), is(value));
                }

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers2.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    space.add(entry.getKey(), value);
                    ((AbstractIndexSpace) space).assertValid();
                }

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers2.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    assertThat(space.find(entry.getKey()), is(value));
                }

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers3.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    space.add(entry.getKey(), value);
                    ((AbstractIndexSpace) space).assertValid();
                }

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers3.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    assertThat(space.find(entry.getKey()), is(value));
                }
            }
        });
    }

    private void testBulkAddVariableRandomIndex(final ITestIndexFactory factory) {
        Random random = new TestRandom();
        final Map<ByteArray, Integer> buffers = new HashMap<ByteArray, Integer>();
        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[random.nextInt(15) + 1];
            random.nextBytes(buffer);
            buffers.put(new ByteArray(buffer), i);
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                BTreeIndexSpace<ByteArray, ByteArray> space0 = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                        16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()));
                ;

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    space0.add(entry.getKey(), value);
                    space0.assertValid();
                }

                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                space.bulkAdd(space0.find(null, true, null, true));
                ((AbstractIndexSpace) space).assertValid();

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    assertThat(space.find(entry.getKey()), is(value));
                }

                int i = 0;
                ByteArray prev = null;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                    if (prev != null)
                        assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                    i++;
                    prev = pair.getKey();
                }
                assertThat(i, is(buffers.size()));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);
                ((AbstractIndexSpace) space).assertValid();

                for (Map.Entry<ByteArray, Integer> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue(), 4);
                    assertThat(space.find(entry.getKey()), is(value));
                }

                int i = 0;
                ByteArray prev = null;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                    if (prev != null)
                        assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                    i++;
                    prev = pair.getKey();
                }
                assertThat(i, is(buffers.size()));
            }
        });
    }

    private void testRemoveVariableKeyRandomIndex(final ITestIndexFactory factory, final boolean fixedValue) {
        final Random random = new TestRandom();

        final Map<ByteArray, Pair<Integer, Integer>> buffers = new HashMap<ByteArray, Pair<Integer, Integer>>();
        for (int i = 0; i < 10000; i++) {
            byte[] buffer = new byte[random.nextInt(15) + 1];
            random.nextBytes(buffer);
            int size = fixedValue ? 4 : random.nextInt(10);
            buffers.put(new ByteArray(buffer), new Pair<Integer, Integer>(i, size));
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                for (Map.Entry<ByteArray, Pair<Integer, Integer>> entry : buffers.entrySet()) {
                    ByteArray value = createBuffer(entry.getValue().getKey(), entry.getValue().getValue());
                    space.add(entry.getKey(), value);
                }

                ((AbstractIndexSpace) space).assertValid();

                List<ByteArray> list = new LinkedList<ByteArray>(buffers.keySet());
                while (!list.isEmpty()) {
                    ByteArray key = list.remove(random.nextInt(list.size()));
                    space.remove(key);
                    ((AbstractIndexSpace) space).assertValid();
                    assertThat(space.find(key), nullValue());

                    int i = 0;
                    ByteArray prev = null;
                    for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true)) {
                        if (prev != null)
                            assertThat(prev.compareTo(pair.getKey()) < 0, is(true));
                        i++;
                        prev = pair.getKey();
                    }
                    assertThat(i, is(list.size()));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });
    }

    private void testNonUniqueIndex(final ITestIndexFactory factory) {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                NonUniqueSortedIndex<String, ByteArray> space = factory.create(transaction, false);

                List<Pair<String, ByteArray>> elements = new ArrayList<Pair<String, ByteArray>>();
                for (int i = 0; i < 99; i++) {
                    String key = createString(i / 3, 1 + ((i / 3) % 5));
                    ByteArray value = createBuffer(i, 4);
                    elements.add(new Pair<String, ByteArray>(key, value));
                }

                java.util.Collections.sort(elements, new Comparator<Pair<String, ByteArray>>() {
                    @Override
                    public int compare(Pair<String, ByteArray> o1, Pair<String, ByteArray> o2) {
                        int res = o1.getKey().compareTo(o2.getKey());
                        if (res != 0)
                            return res;
                        return o1.getValue().compareTo(o2.getValue());
                    }
                });

                ((AbstractIndexSpace) space.getIndex()).assertValid();
                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());
                assertThat(space.find(createString(0, 100)), nullValue());
                assertThat(space.findValues(createString(0, 100)).iterator().hasNext(), is(false));
                assertThat(space.find(createString(0, 100), true, createString(1, 100), true).iterator().hasNext(), is(false));

                for (int i = elements.size() - 1; i >= 0; i--) {
                    Pair<String, ByteArray> pair = elements.get(i);
                    space.add(pair.getKey(), pair.getValue());
                    ((AbstractIndexSpace) space.getIndex()).assertValid();
                }

                assertThat(space.isEmpty(), is(false));
                assertThat((int) space.getCount(), is(elements.size()));

                NonUniqueSortedIndex<ByteArray, ByteArray> space1 = indexManager.createIndex("test", new BTreeIndexSchemaConfiguration("btree", 0, false,
                        16, true, 4, new ByteArrayKeyNormalizerSchemaConfiguration(), new ByteArrayValueConverterSchemaConfiguration(),
                        false, false, java.util.Collections.<String, String>emptyMap()));
                space1.bulkAdd(space.find(null, true, null, true));
                ((AbstractIndexSpace) space1.getIndex()).assertValid();

                for (int i = 0; i < 33; i++) {
                    Pair<String, ByteArray> pair = elements.get(i * 3);
                    assertThat(space.find(pair.getKey()), is(pair.getValue()));
                }

                for (int i = 0; i < 33; i++) {
                    Pair<String, ByteArray> pair = elements.get(i * 3);
                    List<ByteArray> values = Collections.toList(space.findValues(pair.getKey()).iterator());
                    assertThat(values, is(Arrays.asList(elements.get(i * 3).getValue(), elements.get(i * 3 + 1).getValue(),
                            elements.get(i * 3 + 2).getValue())));
                }

                assertThat(space.findFirst(), is(normalize(space, elements.get(0))));
                assertThat(space.findLast(), is(normalize(space, elements.get(98))));

                int i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, true, null, true))
                    assertThat(pair, is(normalize(space, elements.get(i++))));
                assertThat(i, is(99));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(null, false, null, false))
                    assertThat(pair, is(normalize(space, elements.get(1 + i++))));
                assertThat(i, is(97));

                i = 0;
                String key1 = elements.get(6).getKey();
                String key2 = elements.get(13).getKey();
                for (Pair<ByteArray, ByteArray> pair : space.find(key1, true, key2, true))
                    assertThat(pair, is(normalize(space, elements.get(6 + i++))));
                assertThat(i, is(9));

                i = 0;
                for (Pair<ByteArray, ByteArray> pair : space.find(key1, false, key2, false))
                    assertThat(pair, is(normalize(space, elements.get(9 + i++))));
                assertThat(i, is(3));

                Pair<ByteArray, ByteArray> pair = space.findCeiling(null, true);
                assertThat(pair, is(normalize(space, elements.get(0))));
                assertThat(space.findCeilingValue(null, true), is(pair.getValue()));

                pair = space.findCeiling(null, false);
                assertThat(pair, is(normalize(space, elements.get(1))));
                assertThat(space.findCeilingValue(null, false), is(pair.getValue()));

                pair = space.findCeiling(elements.get(7).getKey(), true);
                assertThat(pair, is(normalize(space, elements.get(6))));
                assertThat(space.findCeilingValue(elements.get(6).getKey(), true), is(pair.getValue()));

                pair = space.findCeiling(elements.get(7).getKey(), false);
                assertThat(pair, is(normalize(space, elements.get(9))));
                assertThat(space.findCeilingValue(elements.get(7).getKey(), false), is(pair.getValue()));

                pair = space.findCeiling("", true);
                assertThat(pair, is(normalize(space, elements.get(0))));
                assertThat(space.findCeilingValue("", true), is(pair.getValue()));

                pair = space.findCeiling("", false);
                assertThat(pair, is(normalize(space, elements.get(0))));
                assertThat(space.findCeilingValue("", false), is(pair.getValue()));

                String buffer1 = elements.get(7).getKey().substring(0, elements.get(7).getKey().length() - 1);
                pair = space.findCeiling(buffer1, true);
                assertThat(pair, is(normalize(space, elements.get(6))));
                assertThat(space.findCeilingValue(buffer1, true), is(pair.getValue()));

                pair = space.findCeiling(buffer1, false);
                assertThat(pair, is(normalize(space, elements.get(6))));
                assertThat(space.findCeilingValue(buffer1, false), is(pair.getValue()));

                buffer1 = "\u65535";
                assertThat(space.findCeiling(buffer1, true), nullValue());
                assertThat(space.findCeilingValue(buffer1, true), nullValue());

                assertThat(space.findCeiling(buffer1, true), nullValue());
                assertThat(space.findCeilingValue(buffer1, true), nullValue());

                pair = space.findFloor(null, true);
                assertThat(pair, is(normalize(space, elements.get(98))));
                assertThat(space.findFloorValue(null, true), is(pair.getValue()));

                pair = space.findFloor(null, false);
                assertThat(pair, is(normalize(space, elements.get(97))));
                assertThat(space.findFloorValue(null, false), is(pair.getValue()));

                pair = space.findFloor(elements.get(7).getKey(), true);
                assertThat(pair, is(normalize(space, elements.get(8))));
                assertThat(space.findFloorValue(elements.get(7).getKey(), true), is(pair.getValue()));

                pair = space.findFloor(elements.get(7).getKey(), false);
                assertThat(pair, is(normalize(space, elements.get(5))));
                assertThat(space.findFloorValue(elements.get(7).getKey(), false), is(pair.getValue()));

                buffer1 = "\u65535";
                pair = space.findFloor(buffer1, true);
                assertThat(pair, is(normalize(space, elements.get(98))));
                assertThat(space.findFloorValue(buffer1, true), is(pair.getValue()));

                pair = space.findFloor(buffer1, false);
                assertThat(pair, is(normalize(space, elements.get(98))));
                assertThat(space.findFloorValue(buffer1, false), is(pair.getValue()));

                buffer1 = elements.get(8).getKey().substring(0, elements.get(8).getKey().length() - 1);
                pair = space.findFloor(buffer1, true);
                assertThat(pair, is(normalize(space, elements.get(5))));
                assertThat(space.findFloorValue(buffer1, true), is(pair.getValue()));

                pair = space.findFloor(buffer1, false);
                assertThat(pair, is(normalize(space, elements.get(5))));
                assertThat(space.findFloorValue(buffer1, false), is(pair.getValue()));

                buffer1 = "";
                assertThat(space.findFloor(buffer1, true), nullValue());
                assertThat(space.findFloorValue(buffer1, true), nullValue());

                assertThat(space.findFloor(buffer1, true), nullValue());
                assertThat(space.findFloorValue(buffer1, true), nullValue());

                space.remove(elements.get(0).getKey());
                i = 0;
                for (Pair<ByteArray, ByteArray> pair1 : space.find(null, true, null, true))
                    assertThat(pair1, is(normalize(space, elements.get(3 + i++))));
                assertThat(i, is(96));

                for (i = 3; i < 99; i++) {
                    Pair<String, ByteArray> pair1 = elements.get(i);
                    space.remove(pair1.getKey(), pair1.getValue());
                    ((AbstractIndexSpace) space.getIndex()).assertValid();

                    int k = 0;
                    for (Pair<ByteArray, ByteArray> pair2 : space.find(null, true, null, true))
                        assertThat(pair2, is(normalize(space, elements.get(i + 1 + k++))));
                    assertThat(k, is(98 - i));
                }

                assertThat(space.findFirst(), nullValue());
                assertThat(space.findLast(), nullValue());

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });
    }

    private void testStatistics(final ITestIndexFactory factory) {
        final int COUNT = 100;
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, false);

                for (int i = COUNT - 1; i >= 0; i--) {
                    ByteArray key = createBuffer(i, 8 + (i % 5));
                    ByteArray value = createBuffer(2 * i, 4);
                    space.add(key, value);
                }

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISortedIndex<ByteArray, ByteArray> space = factory.create(transaction, true);

                assertThat(space.estimate(null, true, null, true), is(0l));

                TestBatchControl batchControl = new TestBatchControl();

                Pair<ByteArray, Long> startBin = null;

                int i = 0;
                while (true) {
                    startBin = space.rebuildStatistics(batchControl, startBin, 10, 1, false);
                    i++;
                    if (startBin == null)
                        break;
                }

                assertThat(i, is(5));

                assertThat((int) space.estimate(null, true, null, true), is(COUNT - 1));

                batchControl.count = 2;
                assertThat(space.rebuildStatistics(batchControl, startBin, 10, 1, false), nullValue());
                i = 0;
                while (true) {
                    startBin = space.rebuildStatistics(batchControl, startBin, 10, 1, i == 0);
                    i++;
                    if (startBin == null)
                        break;
                }

                assertThat(i, is(5));

                i = COUNT;
                ByteArray key = createBuffer(i, 8 + (i % 5));
                ByteArray value = createBuffer(2 * i, 4);
                space.add(key, value);

                ((AbstractIndexSpace) space).onTransactionCommitted();

                startBin = null;
                batchControl.count = 2;
                i = 0;
                while (true) {
                    startBin = space.rebuildStatistics(batchControl, startBin, 10, 1, false);
                    i++;
                    if (startBin == null)
                        break;
                }

                assertThat(i, is(5));

                assertThat((int) space.estimate(null, true, null, true), is(COUNT));
                assertThat((int) space.estimate(null, false, null, false), is(COUNT - 20));
                assertThat((int) space.estimate(new ByteArray(new byte[]{0}), true, new ByteArray(new byte[]{(byte) 255}), true), is(COUNT));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });
    }

    private void testIndexTransactions(final ITestIndexFactory factory) {
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, false);
                space.add("test1", createBuffer(0, 4));
                space.add("test2", createBuffer(1, 4));
                space.add("test3", createBuffer(2, 4));
                assertThat(space.find("test1"), is(createBuffer(0, 4)));
                assertThat(space.find("test2"), is(createBuffer(1, 4)));
                assertThat(space.find("test3"), is(createBuffer(2, 4)));

                space.remove("test3");
                assertThat(space.find("test3"), is(nullValue()));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                space.add("test4", createBuffer(3, 4));
                space.add("test5", createBuffer(4, 4));
                space.add("test6", createBuffer(5, 4));
                assertThat(space.find("test4"), is(createBuffer(3, 4)));
                assertThat(space.find("test5"), is(createBuffer(4, 4)));
                assertThat(space.find("test6"), is(createBuffer(5, 4)));

                space.remove("test2");
                space.remove("test6");

                space.add("test2", createBuffer(100, 4));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                assertThat(space.find("test1"), is(createBuffer(0, 4)));
                assertThat(space.find("test2"), is(createBuffer(100, 4)));
                assertThat(space.find("test4"), is(createBuffer(3, 4)));
                assertThat(space.find("test5"), is(createBuffer(4, 4)));
                assertThat(space.isEmpty(), is(false));
                assertThat(space.getCount(), is(4l));

                if (space instanceof TreeIndexSpace) {
                    TreeIndexStatistics statistics = ((TreeIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(4l));
                    assertThat(statistics.getDataSize(), is(36l));
                } else {
                    HashIndexStatistics statistics = ((HashIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(4l));
                    assertThat(statistics.getDataSize(), is(36l));
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                space.add("test6", createBuffer(5, 4));
                space.add("test7", createBuffer(6, 4));
                space.add("test8", createBuffer(7, 4));

                space.remove("test1");
                space.remove("test2");
                space.remove("test6");

                space.add("test2", createBuffer(100, 4));

                ((AbstractIndexSpace) space).onTransactionRolledBack();

                assertThat(space.find("test1"), is(createBuffer(0, 4)));
                assertThat(space.find("test2"), is(createBuffer(100, 4)));
                assertThat(space.find("test4"), is(createBuffer(3, 4)));
                assertThat(space.find("test5"), is(createBuffer(4, 4)));
                assertThat(space.isEmpty(), is(false));
                assertThat(space.getCount(), is(4l));
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                assertThat(space.find("test1"), is(createBuffer(0, 4)));
                assertThat(space.find("test2"), is(createBuffer(100, 4)));
                assertThat(space.find("test4"), is(createBuffer(3, 4)));
                assertThat(space.find("test5"), is(createBuffer(4, 4)));
                assertThat(space.isEmpty(), is(false));
                assertThat(space.getCount(), is(4l));

                if (space instanceof TreeIndexSpace) {
                    TreeIndexStatistics statistics = ((TreeIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(4l));
                    assertThat(statistics.getDataSize(), is(36l));
                } else {
                    HashIndexStatistics statistics = ((HashIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(4l));
                    assertThat(statistics.getDataSize(), is(36l));
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                space.clear();
                assertThat(space.isEmpty(), is(true));
                assertThat(space.getCount(), is(0l));

                ((AbstractIndexSpace) space).onTransactionRolledBack();

                assertThat(space.find("test1"), is(createBuffer(0, 4)));
                assertThat(space.find("test2"), is(createBuffer(100, 4)));
                assertThat(space.find("test4"), is(createBuffer(3, 4)));
                assertThat(space.find("test5"), is(createBuffer(4, 4)));
                assertThat(space.isEmpty(), is(false));
                assertThat(space.getCount(), is(4l));
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                assertThat(space.find("test1"), is(createBuffer(0, 4)));
                assertThat(space.find("test2"), is(createBuffer(100, 4)));
                assertThat(space.find("test4"), is(createBuffer(3, 4)));
                assertThat(space.find("test5"), is(createBuffer(4, 4)));
                assertThat(space.isEmpty(), is(false));
                assertThat(space.getCount(), is(4l));

                if (space instanceof TreeIndexSpace) {
                    TreeIndexStatistics statistics = ((TreeIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(4l));
                    assertThat(statistics.getDataSize(), is(36l));
                } else {
                    HashIndexStatistics statistics = ((HashIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(4l));
                    assertThat(statistics.getDataSize(), is(36l));
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                space.clear();
                assertThat(space.isEmpty(), is(true));
                assertThat(space.getCount(), is(0l));

                ((AbstractIndexSpace) space).onTransactionCommitted();
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                assertThat(space.isEmpty(), is(true));
                assertThat(space.getCount(), is(0l));

                if (space instanceof TreeIndexSpace) {
                    TreeIndexStatistics statistics = ((TreeIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(0l));
                    assertThat(statistics.getDataSize(), is(0l));
                } else {
                    HashIndexStatistics statistics = ((HashIndexSpace) space).getStatistics();
                    assertThat(statistics.getElementCount(), is(0l));
                    assertThat(statistics.getDataSize(), is(0l));
                }
            }
        });

        assertThat(new File(configuration.getPaths().get(0), "test-10.ix").exists(), is(true));

        database.transactionSync(new Operation(IOperation.FLUSH) {
            @Override
            public void run(ITransaction transaction) {
                IUniqueIndex<String, ByteArray> space = factory.create(transaction, true);

                ((AbstractIndexSpace) space).delete();
            }
        });

        assertThat(new File(configuration.getPaths().get(0), "test-14.ix").exists(), is(false));
    }

    private Pair<ByteArray, ByteArray> normalize(IUniqueIndex<String, ByteArray> space, Pair<String, ByteArray> pair) {
        return new Pair<ByteArray, ByteArray>(space.normalize(pair.getKey()), pair.getValue());
    }

    private void checkVarElement(Pair<ByteArray, ByteArray> pair, int i, boolean fixedValue) {
        ByteArray key = createBuffer(i, 8 + (i % 5));
        ByteArray value;
        if (fixedValue)
            value = createBuffer(2 * i, 4);
        else
            value = createBuffer(i, 7 + (i % 4));

        assertThat(pair.getKey(), is(key));
        assertThat(pair.getValue(), is(value));
    }

    private void checkFixedElement(Pair<ByteArray, ByteArray> pair, int i) {
        ByteArray key = createBuffer(i, 10);
        ByteArray value = createBuffer(2 * i, 4);

        assertThat(pair.getKey(), is(key));
        assertThat(pair.getValue(), is(value));
    }

    private ByteArray createBuffer(int base, int length) {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte) (base + i);

        return new ByteArray(buffer);
    }

    private String createString(int base, int length) {
        char[] buffer = new char[length << 1];
        for (int i = 0; i < (length << 1); ) {
            int a = base + i;
            buffer[i] = (char) ('A' + (a & 0xF));
            buffer[i + 1] = (char) ('A' + ((a >>> 4) & 0xF));
            i += 2;
        }

        return new String(buffer);
    }

    public static class TestRandom extends Random {
        public TestRandom() {
        }

        public TestRandom(long seed) {
            super(seed);
        }

        @Override
        public void setSeed(long seed) {
            System.out.println("seed - " + seed);
            super.setSeed(seed);
        }
    }

    ;

    private static class TestBatchControl implements IBatchControl {
        private int count = 2;

        @Override
        public boolean canContinue() {
            if (count == 0) {
                count = 2;
                return false;
            }

            count--;

            return true;
        }

        @Override
        public int getNonCachedPagesInvalidationQueueSize() {
            return 10;
        }

        @Override
        public void setNonCachedPagesInvalidationQueueSize(int value) {
        }

        @Override
        public void setMaxPageCacheSize(int pageTypeIndex, String category, long value) {
        }

        @Override
        public boolean isCachingEnabled() {
            return true;
        }

        @Override
        public void setCachingEnabled(boolean value) {
        }

        @Override
        public void setMaxCacheSize(String category, long value) {
        }

        @Override
        public boolean isPageCachingEnabled() {
            return true;
        }

        @Override
        public void setPageCachingEnabled(boolean value) {
        }
    }

    public interface ITestIndexFactory {
        <T extends IUniqueIndex> T create(ITransaction transaction, boolean open);
    }
}
