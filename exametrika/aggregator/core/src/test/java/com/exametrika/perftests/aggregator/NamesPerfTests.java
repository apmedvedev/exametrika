/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.aggregator;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;


/**
 * The {@link NamesPerfTests} are performance tests for period names.
 *
 * @author Medvedev-A
 */
public class NamesPerfTests {
    private static final int COUNT = 500000;
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
    public void testScopeNames() throws Throwable {
        System.out.println("Scope names:");

        final long ids[] = new long[COUNT];
        final IScopeName[] scopes = new IScopeName[COUNT];
        for (int i = 0; i < COUNT; i++)
            scopes[i] = ScopeName.get("scope" + i);

        long l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < COUNT; i++)
                    ids[i] = nameManager.addName(scopes[i]).getId();
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("add:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < COUNT; i++) {
                    IPeriodName name = nameManager.findByName(scopes[i]);
                    Assert.isTrue(name.getName() == scopes[i] && name.getId() == ids[i]);
                }
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("find by name:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < COUNT; i++) {
                    IPeriodName name = nameManager.findById(ids[i]);
                    Assert.isTrue(name.getName() == scopes[i] && name.getId() == ids[i]);
                }
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("find by id:" + l);
        System.out.println("");
        database.printStatistics();
    }

    @Test
    public void testCallPaths() throws Throwable {
        System.out.println("CallPaths:");

        final long ids[] = new long[COUNT];
        final ICallPath[] callPaths = new ICallPath[COUNT];
        for (int i = 0; i < COUNT; i++)
            callPaths[i] = CallPath.get("scope" + i + ICallPath.SEPARATOR + "scope" + (i + 1));

        long l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < COUNT; i++)
                    ids[i] = nameManager.addName(callPaths[i]).getId();
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("add:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < COUNT; i++) {
                    IPeriodName name = nameManager.findByName(callPaths[i]);
                    Assert.isTrue(name.getName() == callPaths[i] && name.getId() == ids[i]);
                }
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("find by name:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < COUNT; i++) {
                    IPeriodName name = nameManager.findById(ids[i]);
                    Assert.isTrue(name.getName() == callPaths[i] && name.getId() == ids[i]);
                }
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("find by id:" + l);
        System.out.println("");
        database.printStatistics();
    }
}
