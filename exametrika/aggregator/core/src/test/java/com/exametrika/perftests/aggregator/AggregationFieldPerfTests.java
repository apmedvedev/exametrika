/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.aggregator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StandardRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
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
import com.exametrika.common.json.Json;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.tests.aggregator.fields.AggregationFieldTests.TestAggregationNode;
import com.exametrika.tests.aggregator.fields.AggregationFieldTests.TestAggregationNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.fields.AggregationFieldTests.TestDocumentSchemaFactoryConfiguration;
import com.exametrika.tests.aggregator.fields.AggregationFieldTests.TestObjectRepresentationSchemaConfiguration;
import com.exametrika.tests.aggregator.fields.AggregationFieldTests.TestRootNodeSchemaConfiguration;


/**
 * The {@link AggregationFieldPerfTests} are perftests for aggregation fields.
 *
 * @author Medvedev-A
 */
public class AggregationFieldPerfTests {
    private static final int COUNT = 1000000;
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
    public void testAggregationLogPerformance() {
        createDatabase();

        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node1 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("gauge"));
                ids[0] = node1.node.getId();

                TestAggregationNode node2 = period.findOrCreateNode(new Location(5, 5), cycleSchema.findNode("log"));
                ids[1] = node2.node.getId();
            }
        });

        final NameValue nameValue1 = new NameValue(Arrays.asList(new StandardValue(100, 1, 10, 1000)));
        final ComponentValue value1 = new ComponentValue(Arrays.asList(nameValue1), null);
        final ObjectValue objectValue1 = new ObjectValue(Json.object().put("message", "hello1").put("time", 111).toObject());
        final ComponentValue value2 = new ComponentValue(Arrays.asList(objectValue1), null);
        long t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node1 = period.findNodeById(ids[0]);
                ILogAggregationField field = node1.node.getField(2);

                for (int i = 0; i < COUNT; i++)
                    field.add(value1, i, 10);
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test addition without full text indexes: " + t);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node1 = period.findNodeById(ids[1]);
                ILogAggregationField field = node1.node.getField(2);

                for (int i = 0; i < COUNT; i++)
                    field.add(value2, i, 10);
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test addition with full text index: " + t);

        t = Times.getCurrentTime();
        final int r[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node = period.findNodeById(ids[0]);
                ILogAggregationField field = node.node.getField(2);

                for (@SuppressWarnings("unused") IAggregationRecord record : field.getRecords())
                    r[0]++;
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test iteration: " + t + ", " + r[0]);

        t = Times.getCurrentTime();
        final int rr[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node = period.findNodeById(ids[0]);
                ILogAggregationField field = node.node.getField(2);

                for (@SuppressWarnings("unused") IAggregationRecord record : field.getReverseRecords())
                    rr[0]++;
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test reverse iteration: " + t + ", " + rr[0]);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node = period.findNodeById(ids[0]);
                ILogAggregationField field = node.node.getField(2);

                ILogAggregationField.IAggregationIterator it = field.getRecords().iterator();
                r[0] = 0;
                while (it.hasNext()) {
                    it.next();
                    it.getRepresentation(0, false, false);
                    r[0]++;
                }
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test representation: " + t + ", " + r[0]);

        t = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node = period.findNodeById(ids[0]);
                ILogAggregationField field = node.node.getField(2);

                ILogAggregationField.IAggregationIterator it = field.getReverseRecords().iterator();
                rr[0] = 0;
                while (it.hasNext()) {
                    it.next();
                    it.getRepresentation(0, false, false);
                    rr[0]++;
                }
            }
        });

        t = Times.getCurrentTime() - t;
        System.out.println("Test reverse representation: " + t + ", " + rr[0]);
    }

    private void createDatabase() {
        TestRootNodeSchemaConfiguration nodeConfiguration1 = new TestRootNodeSchemaConfiguration("cyclePeriodRootNode");

        MetricTypeSchemaConfiguration metricType1 = new GaugeSchemaConfiguration("gauge", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(
                        new NameRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new NameRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true)))), false);
        MetricTypeSchemaConfiguration metricType2 = new LogSchemaConfiguration("log",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("first"), new TestObjectRepresentationSchemaConfiguration("second")),
                null, (List) Collections.emptyList(), true, new TestDocumentSchemaFactoryConfiguration());

        AggregationComponentTypeSchemaConfiguration componentType1 = new NameSchemaConfiguration("test1",
                Arrays.asList(metricType1), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, true, null, null);
        AggregationComponentTypeSchemaConfiguration componentType2 = new NameSchemaConfiguration("test2",
                Arrays.asList(metricType2), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, true, null, null);

        LogAggregationFieldSchemaConfiguration field1 = new LogAggregationFieldSchemaConfiguration("log", "log", null, "blobStore",
                componentType1);

        LogAggregationFieldSchemaConfiguration field2 = new LogAggregationFieldSchemaConfiguration("log", "log", null, "blobStore",
                componentType2);

        TestAggregationNodeSchemaConfiguration nodeConfiguration2 = new TestAggregationNodeSchemaConfiguration("gauge",
                componentType1, field1, false);
        TestAggregationNodeSchemaConfiguration nodeConfiguration3 = new TestAggregationNodeSchemaConfiguration("log",
                componentType2, field2, false);

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        null, "cyclePeriodRootNode", 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });
    }
}
