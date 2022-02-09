/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.index.IBTreeIndex;
import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IndexManagerTests} are tests for index manager.
 *
 * @author Medvedev-A
 */
public class IndexManagerTests {
    private Database database;
    private DatabaseConfiguration dbConfiguration;
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
        dbConfiguration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put("disableModules", true);
        database = new DatabaseFactory().createDatabase(parameters, dbConfiguration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testIndexes() throws Throwable {
        testIndexes(new BTreeIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                false, true, java.util.Collections.<String, String>emptyMap()));
        testIndexes(new BTreeIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                false, false, java.util.Collections.<String, String>emptyMap()));
        testIndexes(new TreeIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                true, true, java.util.Collections.<String, String>emptyMap()));
        testIndexes(new TreeIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                true, false, java.util.Collections.<String, String>emptyMap()));
        testIndexes(new HashIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                java.util.Collections.<String, String>emptyMap()));
    }

    @Test
    public void testUnloadByTimer() throws Throwable {
        database.close();
        builder.setTimerPeriod(500);
        builder.addExtension(new IndexDatabaseExtensionConfiguration(500, new FullTextIndexConfiguration()));

        dbConfiguration = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(parameters, dbConfiguration);
        database.open();

        final IIndex[] indexes = new IIndex[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IndexSchemaConfiguration configuration = new BTreeIndexSchemaConfiguration("test", 0,
                        false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap());
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                indexes[0] = indexManager.createIndex("test", configuration);
            }
        });

        Thread.sleep(1000);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                assertThat(indexes[0].isStale(), is(true));
            }
        });
    }

    @Test
    public void testTransactionRollback() throws Throwable {
        final IndexSchemaConfiguration configuration = new BTreeIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                false, true, java.util.Collections.<String, String>emptyMap());
        final IndexSchemaConfiguration configuration2 = new HashIndexSchemaConfiguration("test", 0,
                false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                java.util.Collections.<String, String>emptyMap());
        final int ids[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.createIndex("test", configuration);
                assertThat(indexManager.getIndexes().size(), is(1));
                ids[0] = index.getId();
            }
        });

        try {
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                    indexManager.createIndex("test", configuration2);
                    assertThat(indexManager.getIndexes().size(), is(2));
                    indexManager.getIndex(ids[0]).delete();

                    throw new RuntimeException("test");
                }
            });
        } catch (Exception e) {
            assertThat(e.getCause() instanceof RuntimeException, is(true));
        }

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                assertThat(indexManager.getIndexes().size(), is(1));
                IIndexManager.IndexInfo info = indexManager.getIndexes().get(0);
                assertThat(info.schema, is(configuration));
                assertThat(info.filePrefix, is("test"));
                assertThat(info.id, is(ids[0]));

                assertThat(indexManager.getIndex(ids[0]) instanceof IBTreeIndex, is(true));
            }
        });
    }

    private void testIndexes(final IndexSchemaConfiguration configuration) throws Throwable {
        final int ids[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.createIndex("test", configuration);
                index.add("test1", 1l);
                index.add("test2", 2l);
                index.add("test3", 3l);
                assertThat(indexManager.getIndex(index.getId()) == index, is(true));
                assertThat(indexManager.getIndexes().size(), is(1));
                IIndexManager.IndexInfo info = indexManager.getIndexes().get(0);
                assertThat(info.schema, is(configuration));
                assertThat(info.filePrefix, is("test"));
                assertThat(info.id, is(index.getId()));
                ids[0] = info.id;

                assertThat(index.find("test1"), is(1l));
                assertThat(index.find("test2"), is(2l));
                assertThat(index.find("test3"), is(3l));

                index.remove("test3");
                assertThat(index.find("test3"), is(nullValue()));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);

                assertThat(index.find("test1"), is(1l));
                assertThat(index.find("test2"), is(2l));

                index.unload();
                assertThat(index.isStale(), is(true));

                IUniqueIndex<String, Long> index2 = indexManager.getIndex(ids[0]);
                assertThat(index != index2, is(true));

                index2.delete();
                assertThat(index2.isStale(), is(true));
                assertThat(indexManager.getIndexes().size(), is(0));

                index = indexManager.createIndex("test", configuration);
                ids[0] = index.getId();
                index.add("test3", 3l);
                index.add("test4", 4l);
                index.add("test5", 5l);
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value instanceof RawDatabaseException && value.getCause() instanceof IllegalStateException;
            }
        }, IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation(true) {
                    @Override
                    public void run(ITransaction transaction) {
                        IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                        indexManager.createIndex("test1", configuration);
                    }
                });
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, dbConfiguration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);

                assertThat(index.find("test1"), nullValue());
                assertThat(index.find("test2"), nullValue());
                assertThat(index.find("test3"), is(3l));
                assertThat(index.find("test4"), is(4l));
                assertThat(index.find("test5"), is(5l));

                index.unload();
            }
        });

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value instanceof RawDatabaseException && value.getCause() instanceof IllegalStateException;
            }
        }, IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation(true) {
                    @Override
                    public void run(ITransaction transaction) {
                        IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                        IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);
                        index.delete();
                    }
                });
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);

                IUniqueIndex<String, Long> index = indexManager.getIndex(ids[0]);
                index.delete();
            }
        });
    }
}
