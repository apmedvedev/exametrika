/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.name.PeriodNameCache;
import com.exametrika.impl.aggregator.name.PeriodNameManager;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.PeriodTestNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNode;
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
 * The {@link PeriodNameManagerTests} are tests for period name manager.
 *
 * @author Medvedev-A
 */
public class PeriodNameManagerTests {
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
    public void testNames() throws Throwable {
        final long scopeIds[] = new long[100];
        final long metricIds[] = new long[100];
        final long callPathIds[] = new long[100];
        final IPeriodName periodNames[] = new IPeriodName[100];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                assertThat(nameManager.addName(ScopeName.root()), nullValue());
                assertThat(nameManager.addName(MetricName.root()), nullValue());
                assertThat(nameManager.addName(CallPath.root()), nullValue());

                assertThat(nameManager.getName(ScopeName.root()), is(0l));
                assertThat(nameManager.getName(MetricName.root()), is(0l));
                assertThat(nameManager.getName(CallPath.root()), is(0l));
                assertThat(nameManager.getCallPath(0, 0), is(0l));

                for (int i = 0; i < 100; i++) {
                    ScopeName scope = ScopeName.get("scope" + i);
                    IPeriodName scopeName = nameManager.addName(scope);
                    scopeIds[i] = scopeName.getId();
                    assertThat(scopeName.getName() == scope, is(true));
                    assertThat(scopeName.isStale(), is(false));
                    assertThat(nameManager.findById(scopeName.getId()) == scopeName, is(true));
                    assertThat(nameManager.findByName(scope) == scopeName, is(true));
                    periodNames[i] = scopeName;
                    assertThat(nameManager.getName(scope), is(scopeIds[i]));

                    MetricName metric = MetricName.get("metric" + i);
                    IPeriodName metricName = nameManager.addName(metric);
                    metricIds[i] = metricName.getId();
                    assertThat(metricName.getName() == metric, is(true));
                    assertThat(metricName.isStale(), is(false));
                    assertThat(nameManager.getName(metric), is(metricIds[i]));

                    CallPath callPath = CallPath.get("metric" + i + ICallPath.SEPARATOR + "metric" + (i + 1));
                    IPeriodName callPathName = nameManager.addName(callPath);
                    callPathIds[i] = callPathName.getId();
                    assertThat(callPathName.getName() == callPath, is(true));
                    assertThat(callPathName.isStale(), is(false));

                    assertThat(nameManager.getName(callPath), is(callPathIds[i]));

                    long parentId = nameManager.getName(callPath.getParent());
                    long metricId = nameManager.getName(callPath.getLastSegment());
                    assertThat(nameManager.getCallPath(parentId, metricId), is(callPathIds[i]));

                    CallPath callPath2 = CallPath.get("metric" + i);
                    parentId = nameManager.getName(callPath2.getParent());
                    metricId = nameManager.getName(callPath2.getLastSegment());
                    assertThat(nameManager.getCallPath(parentId, metricId), is(nameManager.getName(callPath2)));
                }
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
                        PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                        nameManager.addName(ScopeName.get("test"));
                    }
                });
            }
        });


        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                for (int i = 0; i < 100; i++) {
                    ScopeName scope = ScopeName.get("scope" + i);
                    assertThat(nameManager.findById(scopeIds[i]).getName() == scope, is(true));
                    assertThat(nameManager.findByName(scope).getId() == scopeIds[i], is(true));

                    MetricName metric = MetricName.get("metric" + i);
                    assertThat(nameManager.findById(metricIds[i]).getName() == metric, is(true));
                    assertThat(nameManager.findByName(metric).getId() == metricIds[i], is(true));

                    CallPath callPath = CallPath.get("metric" + i + ICallPath.SEPARATOR + "metric" + (i + 1));
                    assertThat(nameManager.findById(callPathIds[i]).getName() == callPath, is(true));
                    assertThat(nameManager.findByName(callPath).getId() == callPathIds[i], is(true));
                }
            }
        });

        PeriodNameCache nameCache = ((PeriodNameManager) database.getContext().findTransactionExtension(
                IPeriodNameManager.NAME)).getNameCache();

        database.close();

        assertThat(((Long) Tests.get(nameCache, "cacheSize")).intValue(), is(0));
        assertThat(periodNames[0].isStale(), is(true));

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                for (int i = 0; i < 100; i++) {
                    ScopeName scope = ScopeName.get("scope" + i);
                    assertThat(nameManager.findById(scopeIds[i]).getName() == scope, is(true));
                    assertThat(nameManager.findByName(scope).getId() == scopeIds[i], is(true));

                    MetricName metric = MetricName.get("metric" + i);
                    assertThat(nameManager.findById(metricIds[i]).getName() == metric, is(true));
                    assertThat(nameManager.findByName(metric).getId() == metricIds[i], is(true));

                    CallPath callPath = CallPath.get("metric" + i + ICallPath.SEPARATOR + "metric" + (i + 1));
                    assertThat(nameManager.findById(callPathIds[i]).getName() == callPath, is(true));
                    assertThat(nameManager.findByName(callPath).getId() == callPathIds[i], is(true));
                }
            }
        });

        final long newNameIds[] = new long[1];
        final IPeriodName newNames[] = new IPeriodName[1];
        try {
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                    ScopeName scope = ScopeName.get("scope" + 1000);
                    IPeriodName scopeName = nameManager.addName(scope);
                    newNameIds[0] = scopeName.getId();
                    assertThat(nameManager.findById(scopeName.getId()) == scopeName, is(true));
                    assertThat(nameManager.findByName(scope) == scopeName, is(true));
                    newNames[0] = scopeName;

                    throw new RuntimeException("test");
                }
            });
        } catch (Exception e) {
            assertThat(e.getCause() instanceof RuntimeException, is(true));
        }

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                assertThat(nameManager.findById(newNameIds[0]), nullValue());
                assertThat(nameManager.findByName(ScopeName.get("scope" + 1000)), nullValue());
                assertThat(newNames[0].isStale(), is(true));
            }
        });
    }

    @Test
    public void testPeriodNode() throws Throwable {
        final CallPath callPath = CallPath.get("metric0" + ICallPath.SEPARATOR + "metric1");
        final ScopeName scope = ScopeName.get("scope");

        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Collections.<FieldSchemaConfiguration>emptyList());

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IPeriodName scopeName = nameManager.addName(scope);
                IPeriodName metricName = nameManager.addName(callPath);

                period.addNode(new Location(scopeName.getId(), metricName.getId()), 0);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                PeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IPeriodName scopeName = nameManager.findByName(scope);
                IPeriodName metricName = nameManager.findByName(callPath);

                INodeIndex<Location, TestPeriodNode> nodeIndex = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));

                TestPeriodNode node1 = nodeIndex.find(new Location(scopeName.getId(), metricName.getId()));
                assertThat(((IPeriodNode) node1.node).getScope() == scope, is(true));
                assertThat(((IPeriodNode) node1.node).getMetric() == callPath, is(true));
            }
        });
    }
}
