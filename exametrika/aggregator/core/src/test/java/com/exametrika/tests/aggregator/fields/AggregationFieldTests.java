/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.fields;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.CounterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.InfoSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PercentageRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackCounterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StandardRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.AggregationNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.schema.ILogAggregationFieldSchema;
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
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.IVisitor;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.common.values.AggregationContext;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.fields.AggregationRecord;
import com.exametrika.impl.aggregator.nodes.AggregationNode;
import com.exametrika.impl.aggregator.nodes.PeriodNodeObject;
import com.exametrika.impl.aggregator.schema.AggregationNodeSchema;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.aggregator.schema.PeriodNodeSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.DocumentSchemaFactoryConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link AggregationFieldTests} are tests for aggregation fields.
 *
 * @author Medvedev-A
 */
public class AggregationFieldTests {
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
        builder.setTimerPeriod(10);
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
    public void testLogField() throws Throwable {
        TestRootNodeSchemaConfiguration nodeConfiguration1 = new TestRootNodeSchemaConfiguration("cyclePeriodRootNode");

        MetricTypeSchemaConfiguration metricType1 = new GaugeSchemaConfiguration("metricType1", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(
                        new NameRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new NameRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true),
                                new PercentageRepresentationSchemaConfiguration(null, "current", null, null, "std.count", "metricType2.std.sum", true)))), false);
        MetricTypeSchemaConfiguration metricType2 = new CounterSchemaConfiguration("metricType2", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new NameRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new NameRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true)))));
        MetricTypeSchemaConfiguration metricType3 = new StackCounterSchemaConfiguration("metricType3", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new StackRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new StackRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true)))));
        MetricTypeSchemaConfiguration metricType4 = new InfoSchemaConfiguration("metricType4",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("first"), new TestObjectRepresentationSchemaConfiguration("second")));
        MetricTypeSchemaConfiguration metricType5 = new LogSchemaConfiguration("metricType5",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("first"), new TestObjectRepresentationSchemaConfiguration("second")),
                null, (List) Collections.emptyList(), true, new TestDocumentSchemaFactoryConfiguration());

        AggregationComponentTypeSchemaConfiguration componentType1 = new NameSchemaConfiguration("node1",
                Arrays.asList(metricType1, metricType2, metricType3, metricType4), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, true, null, null);
        AggregationComponentTypeSchemaConfiguration componentType2 = new NameSchemaConfiguration("node2",
                Arrays.asList(metricType5), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, true, null, null);

        LogAggregationFieldSchemaConfiguration field1 = new LogAggregationFieldSchemaConfiguration("field1", "field1", null, "blobStore",
                componentType1);

        LogAggregationFieldSchemaConfiguration field2 = new LogAggregationFieldSchemaConfiguration("field2", "field2", null, "blobStore",
                componentType2);

        TestAggregationNodeSchemaConfiguration nodeConfiguration2 = new TestAggregationNodeSchemaConfiguration("node1",
                componentType1, field1, false);
        TestAggregationNodeSchemaConfiguration nodeConfiguration3 = new TestAggregationNodeSchemaConfiguration("node2",
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

        NameValue nameValue1 = new NameValue(Arrays.asList(new StandardValue(100, 1, 10, 1000)));
        NameValue nameValue2 = new NameValue(Arrays.asList(new StandardValue(200, 2, 20, 2000)));
        NameValue nameValue3 = new NameValue(Arrays.asList(new StandardValue(300, 3, 30, 3000)));
        StackValue stackValue1 = new StackValue(Arrays.asList(new StandardValue(100, 1, 10, 1000)),
                Arrays.asList(new StandardValue(100, 1, 10, 1000)));
        StackValue stackValue2 = new StackValue(Arrays.asList(new StandardValue(200, 2, 20, 2000)),
                Arrays.asList(new StandardValue(200, 2, 20, 2000)));
        StackValue stackValue3 = new StackValue(Arrays.asList(new StandardValue(300, 3, 30, 3000)),
                Arrays.asList(new StandardValue(300, 3, 30, 3000)));
        ObjectValue objectValue1 = new ObjectValue(Json.object().put("message", "hello1").put("time", 111).toObject());
        ObjectValue objectValue2 = new ObjectValue(Json.object().put("message", "hello2").put("time", 222).toObject());
        ObjectValue objectValue3 = new ObjectValue(Json.object().put("message", "hello3").put("time", 333).toObject());

        final ComponentValue value1 = new ComponentValue(Arrays.asList(nameValue1, nameValue1, stackValue1, objectValue1), null);
        final ComponentValue value11 = new ComponentValue(Arrays.asList(nameValue1, nameValue1, stackValue1, objectValue1), Json.object().put("key", "value").toObject());
        final ComponentValue value2 = new ComponentValue(Arrays.asList(nameValue2, nameValue2, stackValue2, objectValue2), Json.object().put("key", "value").toObject());
        final ComponentValue value3 = new ComponentValue(Arrays.asList(nameValue3, nameValue3, stackValue3, objectValue3), Json.object().put("key", "value").toObject());

        final ComponentValue logValue1 = new ComponentValue(Arrays.asList(objectValue1), null);
        final ComponentValue logValue2 = new ComponentValue(Arrays.asList(
                new ObjectValue(Json.array().addObject().put("message", "hello2").put("time", 222).end().toArray())), null);
        final ComponentValue logValue21 = new ComponentValue(Arrays.asList(objectValue2), null);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node1 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node1"));
                TestAggregationNode node2 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node2"));
                ILogAggregationField log1 = node1.node.getField(2);
                assertThat(periodSchema.findAggregationNode("node1") == node1.getSchema(), is(true));
                ILogAggregationField log2 = node2.node.getField(2);
                assertThat(periodSchema.findAggregationNode("node2") == node2.getSchema(), is(true));
                IJsonField metadata1 = node1.node.getField(3);
                log1.add(value1, 1000, 100);
                assertThat(metadata1.get(), nullValue());
                log1.add(value2, 3000, 100);
                assertThat(log1.getCurrent(), is((IAggregationRecord) new AggregationRecord(value2, 3000, 100)));
                assertThat(com.exametrika.common.utils.Collections.toList(log1.getRecords().iterator()),
                        is(Arrays.asList((IAggregationRecord) new AggregationRecord(value11, 1000, 100),
                                (IAggregationRecord) new AggregationRecord(value2, 3000, 100))));
                assertThat((JsonObject) metadata1.get(), is(Json.object().put("key", "value").toObject()));

                log2.add(logValue1, 1000, 100);
                log2.add(logValue2, 3000, 100);
                assertThat(log2.getCurrent(), is((IAggregationRecord) new AggregationRecord(logValue21, 3000, 100)));
                assertThat(com.exametrika.common.utils.Collections.toList(log2.getRecords().iterator()),
                        is(Arrays.asList((IAggregationRecord) new AggregationRecord(logValue1, 1000, 100),
                                (IAggregationRecord) new AggregationRecord(logValue21, 3000, 100))));

                final IComponentAccessor accessor = log1.getSchema().getRepresentations().get(0).getAccessorFactory(
                ).createAccessor(null, null, "metricType3.inherent.std.count");
                final List<IAggregationRecord> records = new ArrayList<IAggregationRecord>();
                final IComputeContext context = log1.getComputeContext();
                log1.getRecords().visitRecords(new ICondition<IAggregationRecord>() {
                    @Override
                    public boolean evaluate(IAggregationRecord value) {
                        context.setTime(value.getTime());
                        context.setPeriod(value.getPeriod());

                        if (((Number) accessor.get(value.getValue(), context)).intValue() == 200)
                            return true;
                        else
                            return false;
                    }
                }, new IVisitor<IAggregationRecord>() {
                    @Override
                    public boolean visit(IAggregationRecord element) {
                        records.add(element);
                        return true;
                    }
                });

                assertThat(records,
                        is(Arrays.asList((IAggregationRecord) new AggregationRecord(value2, 3000, 100))));
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
                Period period = cycle.getCyclePeriod();

                TestAggregationNode node1 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node1"));
                TestAggregationNode node2 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node2"));
                ILogAggregationField log1 = node1.node.getField(2);
                ILogAggregationField log2 = node2.node.getField(2);
                IJsonField metadata1 = node1.node.getField(3);
                log1.add(value3, 1000, 100);
                assertThat((JsonObject) metadata1.get(), is(Json.object().put("key", "value").toObject()));
                assertThat(log1.getCurrent(), is((IAggregationRecord) new AggregationRecord(value3, 3000, 100)));

                Object[] reps = new Object[8];
                reps[0] = log1.getRepresentation(0, true, true);
                reps[1] = log1.getRepresentation(1, false, false);
                ILogAggregationField.IAggregationIterator it = log1.getRecords().iterator();
                it.next();
                it.next();
                reps[2] = it.getRepresentation(0, true, true);
                reps[3] = it.getRepresentation(1, false, false);

                ILogAggregationFieldSchema schema = log2.getSchema();
                INodeSearchResult res = log2.getFullTextIndex().search(Queries.term("message", "hello1").toQuery(schema.getDocumentSchema()), 1000);
                assertThat(res.getTotalCount(), is(1));
                assertThat(((Pair) res.getTopElements().get(0).get()).getValue(), is((Object) new AggregationRecord(logValue1, 1000, 100)));

                reps[4] = log2.getRepresentation(0, true, true);
                reps[5] = log2.getRepresentation(1, false, false);
                it = log2.getRecords().iterator();
                it.next();
                reps[6] = it.getRepresentation(0, true, true);
                reps[7] = it.getRepresentation(1, false, false);

                res = log2.getFullTextIndex().search(Queries.expression("time", "time:[0 TO 150]").toQuery(schema.getDocumentSchema()), 1000);
                assertThat(res.getTotalCount(), is(1));
                assertThat(((Pair) res.getTopElements().get(0).get()).getValue(), is((Object) new AggregationRecord(logValue1, 1000, 100)));

                JsonArrayBuilder builder = new JsonArrayBuilder();
                for (int i = 0; i < reps.length; i++)
                    builder.add(reps[i]);

                JsonArray ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data5.json", false);
                builder = JsonSerializers.read(builder.toString(), true);
                assertThat(builder.toJson(), is(ethalon));
            }
        });
    }

    @Test
    public void testPeriodField() throws Throwable {
        TestRootNodeSchemaConfiguration nodeConfiguration1 = new TestRootNodeSchemaConfiguration("cyclePeriodRootNode");

        MetricTypeSchemaConfiguration metricType1 = new GaugeSchemaConfiguration("metricType1", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(
                        new NameRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new NameRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true),
                                new PercentageRepresentationSchemaConfiguration(null, "current", null, null, "std.count", "metricType2.std.sum", true)))), false);
        MetricTypeSchemaConfiguration metricType2 = new CounterSchemaConfiguration("metricType2", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new NameRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new NameRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true)))));
        MetricTypeSchemaConfiguration metricType3 = new StackCounterSchemaConfiguration("metricType3", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new StackRepresentationSchemaConfiguration("first", Arrays.asList(new StandardRepresentationSchemaConfiguration(true))),
                        new StackRepresentationSchemaConfiguration("second", Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new PeriodRepresentationSchemaConfiguration("std.sum", true)))));
        MetricTypeSchemaConfiguration metricType4 = new InfoSchemaConfiguration("metricType4",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("first"), new TestObjectRepresentationSchemaConfiguration("second")));
        MetricTypeSchemaConfiguration metricType5 = new LogSchemaConfiguration("metricType5",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("first"), new TestObjectRepresentationSchemaConfiguration("second")),
                null, (List) Collections.emptyList(), true, new TestDocumentSchemaFactoryConfiguration());

        AggregationComponentTypeSchemaConfiguration componentType1 = new NameSchemaConfiguration("node3",
                Arrays.asList(metricType1, metricType2, metricType3, metricType4), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, true, null, null);
        AggregationComponentTypeSchemaConfiguration componentType2 = new NameSchemaConfiguration("node4",
                Arrays.asList(metricType5), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, true, null, null);

        LogAggregationFieldSchemaConfiguration field1 = new LogAggregationFieldSchemaConfiguration("field1", "field1", null, "blobStore",
                new NameSchemaConfiguration("field1", Arrays.asList(metricType1, metricType2, metricType3, metricType4),
                        true, Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                        Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                        null, null, null, null, null, false, true, null, null));

        LogAggregationFieldSchemaConfiguration field2 = new LogAggregationFieldSchemaConfiguration("field2", "field2", null, "blobStore",
                new NameSchemaConfiguration("field2", Arrays.asList(metricType5), true,
                        Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                        Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                        null, null, null, null, null, false, true, null, null));

        TestAggregationNodeSchemaConfiguration nodeConfiguration2 = new TestAggregationNodeSchemaConfiguration("node1",
                componentType1, field1, true);
        TestAggregationNodeSchemaConfiguration nodeConfiguration3 = new TestAggregationNodeSchemaConfiguration("node2",
                componentType2, field2, true);

        PeriodAggregationFieldSchemaConfiguration field11 = new PeriodAggregationFieldSchemaConfiguration("field1", "field1", null,
                new NameSchemaConfiguration("field1", Arrays.asList(metricType1, metricType2, metricType3, metricType4),
                        true, Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                        Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                        null, null, null, null, null, false, true, null, null), "node1");

        PeriodAggregationFieldSchemaConfiguration field12 = new PeriodAggregationFieldSchemaConfiguration("field2", "field2", null,
                new NameSchemaConfiguration("field2", Arrays.asList(metricType5), true,
                        Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                        Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(),
                        null, null, null, null, null, false, true, null, null), "node2");

        TestAggregationNodeSchemaConfiguration nodeConfiguration4 = new TestAggregationNodeSchemaConfiguration("node3",
                componentType1, field11, false);
        TestAggregationNodeSchemaConfiguration nodeConfiguration5 = new TestAggregationNodeSchemaConfiguration("node4",
                componentType2, field12, false);

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2,
                        nodeConfiguration3, nodeConfiguration4, nodeConfiguration5)),
                        null, "cyclePeriodRootNode", 1000000, 10, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        NameValue nameValue1 = new NameValue(Arrays.asList(new StandardValue(100, 1, 10, 1000)));
        NameValue nameValue2 = new NameValue(Arrays.asList(new StandardValue(200, 2, 20, 2000)));
        NameValue nameValue3 = new NameValue(Arrays.asList(new StandardValue(300, 1, 20, 3000)));
        StackValue stackValue1 = new StackValue(Arrays.asList(new StandardValue(100, 1, 10, 1000)),
                Arrays.asList(new StandardValue(100, 1, 10, 1000)));
        StackValue stackValue2 = new StackValue(Arrays.asList(new StandardValue(200, 2, 20, 2000)),
                Arrays.asList(new StandardValue(200, 2, 20, 2000)));
        StackValue stackValue3 = new StackValue(Arrays.asList(new StandardValue(300, 1, 20, 3000)),
                Arrays.asList(new StandardValue(300, 1, 20, 3000)));
        ObjectValue objectValue1 = new ObjectValue(Json.object().put("message", "hello1").put("time", 111).toObject());
        ObjectValue objectValue2 = new ObjectValue(Json.object().put("message", "hello2").put("time", 222).toObject());
        ObjectValue objectValue3 = new ObjectValue(Json.object().put("message", "hello2").put("time", 222).toObject());

        final ComponentValue value1 = new ComponentValue(Arrays.asList(nameValue1, nameValue1, stackValue1, objectValue1), null);
        final ComponentValue value11 = new ComponentValue(Arrays.asList(nameValue1, nameValue1, stackValue1, objectValue1), Json.object().put("key", "value").toObject());
        final ComponentValue value2 = new ComponentValue(Arrays.asList(nameValue2, nameValue2, stackValue2, objectValue2), Json.object().put("key", "value").toObject());
        final ComponentValue value3 = new ComponentValue(Arrays.asList(nameValue3, nameValue3, stackValue3, objectValue3), Json.object().put("key", "value").toObject());

        final ComponentValue logValue1 = new ComponentValue(Arrays.asList(objectValue1), null);
        final ComponentValue logValue2 = new ComponentValue(Arrays.asList(
                new ObjectValue(Json.array().addObject().put("message", "hello2").put("time", 222).end().toArray())), null);
        final ComponentValue logValue21 = new ComponentValue(Arrays.asList(objectValue2), null);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                AggregationContext context = new AggregationContext();
                context.setTime(3000);
                context.setPeriod(100);
                TestAggregationNode node1 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node3"));
                TestAggregationNode node2 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node4"));
                IPeriodAggregationField field1 = node1.node.getField(2);
                assertThat(periodSchema.findAggregationNode("node3") == node1.getSchema(), is(true));
                assertThat(field1.getLog() != null, is(true));
                field1.aggregate(value1, context);
                field1.aggregate(value2, context);

                IPeriodAggregationField field2 = node2.node.getField(2);
                assertThat(field2.getLog() != null, is(true));
                field2.aggregate(logValue1, context);
                field2.aggregate(logValue2, context);

                try {
                    cycle.addPeriod();
                    Tests.set(period, "closed", false);
                    field2.onPeriodClosed(period);
                    field1.onPeriodClosed(period);
                } catch (Exception e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        final Object[] reps = new Object[4];

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestAggregationNode node1 = period.<INodeIndex<Location, TestAggregationNode>>getIndex(
                        cycleSchema.findNode("node3").findField("location")).find(new Location(1, 1));
                TestAggregationNode node2 = period.<INodeIndex<Location, TestAggregationNode>>getIndex(
                        cycleSchema.findNode("node4").findField("location")).find(new Location(1, 1));
                IPeriodAggregationField field1 = node1.node.getField(2);
                assertThat(field1.get(), is((IComponentValue) value3));

                reps[0] = field1.getRepresentation(0, false, true);
                reps[1] = field1.getRepresentation(1, false, false);

                IPeriodAggregationField field2 = node2.node.getField(2);
                assertThat(field2.get(), is((IComponentValue) logValue21));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                AggregationContext context = new AggregationContext();
                context.setTime(4000);
                context.setPeriod(100);
                TestAggregationNode node1 = period.findOrCreateNode(new Location(1, 1), cycleSchema.findNode("node3"));
                IPeriodAggregationField field1 = node1.node.getField(2);
                field1.aggregate(value1, context);

                cycle.addPeriod();
                try {
                    Tests.set(period, "closed", false);
                    field1.onPeriodClosed(period);
                } catch (Exception e) {
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
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestAggregationNode node1 = period.<INodeIndex<Location, TestAggregationNode>>getIndex(
                        cycleSchema.findNode("node3").findField("location")).find(new Location(1, 1));
                IPeriodAggregationField field1 = node1.node.getField(2);

                assertThat(field1.getLog().getCurrent(), is((IAggregationRecord) new AggregationRecord(value11, period.getEndTime(), period.getEndTime() - period.getStartTime())));
                assertThat(com.exametrika.common.utils.Collections.toList(field1.getPeriodRecords().iterator()),
                        is(Arrays.asList((IAggregationRecord) new AggregationRecord(value11, period.getEndTime(), period.getEndTime() - period.getStartTime()))));

                reps[2] = field1.getRepresentation(0, false, true);
                reps[3] = field1.getRepresentation(1, false, false);
            }
        });

        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (int i = 0; i < reps.length; i++)
            builder.add(reps[i]);

        JsonArray ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data6.json", false);
        builder = JsonSerializers.read(builder.toString(), true);
        assertThat(builder.toJson(), is(ethalon));
    }

    public static class TestRootNodeSchemaConfiguration extends PeriodNodeSchemaConfiguration {
        public TestRootNodeSchemaConfiguration(String name) {
            super(name, name, null, new IndexedLocationFieldSchemaConfiguration("location"),
                    Collections.<FieldSchemaConfiguration>singletonList(new BlobStoreFieldSchemaConfiguration("blobStore")), null);
        }

        @Override
        public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            return new PeriodNodeSchema(this, index, fields, documentSchema);
        }

        @Override
        public INodeObject createNode(INode node) {
            return new TestRootNode((IPeriodNode) node);
        }
    }

    public static class TestAggregationNodeSchemaConfiguration extends AggregationNodeSchemaConfiguration {
        private final boolean derived;

        public TestAggregationNodeSchemaConfiguration(String name, AggregationComponentTypeSchemaConfiguration componentType,
                                                      FieldSchemaConfiguration aggregationField, boolean derived) {
            super(name, name, null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField,
                    Arrays.<FieldSchemaConfiguration>asList(new NumericFieldSchemaConfiguration("flags", DataType.INT)));
            this.derived = derived;
        }

        @Override
        public boolean isDerived() {
            return derived;
        }

        @Override
        public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            return new TestAggregationNodeSchema(this, index, fields, documentSchema);
        }

        @Override
        public INodeObject createNode(INode node) {
            return new TestAggregationNode((IPeriodNode) node);
        }
    }

    public static class TestAggregationNodeSchema extends AggregationNodeSchema {
        public TestAggregationNodeSchema(AggregationNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                         IDocumentSchema fullTextSchema) {
            super(configuration, index, fields, fullTextSchema);
        }
    }

    public static class TestRootNode extends PeriodNodeObject {
        public final IPeriodNode node;

        public TestRootNode(IPeriodNode node) {
            super(node);

            this.node = node;
        }
    }

    public static class TestAggregationNode extends AggregationNode {
        public final IPeriodNode node;

        public TestAggregationNode(IPeriodNode node) {
            super(node);

            this.node = node;
        }
    }

    public static class TestDocumentSchemaFactoryConfiguration extends DocumentSchemaFactoryConfiguration {
        @Override
        public DocumentSchemaConfiguration createSchema() {
            return new DocumentSchemaConfiguration("test", Arrays.asList(new StringFieldSchemaConfiguration("message", Enums.of(Option.INDEXED, Option.INDEX_DOCUMENTS,
                    Option.OMIT_NORMS), new StandardAnalyzerSchemaConfiguration()),
                    new com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration("time",
                            com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.LONG, true, true)),
                    new StandardAnalyzerSchemaConfiguration());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDocumentSchemaFactoryConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    public static class TestObjectRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
        public TestObjectRepresentationSchemaConfiguration(String name) {
            super(name);
        }

        @Override
        public IMetricComputer createComputer(ComponentValueSchemaConfiguration schema,
                                              ComponentRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                                              int metricIndex) {
            return new TestComputer();
        }
    }

    public static class TestComputer implements IMetricComputer {
        @Override
        public Object compute(IComponentValue r, IMetricValue v, IComputeContext context) {
            IObjectValue value = (IObjectValue) v;
            return ((JsonObject) value.getObject()).get("message");
        }

        @Override
        public void computeSecondary(IComponentValue r, IMetricValue value, IComputeContext context) {
        }
    }
}
