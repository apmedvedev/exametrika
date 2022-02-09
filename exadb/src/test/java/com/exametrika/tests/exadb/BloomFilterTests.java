/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.index.BloomFilterSpace;


/**
 * The {@link BloomFilterTests} are tests for {@link BloomFilterSpace}.
 *
 * @author Medvedev-A
 */
public class BloomFilterTests {
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
    public void testFilter() {
        final int pageTypeIndex = 2;
        final int initialHashCount = 13;
        final int[] n = new int[1];
        final int addCount = 1000000;
        final int checkCount = 10000000;
        long t = Times.getCurrentTime();
        final BloomFilterSpace[] filter = new BloomFilterSpace[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                filter[0] = BloomFilterSpace.create(100, "test", 0, pageTypeIndex, database.getContext(),
                        Collections.<String, String>emptyMap(), initialHashCount);

                for (int i = 0; i < addCount / 2; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    filter[0].add(value);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                filter[0] = BloomFilterSpace.open(100, "test", 0, pageTypeIndex, database.getContext(),
                        Collections.<String, String>emptyMap(), initialHashCount);

                for (int i = addCount / 2; i < addCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    filter[0].add(value);
                }
            }
        });

        System.out.println("Add time: " + (Times.getCurrentTime() - t));
        t = Times.getCurrentTime();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                for (int i = 0; i < addCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    assertThat(filter[0].isNotContained(value), is(false));
                }

                for (int i = addCount; i < checkCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    if (!filter[0].isNotContained(value))
                        n[0]++;
                }
            }
        });

        System.out.println("Find time: " + (Times.getCurrentTime() - t));
        System.out.println("Collisions: " + n[0]);
        t = Times.getCurrentTime();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                BloomFilterSpace filter = BloomFilterSpace.open(100, "test", 0, pageTypeIndex, database.getContext(),
                        Collections.<String, String>emptyMap(), initialHashCount);

                for (int i = 0; i < addCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    assertThat(filter.isNotContained(value), is(false));
                }

                int c = 0;
                for (int i = addCount; i < checkCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    if (!filter.isNotContained(value))
                        c++;
                }

                assertThat(c, is(n[0]));
            }
        });

        System.out.println("Find time2: " + (Times.getCurrentTime() - t));

        IOs.close(database);
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        t = Times.getCurrentTime();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                BloomFilterSpace filter = BloomFilterSpace.open(100, "test", 0, pageTypeIndex, database.getContext(),
                        Collections.<String, String>emptyMap(), initialHashCount);

                for (int i = 0; i < addCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    assertThat(filter.isNotContained(value), is(false));
                }

                int c = 0;
                for (int i = addCount; i < checkCount; i++) {
                    ByteArray value = Indexes.normalizeInt(i);
                    if (!filter.isNotContained(value))
                        c++;
                }

                assertThat(c, is(n[0]));
            }
        });

        System.out.println("Find time3: " + (Times.getCurrentTime() - t));
    }
}
