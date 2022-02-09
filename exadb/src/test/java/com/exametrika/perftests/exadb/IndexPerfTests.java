/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.exadb;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration.Strength;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IndexPerfTests} are performance tests for indexes.
 *
 * @author Medvedev-A
 */
public class IndexPerfTests {
    private static final int COUNT = 10000000;
    private Database database;
    private DatabaseConfiguration parameters;
    private DatabaseConfigurationBuilder builder;

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(1000000);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(500000000)).toConfiguration());
        parameters = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(null, parameters);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testIndexes() throws Throwable {
        testStringIndex("BTree unique string:", new BTreeIndexSchemaConfiguration("test1", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                false, true, java.util.Collections.<String, String>emptyMap()));
        testStringIndex("BTree non-unique string:", new BTreeIndexSchemaConfiguration("test2", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                false, false, java.util.Collections.<String, String>emptyMap()));
//        testStringIndex("Tree unique string:", new TreeIndexSchemaConfiguration("test3", 0, 
//            false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(), 
//            true, java.util.Collections.<String, String>emptyMap()));
//        testStringIndex("Tree non-unique string:", new TreeIndexSchemaConfiguration("test4", 0, 
//            false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(), 
//            false, java.util.Collections.<String, String>emptyMap()));

        testStringIndex("BTree unique collation string:", new BTreeIndexSchemaConfiguration("test5", 0,
                false, 256, true, 8, new CollatorKeyNormalizerSchemaConfiguration("en_EN", Strength.SECONDARY),
                new LongValueConverterSchemaConfiguration(),
                false, true, java.util.Collections.<String, String>emptyMap()));
        testStringIndex("BTree non-unique collation string:", new BTreeIndexSchemaConfiguration("test6", 0,
                false, 256, true, 8, new CollatorKeyNormalizerSchemaConfiguration("en_EN", Strength.SECONDARY),
                new LongValueConverterSchemaConfiguration(),
                false, false, java.util.Collections.<String, String>emptyMap()));
//        testStringIndex("Tree unique collation string:", new TreeIndexSchemaConfiguration("test7", 0, 
//            false, 256, true, 8, new CollatorKeyNormalizerSchemaConfiguration("en_EN", Strength.SECONDARY), 
//            new LongValueConverterSchemaConfiguration(), 
//            true, java.util.Collections.<String, String>emptyMap()));
//        testStringIndex("Tree non-unique collation string:", new TreeIndexSchemaConfiguration("test8", 0, 
//            false, 256, true, 8, new CollatorKeyNormalizerSchemaConfiguration("en_EN", Strength.SECONDARY), 
//            new LongValueConverterSchemaConfiguration(), 
//            false, java.util.Collections.<String, String>emptyMap()));

        testLongIndex("BTree unique long:", new BTreeIndexSchemaConfiguration("test9", 0,
                true, 8, true, 8, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG), new LongValueConverterSchemaConfiguration(),
                false, true, java.util.Collections.<String, String>emptyMap()));
        testLongIndex("BTree non-unique long:", new BTreeIndexSchemaConfiguration("test10", 0,
                true, 8, true, 8, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG), new LongValueConverterSchemaConfiguration(),
                false, false, java.util.Collections.<String, String>emptyMap()));
//        testLongIndex("Tree unique long:", new TreeIndexSchemaConfiguration("test11", 0, 
//            true, 8, true, 8, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG), new LongValueConverterSchemaConfiguration(), 
//            true, java.util.Collections.<String, String>emptyMap()));
//        testLongIndex("Tree non-unique long:", new TreeIndexSchemaConfiguration("test12", 0, 
//            true, 8, true, 8, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG), new LongValueConverterSchemaConfiguration(), 
//            false, java.util.Collections.<String, String>emptyMap()));
    }

    private void testStringIndex(String name, final IndexSchemaConfiguration configuration) throws Throwable {
        System.out.println(name);
        final int ids[] = new int[1];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.createIndex("test", configuration);
                ids[0] = index.getId();
            }
        });

        long l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);

                for (int i = 0; i < COUNT; i++)
                    index.add("test " + i + "long index" + i, (long) i);
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("add:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);

                for (int i = 0; i < COUNT; i++)
                    Assert.isTrue(index.find("test " + i + "long index" + i) == i);
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("find:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                ISortedIndex<String, Long> index = indexManager.getIndex(ids[0]);
                for (@SuppressWarnings("unused") Long v : index.findValues(null, true, null, true))
                    ;
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("iterate:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);

                for (int i = 0; i < COUNT; i++)
                    index.remove("test " + i + "long index" + i);
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("remove:" + l);
        System.out.println("");
    }

    private void testLongIndex(String name, final IndexSchemaConfiguration configuration) throws Throwable {
        System.out.println(name);
        final int ids[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<Long, Long> index = indexManager.createIndex("test", configuration);
                ids[0] = index.getId();
            }
        });

        long l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<Long, Long> index = indexManager.getIndex(ids[0]);

                for (int i = 0; i < COUNT; i++)
                    index.add((long) i, (long) i);
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("add:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<Long, Long> index = indexManager.getIndex(ids[0]);

                for (int i = 0; i < COUNT; i++)
                    Assert.isTrue(index.find((long) i) == i);
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("find:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                ISortedIndex<Long, Long> index = indexManager.getIndex(ids[0]);
                for (@SuppressWarnings("unused") Long v : index.findValues(null, true, null, true))
                    ;
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("iterate:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<Long, Long> index = indexManager.getIndex(ids[0]);

                for (int i = 0; i < COUNT; i++)
                    index.remove((long) i);
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("remove:" + l);
        System.out.println("");
    }
}
