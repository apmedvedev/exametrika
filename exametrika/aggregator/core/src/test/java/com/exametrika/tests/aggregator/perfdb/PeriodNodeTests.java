/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.config.schema.Documents;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IBlobFieldInitializer;
import com.exametrika.api.exadb.objectdb.fields.IFileField;
import com.exametrika.api.exadb.objectdb.fields.IFileFieldInitializer;
import com.exametrika.api.exadb.objectdb.fields.IIndexField;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.schema.IJsonDiagnostics;
import com.exametrika.common.json.schema.IJsonValidationContext;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonType;
import com.exametrika.common.json.schema.JsonValidationException;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodNode;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.aggregator.schema.PeriodNodeSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.fields.JsonField;
import com.exametrika.impl.exadb.objectdb.fields.SerializableField;
import com.exametrika.impl.exadb.objectdb.fields.StringField;
import com.exametrika.impl.exadb.objectdb.schema.FieldSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonConverterSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonValidatorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNode;
import com.exametrika.tests.exadb.ObjectNodeTests.TestNodeSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link PeriodNodeTests} are tests for period nodes.
 *
 * @author Medvedev-A
 */
public class PeriodNodeTests {
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
    public void testNodes() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2")));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
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
                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");
                INodeIndex<Location, TestPeriodNode> index3 = getIndex(period, "node3", "field");

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                PeriodNode periodNode1 = (PeriodNode) node1.getNode();
                assertThat(((TestPeriodNode) period.findNodeById(periodNode1.getId())).getNode(), is((INode) periodNode1));
                assertThat(periodNode1.getPeriod() == period, is(true));
                assertThat(periodNode1.getLocation(), is(new Location(1, 1)));
                Iterator<Pair<IPeriod, TestPeriodNode>> it = ((IPeriodNode) periodNode1).<TestPeriodNode>getPeriodNodes().iterator();
                Pair<IPeriod, TestPeriodNode> p = it.next();
                assertThat(p.getKey() == period, is(true));
                assertThat(p.getValue().equals(periodNode1.getObject()), is(true));
                assertThat(it.hasNext(), is(false));
                assertThat(periodNode1.getFieldCount(), is(2));
                TestField field1 = periodNode1.getField(1);
                assertThat(field1.field.getNode() == periodNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 1);
                PeriodNode periodNode2 = (PeriodNode) node2.getNode();
                assertThat(periodNode2.getPeriod() == period, is(true));
                assertThat(periodNode2.getLocation(), is(new Location(2, 2)));
                it = ((IPeriodNode) periodNode2).<TestPeriodNode>getPeriodNodes().iterator();
                p = it.next();
                assertThat(p.getKey() == period, is(true));
                assertThat(p.getValue().equals(periodNode2.getObject()), is(true));
                assertThat(it.hasNext(), is(false));
                assertThat(periodNode2.getFieldCount(), is(3));
                field1 = periodNode2.getField(1);
                TestField field2 = periodNode2.getField(2);
                assertThat(field1.field.getNode() == periodNode2, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == periodNode2, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 2);
                PeriodNode periodNode3 = (PeriodNode) node3.getNode();
                assertThat(periodNode3.getPeriod() == period, is(true));
                assertThat(periodNode3.getLocation(), is(new Location(3, 3)));
                it = ((IPeriodNode) periodNode3).<TestPeriodNode>getPeriodNodes().iterator();
                p = it.next();
                assertThat(p.getKey() == period, is(true));
                assertThat(p.getValue().equals(periodNode3.getObject()), is(true));
                assertThat(it.hasNext(), is(false));
                assertThat(periodNode3.getFieldCount(), is(4));
                field1 = periodNode3.getField(1);
                field2 = periodNode3.getField(2);
                TestField field3 = periodNode3.getField(3);
                assertThat(field1.field.getNode() == periodNode3, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == periodNode3, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));
                assertThat(field3.field.getNode() == periodNode3, is(true));
                assertThat(field3.field.getSchema().getConfiguration().getName(), is("field3"));

                List<TestPeriodNode> nodes = new ArrayList<PeriodNodeTests.TestPeriodNode>();
                Iterable<TestPeriodNode> n = period.getNodes();
                for (Iterator<TestPeriodNode> it1 = n.iterator(); it1.hasNext(); )
                    nodes.add(it1.next());
                assertThat(nodes, is(Arrays.asList(node1, node2, node3)));

                assertThat(period.findNode(node1), is(node1));
                assertThat(period.findNode(node2), is(node2));
                assertThat(period.findNode(node3), is(node3));

                assertThat(index1.find(new Location(1, 1)), is(node1));
                assertThat(index2.find(new Location(2, 2)), is(node2));
                assertThat(index3.find(new Location(3, 3)), is(node3));

                assertThat((TestPeriodNode) period.addNode(new Location(1, 1), 0), is(node1));
                assertThat((TestPeriodNode) period.addNode(new Location(2, 2), 1), is(node2));
                assertThat((TestPeriodNode) period.addNode(new Location(3, 3), 2), is(node3));

                cycle.close(null, null, false);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                ICycleSchema cycleSchema = periodSchema.getCycles().get(0);

                PeriodSpace cycle = (PeriodSpace) cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");
                INodeIndex<Location, TestPeriodNode> index3 = getIndex(period, "node3", "field");

                TestPeriodNode node1 = index1.find(new Location(1, 1));
                PeriodNode periodNode1 = (PeriodNode) node1.getNode();
                assertThat(periodNode1.getPeriod() == period, is(true));
                assertThat(periodNode1.getLocation(), is(new Location(1, 1)));
                Iterator<Pair<IPeriod, TestPeriodNode>> it = ((IPeriodNode) periodNode1).<TestPeriodNode>getPeriodNodes().iterator();
                Pair<IPeriod, TestPeriodNode> p = it.next();
                assertThat(p.getKey() == period, is(true));
                assertThat(p.getValue().equals(periodNode1.getObject()), is(true));
                assertThat(it.hasNext(), is(false));
                assertThat(periodNode1.getFieldCount(), is(2));
                TestField field1 = periodNode1.getField(1);
                assertThat(field1.field.getNode() == periodNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                TestPeriodNode node2 = index2.find(new Location(2, 2));
                PeriodNode periodNode2 = (PeriodNode) node2.getNode();
                assertThat(periodNode2.getPeriod() == period, is(true));
                assertThat(periodNode2.getLocation(), is(new Location(2, 2)));
                it = ((IPeriodNode) periodNode2).<TestPeriodNode>getPeriodNodes().iterator();
                p = it.next();
                assertThat(p.getKey() == period, is(true));
                assertThat(p.getValue().equals(periodNode2.getObject()), is(true));
                assertThat(it.hasNext(), is(false));
                assertThat(periodNode2.getFieldCount(), is(3));
                field1 = periodNode2.getField(1);
                TestField field2 = periodNode2.getField(2);
                assertThat(field1.field.getNode() == periodNode2, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == periodNode2, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));

                TestPeriodNode node3 = index3.find(new Location(3, 3));
                PeriodNode periodNode3 = (PeriodNode) node3.getNode();
                assertThat(periodNode3.getPeriod() == period, is(true));
                assertThat(periodNode3.getLocation(), is(new Location(3, 3)));
                it = ((IPeriodNode) periodNode3).<TestPeriodNode>getPeriodNodes().iterator();
                p = it.next();
                assertThat(p.getKey() == period, is(true));
                assertThat(p.getValue().equals(periodNode3.getObject()), is(true));
                assertThat(it.hasNext(), is(false));
                assertThat(periodNode3.getFieldCount(), is(4));
                field1 = periodNode3.getField(1);
                assertThat(periodNode3.getField(1) == field1, is(true));
                field2 = periodNode3.getField(2);
                TestField field3 = periodNode3.getField(3);
                assertThat(field1.field.getNode() == periodNode3, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
                assertThat(field2.field.getNode() == periodNode3, is(true));
                assertThat(field2.field.getSchema().getConfiguration().getName(), is("field2"));
                assertThat(field3.field.getNode() == periodNode3, is(true));
                assertThat(field3.field.getSchema().getConfiguration().getName(), is("field3"));

                List<PeriodNodeTests.TestPeriodNode> nodes = new ArrayList<PeriodNodeTests.TestPeriodNode>();
                Iterable<TestPeriodNode> n = period.getNodes();
                for (Iterator<TestPeriodNode> it2 = n.iterator(); it2.hasNext(); )
                    nodes.add(it2.next());
                assertThat(nodes, is(Arrays.asList(node1, node2, node3)));

                assertThat(period.findNode(node1), is(node1));
                assertThat(period.findNode(node2), is(node2));
                assertThat(period.findNode(node3), is(node3));

                assertThat(index1.find(new Location(1, 1)), is(node1));
                assertThat(index2.find(new Location(2, 2)), is(node2));
                assertThat(index3.find(new Location(3, 3)), is(node3));
            }
        });
    }

    @Test
    public void testPeriodIterator() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 500, 4, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });
        database.close();

        builder.setTimerPeriod(100);
        configuration = builder.toConfiguration();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        final long[] ids = new long[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        Threads.sleep(500);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();
                TestPeriodNode node2 = period.addNode(new Location(1, 1), 0);
                Iterator<Pair<IPeriod, TestPeriodNode>> it = ((IPeriodNode) node2.node).<TestPeriodNode>getPeriodNodes().iterator();
                Iterator<Pair<IPeriod, TestPeriodNode>> it2 = ((IPeriodNode) node2.node).<TestPeriodNode>getPeriodNodes().iterator();
                Pair<IPeriod, TestPeriodNode> p = it.next();
                Pair<IPeriod, TestPeriodNode> p2 = it2.next();
                assertThat(((Period) p.getKey()).getPeriodIndex(), is(1));
                assertThat(p.getValue() == node2, is(true));
                assertThat(p.getKey() == p2.getKey(), is(true));
                assertThat(p.getValue() == p2.getValue(), is(true));
                p = it.next();
                p2 = it2.next();
                assertThat(((Period) p.getKey()).getPeriodIndex(), is(0));
                assertThat(p.getValue().node.getId(), is(ids[0]));
                assertThat(it.hasNext(), is(false));
            }
        });
    }

    @Test
    public void testPerformance() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2")));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2,
                        nodeConfiguration3)), null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final ScopeName[] scopes = new ScopeName[COUNT];
        final MetricName[] metrics = new MetricName[COUNT];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                for (int i = 0; i < COUNT; i++) {
                    scopes[i] = ScopeName.get("scope" + i);
                    metrics[i] = MetricName.get("metric" + i);
                }

                int p = 0;
                long t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestPeriodNode node = period.addNode(new Location(i + 1, i + 1), 0);
                        TestField field = node.node.getField(1);
                        p += field.field.getSchema().getIndex();
                    }

                System.out.println("write add node: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                Iterable<TestPeriodNode> n = period.getNodes();
                for (Iterator<TestPeriodNode> it = n.iterator(); it.hasNext(); ) {
                    TestPeriodNode node = it.next();
                    TestField field = node.node.getField(1);
                    p += field.field.getSchema().getIndex();
                }

                System.out.println("write period iterator: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestPeriodNode node = index1.find(new Location(i + 1, i + 1));
                        TestField field = node.node.getField(1);
                        p += field.field.getSchema().getIndex();
                    }
                System.out.println("write find: " + (Times.getCurrentTime() - t) + " " + p);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                int p = 0;
                long t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestPeriodNode node = period.addNode(new Location(i + 1, i + 1), 0);
                        TestField field = node.node.getField(1);
                        p += field.field.getSchema().getIndex();
                    }

                System.out.println("write add node2: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                Iterable<TestPeriodNode> n = period.getNodes();
                for (Iterator<TestPeriodNode> it = n.iterator(); it.hasNext(); ) {
                    TestPeriodNode node = it.next();
                    TestField field = node.node.getField(1);
                    p += field.field.getSchema().getIndex();
                }

                System.out.println("write period iterator2: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                for (int k = 0; k < 1000; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestPeriodNode node = index1.find(new Location(i + 1, i + 1));
                        TestField field = node.node.getField(1);
                        p += field.field.getSchema().getIndex();
                    }
                System.out.println("write find2: " + (Times.getCurrentTime() - t) + " " + p);

                cycle.close(null, null, false);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                int p = 0;
                long t = Times.getCurrentTime();
                Iterable<TestPeriodNode> n = period.getNodes();
                for (Iterator<TestPeriodNode> it = n.iterator(); it.hasNext(); ) {
                    TestPeriodNode node = it.next();
                    TestField field = node.node.getField(1);
                    p += field.field.getSchema().getIndex();
                }

                System.out.println("read period iterator: " + (Times.getCurrentTime() - t) + " " + p);

                t = Times.getCurrentTime();
                for (int k = 0; k < 100; k++)
                    for (int i = 0; i < COUNT; i++) {
                        TestPeriodNode node = index1.find(new Location(i + 1, i + 1));
                        TestField field = node.node.getField(1);
                        p += field.field.getSchema().getIndex();
                    }
                System.out.println("read find: " + (Times.getCurrentTime() - t) + " " + p);
            }
        });
    }

    @Test
    public void testPrimitiveFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new PrimitiveFieldSchemaConfiguration("field1", DataType.BYTE),
                        new PrimitiveFieldSchemaConfiguration("field2", DataType.SHORT),
                        new PrimitiveFieldSchemaConfiguration("field3", DataType.CHAR),
                        new PrimitiveFieldSchemaConfiguration("field4", DataType.INT),
                        new PrimitiveFieldSchemaConfiguration("field5", DataType.LONG),
                        new PrimitiveFieldSchemaConfiguration("field6", DataType.BOOLEAN),
                        new PrimitiveFieldSchemaConfiguration("field7", DataType.DOUBLE)));
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

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                assertThat(node1.node.isModified(), is(true));

                IPrimitiveField field1 = node1.node.getField(1);
                field1.setByte((byte) 123);
                IPrimitiveField field2 = node1.node.getField(2);
                field2.setShort((short) 45678);
                IPrimitiveField field3 = node1.node.getField(3);
                field3.setChar('A');
                IPrimitiveField field4 = node1.node.getField(4);
                field4.setInt(Integer.MAX_VALUE);
                IPrimitiveField field5 = node1.node.getField(5);
                field5.setLong(Long.MAX_VALUE);
                IPrimitiveField field6 = node1.node.getField(6);
                field6.setBoolean(true);
                IPrimitiveField field7 = node1.node.getField(7);
                field7.setDouble(Double.MAX_VALUE);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                TestPeriodNode node1 = index1.find(new Location(1, 1));
                assertThat(node1.node.isModified(), is(false));
                IPrimitiveField field1 = node1.node.getField(1);
                assertThat(field1.getByte(), is((byte) 123));
                field1.setByte((byte) 23);
                IPrimitiveField field2 = node1.node.getField(2);
                assertThat(field2.getShort(), is((short) 45678));
                field2.setShort((short) 5678);
                IPrimitiveField field3 = node1.node.getField(3);
                assertThat(field3.getChar(), is('A'));
                field3.setChar('B');
                IPrimitiveField field4 = node1.node.getField(4);
                assertThat(field4.getInt(), is(Integer.MAX_VALUE));
                field4.setInt(Integer.MAX_VALUE - 1);
                IPrimitiveField field5 = node1.node.getField(5);
                assertThat(field5.getLong(), is(Long.MAX_VALUE));
                field5.setLong(Long.MAX_VALUE - 1);
                IPrimitiveField field6 = node1.node.getField(6);
                assertThat(field6.getBoolean(), is(true));
                field6.setBoolean(false);
                IPrimitiveField field7 = node1.node.getField(7);
                assertThat(field7.getDouble(), is(Double.MAX_VALUE));
                field7.setDouble(Double.MAX_VALUE - 1);
                assertThat(node1.node.isModified(), is(true));

                cycle.close(null, null, false);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                    TestPeriodNode node1 = index1.find(new Location(1, 1));
                    IPrimitiveField field1 = node1.node.getField(1);
                    assertThat(field1.getByte(), is((byte) 23));
                    IPrimitiveField field2 = node1.node.getField(2);
                    assertThat(field2.getShort(), is((short) 5678));
                    IPrimitiveField field3 = node1.node.getField(3);
                    assertThat(field3.getChar(), is('B'));
                    IPrimitiveField field4 = node1.node.getField(4);
                    assertThat(field4.getInt(), is(Integer.MAX_VALUE - 1));
                    IPrimitiveField field5 = node1.node.getField(5);
                    assertThat(field5.getLong(), is(Long.MAX_VALUE - 1));
                    IPrimitiveField field6 = node1.node.getField(6);
                    assertThat(field6.getBoolean(), is(false));
                    IPrimitiveField field7 = node1.node.getField(7);
                    assertThat(field7.getDouble(), is(Double.MAX_VALUE - 1));

                    final IPrimitiveField f1 = field1;
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            f1.setByte((byte) 123);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testFileFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new FileFieldSchemaConfiguration("field1")));
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

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                IFileField field1 = node1.node.getField(1);
                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                IFileField field2 = node2.node.getField(1);
                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    writeRegion(i, page1.getWriteRegion());

                    IRawPage page2 = field2.getPage(i);
                    writeRegion(i, page2.getWriteRegion());
                }
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

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                IFileField field1 = node1.node.getField(1);
                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                IFileField field2 = node2.node.getField(1);

                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i));

                    IRawPage page2 = field2.getPage(i);
                    checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i));

                    writeRegion(i + 1, page1.getWriteRegion());
                    writeRegion(i + 2, page2.getWriteRegion());
                }

                assertThat(node1.node.isModified(), is(false));
                assertThat(node2.node.isModified(), is(false));

                cycle.close(null, null, false);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                TestPeriodNode node1 = index1.find(new Location(1, 1));
                IFileField field1 = node1.node.getField(1);
                TestPeriodNode node2 = index1.find(new Location(2, 2));
                IFileField field2 = node2.node.getField(1);

                for (int i = 0; i < 10; i++) {
                    IRawPage page1 = field1.getPage(i);
                    checkRegion(page1.getReadRegion(), createBuffer(page1.getSize(), i + 1));

                    IRawPage page2 = field2.getPage(i);
                    checkRegion(page2.getReadRegion(), createBuffer(page2.getSize(), i + 2));
                }
            }
        });
    }

    @Test
    public void testFileFieldsWithBinding() throws Throwable {
        database.close();

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(new File(tempDir, "db1").getPath());
        builder.addPath(new File(tempDir, "db2").getPath());
        builder.addPath(new File(tempDir, "db3").getPath());
        builder.setTimerPeriod(1000000);

        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();

        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new FileFieldSchemaConfiguration("field1", "field1", null, 0, 0, "fd1",
                                PageType.NORMAL, false, java.util.Collections.<String, String>emptyMap()),
                        new FileFieldSchemaConfiguration("field2", "field2", null, 1, 0, "fd2",
                                PageType.NORMAL, false, java.util.Collections.<String, String>emptyMap())));
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

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                IFileField field11 = node1.node.getField(1);
                IFileField field12 = node1.node.getField(2);
                TestPeriodNode node2 = period.findOrCreateNode(new Location(2, 2), period.getSpace().getSchema().getNodes().get(0), 2, 300, 0, "fd3");
                IFileField field21 = node2.node.getField(1);
                IFileField field22 = node2.node.getField(2);
                for (int i = 0; i < 10; i++) {
                    IRawPage page11 = field11.getPage(i);
                    assertThat(page11.getSize(), is(0x4000));
                    assertThat(page11.getFile().getPath().contains("db1"), is(true));
                    assertThat(page11.getFile().getPath().contains("fd1"), is(true));

                    writeRegion(i, page11.getWriteRegion());
                    IRawPage page12 = field12.getPage(i);
                    assertThat(page12.getSize(), is(0x4000));
                    assertThat(page12.getFile().getPath().contains("db2"), is(true));
                    assertThat(page12.getFile().getPath().contains("fd2"), is(true));
                    writeRegion(i + 1, page12.getWriteRegion());

                    IRawPage page21 = field21.getPage(i);
                    assertThat(page21.getSize(), is(0x4000));
                    assertThat(page21.getFile().getPath().contains("db3"), is(true));
                    assertThat(page21.getFile().getPath().contains("fd3"), is(true));
                    writeRegion(i, page21.getWriteRegion());
                    IRawPage page22 = field22.getPage(i);
                    assertThat(page22.getSize(), is(0x4000));
                    assertThat(page22.getFile().getPath().contains("db2"), is(true));
                    assertThat(page22.getFile().getPath().contains("fd2"), is(true));
                    writeRegion(i + 1, page22.getWriteRegion());
                }
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                IFileField field11 = node1.node.getField(1);
                IFileField field12 = node1.node.getField(2);
                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                IFileField field21 = node2.node.getField(1);
                IFileField field22 = node2.node.getField(2);

                for (int i = 0; i < 10; i++) {
                    IRawPage page11 = field11.getPage(i);
                    assertThat(page11.getSize(), is(0x4000));
                    assertThat(page11.getFile().getPath().contains("db1"), is(true));
                    assertThat(page11.getFile().getPath().contains("fd1"), is(true));
                    checkRegion(page11.getReadRegion(), createBuffer(page11.getSize(), i));
                    IRawPage page12 = field12.getPage(i);
                    assertThat(page12.getSize(), is(0x4000));
                    assertThat(page12.getFile().getPath().contains("db2"), is(true));
                    assertThat(page12.getFile().getPath().contains("fd2"), is(true));
                    checkRegion(page12.getReadRegion(), createBuffer(page12.getSize(), i + 1));

                    IRawPage page21 = field21.getPage(i);
                    assertThat(page21.getSize(), is(0x4000));
                    assertThat(page21.getFile().getPath().contains("db3"), is(true));
                    assertThat(page21.getFile().getPath().contains("fd3"), is(true));
                    checkRegion(page21.getReadRegion(), createBuffer(page21.getSize(), i));
                    IRawPage page22 = field22.getPage(i);
                    assertThat(page22.getFile().getPath().contains("db2"), is(true));
                    assertThat(page22.getFile().getPath().contains("fd2"), is(true));
                    checkRegion(page22.getReadRegion(), createBuffer(page22.getSize(), i + 1));

                    writeRegion(i + 1, page11.getWriteRegion());
                    writeRegion(i + 2, page12.getWriteRegion());
                    writeRegion(i + 3, page21.getWriteRegion());
                    writeRegion(i + 4, page22.getWriteRegion());
                }

                assertThat(node1.node.isModified(), is(false));
                assertThat(node2.node.isModified(), is(false));

                cycle.close(null, null, false);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                TestPeriodNode node1 = index1.find(new Location(1, 1));
                IFileField field11 = node1.node.getField(1);
                IFileField field12 = node1.node.getField(2);
                TestPeriodNode node2 = index1.find(new Location(2, 2));
                IFileField field21 = node2.node.getField(1);
                IFileField field22 = node2.node.getField(2);

                for (int i = 0; i < 10; i++) {
                    IRawPage page11 = field11.getPage(i);
                    checkRegion(page11.getReadRegion(), createBuffer(page11.getSize(), i + 1));
                    IRawPage page12 = field12.getPage(i);
                    checkRegion(page12.getReadRegion(), createBuffer(page12.getSize(), i + 2));

                    final IRawPage page21 = field21.getPage(i);
                    checkRegion(page21.getReadRegion(), createBuffer(page21.getSize(), i + 3));
                    final IRawPage page22 = field22.getPage(i);
                    checkRegion(page22.getReadRegion(), createBuffer(page22.getSize(), i + 4));
                }
            }
        });
    }

    @Test
    public void testReferenceFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final TestPeriodNode[] nodes = new TestPeriodNode[COUNT];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(COUNT + 1, 1), 0);
                IReferenceField<TestPeriodNode> field1 = node1.node.getField(1);
                TestPeriodNode node2 = period.addNode(new Location(COUNT + 2, 2), 0);
                IReferenceField<TestPeriodNode> field2 = node2.node.getField(1);
                TestPeriodNode node3 = period.addNode(new Location(COUNT + 3, 3), 0);
                IReferenceField<TestPeriodNode> field3 = node3.node.getField(1);
                TestPeriodNode node4 = period.addNode(new Location(COUNT + 4, 4), 0);
                IReferenceField<TestPeriodNode> field4 = node4.node.getField(1);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = period.addNode(new Location(i + 1, i + 1), 1);

                long t = Times.getCurrentTime();

                for (int i = 0; i < nodes.length; i++) {
                    TestPeriodNode node = nodes[i];
                    ISingleReferenceField<TestPeriodNode> field = node.node.getField(1);

                    if ((i % 2) == 0) {
                        field1.add(node);
                        field.set(node1);
                    } else {
                        field2.add(node);
                        field.set(node2);
                    }
                }

                field3.add(nodes[0]);
                field3.add(nodes[1]);
                field3.add(nodes[2]);

                field4.add(nodes[0]);
                field4.add(nodes[1]);
                field4.add(nodes[2]);

                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
                System.out.println("add reference: " + (Times.getCurrentTime() - t));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node3 = period.addNode(new Location(COUNT + 3, 3), 0);
                IReferenceField<TestPeriodNode> field3 = node3.node.getField(1);
                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                    INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");

                    TestPeriodNode child1 = index2.find(new Location(1, 1));
                    TestPeriodNode child4 = index2.find(new Location(4, 4));
                    TestPeriodNode child5 = index2.find(new Location(5, 5));

                    ISingleReferenceField<TestPeriodNode> field11 = child1.node.getField(1);
                    field11.set(null);

                    TestPeriodNode node1 = index1.find(new Location(COUNT + 1, 1));
                    field11.set(node1);
                    TestPeriodNode node2 = index1.find(new Location(COUNT + 2, 2));
                    IReferenceField<TestPeriodNode> field2 = node2.node.getField(1);
                    field2.add(child1);
                    assertThat(node2.node.isModified(), is(true));

                    TestPeriodNode node3 = index1.find(new Location(COUNT + 3, 3));
                    IReferenceField<TestPeriodNode> field3 = node3.node.getField(1);
                    final Iterator<TestPeriodNode> it1 = field3.iterator();
                    TestPeriodNode node4 = index1.find(new Location(COUNT + 4, 4));
                    IReferenceField<TestPeriodNode> field4 = node4.node.getField(1);
                    Iterator<TestPeriodNode> it4 = field4.iterator();
                    for (int i = 0; i < 2; i++) {
                        assertThat(it1.next(), is(nodes[i]));
                        assertThat(it4.next(), is(nodes[i]));
                    }

                    field3.add(child4);
                    field3.add(child5);

                    field4.add(child4);
                    field4.add(child5);

                    new Expected(ConcurrentModificationException.class, new Runnable() {
                        @Override
                        public void run() {
                            it1.next();
                        }
                    });

                    assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2], nodes[3], nodes[4])));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                TestPeriodNode node4 = index1.find(new Location(COUNT + 4, 4));
                IReferenceField<TestPeriodNode> field4 = node4.node.getField(1);
                assertThat(new HashSet<TestPeriodNode>(Collections.toList(field4.iterator())), is(Collections.asSet(nodes[0], nodes[1], nodes[3], nodes[4], nodes[2])));

                cycle.close(null, null, false);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");

                TestPeriodNode node1 = index1.find(new Location(COUNT + 1, 1));
                IReferenceField<TestPeriodNode> field1 = node1.node.getField(1);
                List<TestPeriodNode> refs1 = Collections.toList(field1.iterator());
                TestPeriodNode node2 = index1.find(new Location(COUNT + 2, 2));
                IReferenceField<TestPeriodNode> field2 = node2.node.getField(1);
                List<TestPeriodNode> refs2 = Collections.toList(field2.iterator());

                for (int i = 0; i < nodes.length; i++) {
                    TestPeriodNode node = index2.find(new Location(i + 1, i + 1));
                    ISingleReferenceField<TestPeriodNode> field = node.node.getField(1);

                    if ((i % 2) == 0) {
                        assertThat(field.get().equals(node1), is(true));
                        assertThat(refs1.indexOf(node) != -1, is(true));
                    } else {
                        assertThat(field.get().equals(node2), is(true));
                        assertThat(refs2.indexOf(node) != -1, is(true));
                    }
                }
            }
        });
    }

    @Test
    public void testReferenceFieldStableOrder() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", true)));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new SingleReferenceFieldSchemaConfiguration("parent", "node1")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final TestPeriodNode[] nodes = new TestPeriodNode[COUNT];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(2 * COUNT + 1, 1), 0);
                IReferenceField<TestPeriodNode> field1 = node1.node.getField(1);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = period.addNode(new Location(i + 1, i + 1), 1);

                for (int i = 0; i < nodes.length; i++) {
                    TestPeriodNode node = nodes[i];
                    ISingleReferenceField<TestPeriodNode> field = node.node.getField(1);

                    field1.add(node);
                    field.set(node1);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);
                INodeSchema nodeSchema = cycleSchema.getNodes().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.findNode(new Location(2 * COUNT + 1, 1), nodeSchema);
                IReferenceField<TestPeriodNode> field1 = node1.node.getField(1);
                int i = 0;
                for (TestPeriodNode node : field1) {
                    assertThat(((IPeriodNode) node.node).getLocation(), is(new Location(i + 1, i + 1)));
                    i++;
                }

                assertThat(i, is(COUNT));
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
                INodeSchema nodeSchema = cycleSchema.getNodes().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.findNode(new Location(2 * COUNT + 1, 1), nodeSchema);
                IReferenceField<TestPeriodNode> field1 = node1.node.getField(1);

                for (int i = 0; i < 10; i++)
                    field1.add((TestPeriodNode) period.addNode(new Location(COUNT + 1 + i, COUNT + 1 + i), 1));

                int i = 0;
                for (TestPeriodNode node : field1) {
                    assertThat(((IPeriodNode) node.node).getLocation(), is(new Location(i + 1, i + 1)));
                    i++;
                }

                assertThat(i, is(COUNT + 10));
            }
        });
    }

    @Test
    public void testExternalReferenceFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("children", "node2", "test2.space2")));
        NodeSchemaConfiguration nodeConfiguration2 = new TestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedNumericFieldSchemaConfiguration("field", DataType.INT)));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 1000000, 2, false, null)), 0, 0);
        ObjectSpaceSchemaConfiguration space2 = new ObjectSpaceSchemaConfiguration("space2", "space2", "", new HashSet(Arrays.asList(
                nodeConfiguration2)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test1", new HashSet(Arrays.asList(space1)));
        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test2", new HashSet(Arrays.asList(space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0),
                        Collections.asSet(configuration1, configuration2)), null);
            }
        });

        final int COUNT = 10000;
        final TestNode[] nodes = new TestNode[COUNT];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                TestPeriodNode node1 = period.addNode(new Location(COUNT + 1, 1), 0);
                IReferenceField<TestNode> field1 = node1.node.getField(1);
                TestPeriodNode node2 = period.addNode(new Location(COUNT + 2, 2), 0);
                IReferenceField<TestNode> field2 = node2.node.getField(1);
                TestPeriodNode node3 = period.addNode(new Location(COUNT + 3, 3), 0);
                IReferenceField<TestNode> field3 = node3.node.getField(1);
                TestPeriodNode node4 = period.addNode(new Location(COUNT + 4, 4), 0);
                IReferenceField<TestNode> field4 = node4.node.getField(1);

                for (int i = 0; i < nodes.length; i++)
                    nodes[i] = space2.addNode(i + 1, 0);

                long t = Times.getCurrentTime();

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = nodes[i];

                    if ((i % 2) == 0)
                        field1.add(node);
                    else
                        field2.add(node);
                }

                field3.add(nodes[0]);
                field3.add(nodes[1]);
                field3.add(nodes[2]);

                field4.add(nodes[0]);
                field4.add(nodes[1]);
                field4.add(nodes[2]);

                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
                System.out.println("add reference: " + (Times.getCurrentTime() - t));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node3 = period.addNode(new Location(COUNT + 3, 3), 0);
                IReferenceField<TestNode> field3 = node3.node.getField(1);
                assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2])));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                    ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                    INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                    INodeIndex<Integer, TestNode> index2 = getIndex(space2, "node2", "field");

                    TestNode child1 = index2.find(1);
                    TestNode child4 = index2.find(4);
                    TestNode child5 = index2.find(5);

                    TestPeriodNode node2 = index1.find(new Location(COUNT + 2, 2));
                    IReferenceField<TestNode> field2 = node2.node.getField(1);
                    field2.add(child1);
                    assertThat(node2.node.isModified(), is(true));

                    TestPeriodNode node3 = index1.find(new Location(COUNT + 3, 3));
                    IReferenceField<TestNode> field3 = node3.node.getField(1);
                    final Iterator<TestNode> it1 = field3.iterator();
                    TestPeriodNode node4 = index1.find(new Location(COUNT + 4, 4));
                    IReferenceField<TestNode> field4 = node4.node.getField(1);
                    Iterator<TestNode> it4 = field4.iterator();
                    for (int i = 0; i < 2; i++) {
                        assertThat(it1.next(), is(nodes[i]));
                        assertThat(it4.next(), is(nodes[i]));
                    }

                    field3.add(child4);
                    field3.add(child5);

                    field4.add(child4);
                    field4.add(child5);

                    new Expected(ConcurrentModificationException.class, new Runnable() {
                        @Override
                        public void run() {
                            it1.next();
                        }
                    });

                    assertThat(Collections.toList(field3.iterator()), is(Arrays.asList(nodes[0], nodes[1], nodes[2], nodes[3], nodes[4])));
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");

                TestPeriodNode node4 = index1.find(new Location(COUNT + 4, 4));
                IReferenceField<TestNode> field4 = node4.node.getField(1);
                assertThat(new HashSet<TestNode>(Collections.toList(field4.iterator())), is(Collections.asSet(nodes[0], nodes[1], nodes[3], nodes[4], nodes[2])));

                cycle.close(null, null, false);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test1").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Integer, TestNode> index2 = getIndex(space2, "node2", "field");

                TestPeriodNode node1 = index1.find(new Location(COUNT + 1, 1));
                IReferenceField<TestPeriodNode> field1 = node1.node.getField(1);
                List<TestPeriodNode> refs1 = Collections.toList(field1.iterator());
                TestPeriodNode node2 = index1.find(new Location(COUNT + 2, 2));
                IReferenceField<TestPeriodNode> field2 = node2.node.getField(1);
                List<TestPeriodNode> refs2 = Collections.toList(field2.iterator());

                for (int i = 0; i < nodes.length; i++) {
                    TestNode node = index2.find(i + 1);

                    if ((i % 2) == 0)
                        assertThat(refs1.indexOf(node) != -1, is(true));
                    else
                        assertThat(refs2.indexOf(node) != -1, is(true));
                }
            }
        });
    }

    @Test
    public void testComplexFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2")));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final ByteArray buf = createBuffer(23456, 123);
        final String str = createString(34567, 9876);
        final long[] areaBlockIndex = new long[2];
        final int[] areaOffset = new int[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                    TestField field11 = node1.node.getField(1);

                    TestPeriodNode node2 = period.addNode(new Location(2, 2), 1);
                    TestField field21 = node2.node.getField(1);
                    TestField field22 = node2.node.getField(2);

                    TestPeriodNode node3 = period.addNode(new Location(3, 3), 2);
                    TestField field31 = node3.node.getField(1);
                    TestField field32 = node3.node.getField(2);
                    TestField field33 = node3.node.getField(3);

                    IFieldSerialization serialization11 = field11.field.createSerialization();
                    IFieldSerialization serialization21 = field21.field.createSerialization();
                    IFieldSerialization serialization22 = field22.field.createSerialization();
                    IFieldSerialization serialization31 = field31.field.createSerialization();
                    IFieldSerialization serialization32 = field32.field.createSerialization();
                    IFieldSerialization serialization33 = field33.field.createSerialization();

                    for (int i = 0; i < 10000; i++) {
                        if (i == 5000) {
                            areaBlockIndex[0] = serialization11.getAreaId();
                            areaOffset[0] = serialization11.getAreaOffset();
                        }
                        serialization11.writeInt(i);
                        serialization21.writeInt(i);
                        serialization22.writeInt(i);
                        serialization31.writeInt(i);
                        serialization32.writeInt(i);
                        serialization33.writeInt(i);
                    }

                    areaBlockIndex[1] = serialization11.getAreaId();
                    areaOffset[1] = serialization11.getAreaOffset();
                    serialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
                    serialization11.writeInt(5000);
                    serialization11.setPosition(areaBlockIndex[1], areaOffset[1]);

                    serialization11.writeByteArray(buf);
                    serialization21.writeByteArray(buf);
                    serialization22.writeByteArray(buf);
                    serialization31.writeByteArray(buf);
                    serialization32.writeByteArray(buf);
                    serialization33.writeByteArray(buf);

                    serialization11.writeString(str);
                    serialization21.writeString(str);
                    serialization22.writeString(str);
                    serialization31.writeString(str);
                    serialization32.writeString(str);
                    serialization33.writeString(str);

                    IFieldDeserialization deserialization11 = field11.field.createDeserialization();
                    IFieldDeserialization deserialization21 = field21.field.createDeserialization();
                    IFieldDeserialization deserialization22 = field22.field.createDeserialization();
                    IFieldDeserialization deserialization31 = field31.field.createDeserialization();
                    IFieldDeserialization deserialization32 = field32.field.createDeserialization();
                    IFieldDeserialization deserialization33 = field33.field.createDeserialization();

                    for (int i = 0; i < 10000; i++) {
                        assertThat(deserialization33.readInt(), is(i));
                        assertThat(deserialization32.readInt(), is(i));
                        assertThat(deserialization31.readInt(), is(i));
                        assertThat(deserialization22.readInt(), is(i));
                        assertThat(deserialization21.readInt(), is(i));
                        assertThat(deserialization11.readInt(), is(i));
                    }

                    assertThat(deserialization33.readByteArray(), is(buf));
                    assertThat(deserialization33.readString(), is(str));
                    assertThat(deserialization32.readByteArray(), is(buf));
                    assertThat(deserialization32.readString(), is(str));
                    assertThat(deserialization31.readByteArray(), is(buf));
                    assertThat(deserialization31.readString(), is(str));
                    assertThat(deserialization22.readByteArray(), is(buf));
                    assertThat(deserialization22.readString(), is(str));
                    assertThat(deserialization21.readByteArray(), is(buf));
                    assertThat(deserialization21.readString(), is(str));
                    assertThat(deserialization11.readByteArray(), is(buf));
                    assertThat(deserialization11.readString(), is(str));

                    areaBlockIndex[1] = deserialization11.getAreaId();
                    areaOffset[1] = deserialization11.getAreaOffset();

                    deserialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
                    assertThat(deserialization11.readInt(), is(5000));

                    deserialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
                    final IFieldDeserialization d1 = deserialization11;
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 100; i++)
                                assertThat(d1.readInt(), is(0));
                        }
                    });
                    cycle.close(null, null, false);
                } catch (Throwable e) {
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
                try {
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                    INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");
                    INodeIndex<Location, TestPeriodNode> index3 = getIndex(period, "node3", "field");

                    TestPeriodNode node1 = index1.find(new Location(1, 1));
                    TestField field11 = node1.node.getField(1);
                    TestPeriodNode node2 = index2.find(new Location(2, 2));
                    TestField field21 = node2.node.getField(1);
                    TestField field22 = node2.node.getField(2);
                    TestPeriodNode node3 = index3.find(new Location(3, 3));
                    TestField field31 = node3.node.getField(1);
                    TestField field32 = node3.node.getField(2);
                    TestField field33 = node3.node.getField(3);

                    IFieldDeserialization deserialization11 = field11.field.createDeserialization();
                    IFieldDeserialization deserialization21 = field21.field.createDeserialization();
                    IFieldDeserialization deserialization22 = field22.field.createDeserialization();
                    IFieldDeserialization deserialization31 = field31.field.createDeserialization();
                    IFieldDeserialization deserialization32 = field32.field.createDeserialization();
                    IFieldDeserialization deserialization33 = field33.field.createDeserialization();

                    for (int i = 0; i < 10000; i++) {
                        assertThat(deserialization11.readInt(), is(i));
                        assertThat(deserialization21.readInt(), is(i));
                        assertThat(deserialization22.readInt(), is(i));
                        assertThat(deserialization31.readInt(), is(i));
                        assertThat(deserialization32.readInt(), is(i));
                        assertThat(deserialization33.readInt(), is(i));
                    }

                    assertThat(deserialization33.readByteArray(), is(buf));
                    assertThat(deserialization33.readString(), is(str));
                    assertThat(deserialization32.readByteArray(), is(buf));
                    assertThat(deserialization32.readString(), is(str));
                    assertThat(deserialization31.readByteArray(), is(buf));
                    assertThat(deserialization31.readString(), is(str));
                    assertThat(deserialization22.readByteArray(), is(buf));
                    assertThat(deserialization22.readString(), is(str));
                    assertThat(deserialization21.readByteArray(), is(buf));
                    assertThat(deserialization21.readString(), is(str));
                    assertThat(deserialization11.readByteArray(), is(buf));
                    assertThat(deserialization11.readString(), is(str));

                    deserialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
                    assertThat(deserialization11.readInt(), is(5000));

                    deserialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
                    final IFieldDeserialization d2 = deserialization11;
                    new Expected(EndOfStreamException.class, new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 100; i++)
                                assertThat(d2.readInt(), is(0));
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    @Test
    public void testStringFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(new StringFieldSchemaConfiguration("field1", 256)));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", 10000), new StringFieldSchemaConfiguration("field2", 10000)));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new StringFieldSchemaConfiguration("field1", 10000), new StringFieldSchemaConfiguration("field2", 10000),
                        new StringFieldSchemaConfiguration("field3", 10000)));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final String str1 = createString(10000, 9876);
        final String str2 = createString(34, 98);
        final String str3 = createString(3456, 987);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                StringField field11 = node1.node.getField(1);

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 1);
                StringField field21 = node2.node.getField(1);
                StringField field22 = node2.node.getField(2);

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 2);
                StringField field31 = node3.node.getField(1);
                StringField field32 = node3.node.getField(2);
                StringField field33 = node3.node.getField(3);

                for (int i = 0; i < 10000; i++) {
                    field11.set(str1);
                    field21.set(str2);
                    field22.set(str3);
                    field31.set(str1);
                    field32.set(str2);
                    field33.set(str3);
                }

                for (int i = 0; i < 10000; i++) {
                    assertThat(field33.get(), is(str3));
                    assertThat(field32.get(), is(str2));
                    assertThat(field31.get(), is(str1));
                    assertThat(field22.get(), is(str3));
                    assertThat(field21.get(), is(str2));
                    assertThat(field11.get(), is(str1));
                }

                cycle.close(null, null, false);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");
                INodeIndex<Location, TestPeriodNode> index3 = getIndex(period, "node3", "field");

                TestPeriodNode node1 = index1.find(new Location(1, 1));
                StringField field11 = node1.node.getField(1);
                TestPeriodNode node2 = index2.find(new Location(2, 2));
                StringField field21 = node2.node.getField(1);
                StringField field22 = node2.node.getField(2);
                TestPeriodNode node3 = index3.find(new Location(3, 3));
                StringField field31 = node3.node.getField(1);
                StringField field32 = node3.node.getField(2);
                StringField field33 = node3.node.getField(3);

                for (int i = 0; i < 10000; i++) {
                    assertThat(field33.get(), is(str3));
                    assertThat(field32.get(), is(str2));
                    assertThat(field31.get(), is(str1));
                    assertThat(field22.get(), is(str3));
                    assertThat(field21.get(), is(str2));
                    assertThat(field11.get(), is(str1));
                }
            }
        });
    }

    @Test
    public void testSerializableFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", Arrays.asList(new SerializableFieldSchemaConfiguration("field1")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new SerializableFieldSchemaConfiguration("field1"), new SerializableFieldSchemaConfiguration("field2")));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new SerializableFieldSchemaConfiguration("field1"), new SerializableFieldSchemaConfiguration("field2"),
                        new SerializableFieldSchemaConfiguration("field3")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final String str1 = createString(34567, 9876);
        final String str2 = createString(34, 98);
        final String str3 = createString(3456, 987);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                SerializableField field11 = node1.node.getField(1);

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 1);
                SerializableField field21 = node2.node.getField(1);
                SerializableField field22 = node2.node.getField(2);

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 2);
                SerializableField field31 = node3.node.getField(1);
                SerializableField field32 = node3.node.getField(2);
                SerializableField field33 = node3.node.getField(3);

                for (int i = 0; i < 10000; i++) {
                    field11.set(str1);
                    field21.set(str2);
                    field22.set(str3);
                    field31.set(str1);
                    field32.set(str2);
                    field33.set(str3);
                }

                for (int i = 0; i < 10000; i++) {
                    assertThat((String) field33.get(), is(str3));
                    assertThat((String) field32.get(), is(str2));
                    assertThat((String) field31.get(), is(str1));
                    assertThat((String) field22.get(), is(str3));
                    assertThat((String) field21.get(), is(str2));
                    assertThat((String) field11.get(), is(str1));
                }

                cycle.close(null, null, false);
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

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index2 = getIndex(period, "node2", "field");
                INodeIndex<Location, TestPeriodNode> index3 = getIndex(period, "node3", "field");

                TestPeriodNode node1 = index1.find(new Location(1, 1));
                SerializableField field11 = node1.node.getField(1);
                TestPeriodNode node2 = index2.find(new Location(2, 2));
                SerializableField field21 = node2.node.getField(1);
                SerializableField field22 = node2.node.getField(2);
                TestPeriodNode node3 = index3.find(new Location(3, 3));
                SerializableField field31 = node3.node.getField(1);
                SerializableField field32 = node3.node.getField(2);
                SerializableField field33 = node3.node.getField(3);

                for (int i = 0; i < 10000; i++) {
                    assertThat((String) field33.get(), is(str3));
                    assertThat((String) field32.get(), is(str2));
                    assertThat((String) field31.get(), is(str1));
                    assertThat((String) field22.get(), is(str3));
                    assertThat((String) field21.get(), is(str2));
                    assertThat((String) field11.get(), is(str1));
                }
            }
        });
    }

    @Test
    public void testJsonFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new JsonFieldSchemaConfiguration("field1")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new JsonFieldSchemaConfiguration("field1", "field1", "",
                        IOs.read(Classes.getResource(getClass(), "test.schema"), "UTF-8"),
                        Collections.asSet(new TestJsonValidatorConfiguration("testValidator")),
                        Collections.<JsonConverterSchemaConfiguration>asSet(), "type1", true, false)));

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final JsonObject jsonObject1 = Json.object().put("key1", "value1").put("key2", "value2").toObject();
        final JsonObject jsonObject2 = Json.object().put("prop1", "value1").put("prop2", 50).toObject();
        final long[] ids = new long[4];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                JsonField field11 = node1.node.getField(1);
                assertThat(field11.get(), nullValue());

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                ids[1] = node2.node.getId();
                JsonField field2 = node2.node.getField(1);
                field2.set(jsonObject1);

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 1);
                ids[2] = node3.node.getId();
                JsonField field3 = node3.node.getField(1);
                field3.set(jsonObject2);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                        CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                        PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                        Period period = cycle.getCurrentPeriod();

                        TestPeriodNode node1 = period.findNodeById(ids[0]);
                        JsonField field11 = node1.node.getField(1);
                        assertThat(field11.get(), nullValue());

                        TestPeriodNode node2 = period.findNodeById(ids[1]);
                        JsonField field21 = node2.node.getField(1);
                        assertThat(field21.get(), is((IJsonCollection) jsonObject1));

                        TestPeriodNode node3 = period.findNodeById(ids[2]);
                        JsonField field31 = node3.node.getField(1);
                        assertThat(field31.get(), is((IJsonCollection) jsonObject2));

                        final TestPeriodNode node4 = period.addNode(new Location(4, 4), 1);
                        ids[3] = node4.node.getId();
                    }
                });
            }
        });

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        try {
                            IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                            CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                            PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                            Period period = cycle.getCurrentPeriod();

                            final Period period0 = period;
                            new Expected(RawDatabaseException.class, new Runnable() {
                                @Override
                                public void run() {
                                    period0.findNodeById(ids[3]);
                                }
                            });

                            final Period period2 = period;
                            new Expected(JsonValidationException.class, new Runnable() {
                                @Override
                                public void run() {
                                    TestPeriodNode node5 = period2.addNode(new Location(5, 5), 1);
                                    JsonField field5 = node5.node.getField(1);
                                    field5.set(jsonObject1);
                                }
                            });
                        } catch (Throwable e) {
                            Exceptions.wrapAndThrow(e);
                        }
                    }
                });
            }
        });

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        try {
                            IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                            CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                            PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                            Period period = cycle.getCurrentPeriod();

                            final JsonObject jsonObject3 = Json.object().put("prop1", "value1").put("prop2", 1000).toObject();
                            final Period period3 = period;
                            new Expected(JsonValidationException.class, new Runnable() {
                                @Override
                                public void run() {
                                    TestPeriodNode node5 = period3.addNode(new Location(5, 5), 1);
                                    JsonField field5 = node5.node.getField(1);
                                    field5.set(jsonObject3);
                                }
                            });
                        } catch (Throwable e) {
                            Exceptions.wrapAndThrow(e);
                        }
                    }
                });
            }
        });
    }

    @Test
    public void testIndexFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexFieldSchemaConfiguration("field1", new BTreeIndexSchemaConfiguration("test", 0,
                        false, 256, true, 8, new StringKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(),
                        false, true, java.util.Collections.<String, String>emptyMap()))));

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

        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                IIndexField field1 = node1.node.getField(1);
                IUniqueIndex<String, Long> index = field1.getIndex();
                index.add("test1", 1l);
                index.add("test2", 2l);
                index.add("test3", 3l);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                assertThat(indexManager.getIndexes().size(), is(5));

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> nodeIndex = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));

                TestPeriodNode node1 = nodeIndex.find(new Location(1, 1));
                IIndexField field1 = node1.node.getField(1);
                IUniqueIndex<String, Long> index = field1.getIndex();

                assertThat(index.find("test1"), is(1l));
                assertThat(index.find("test2"), is(2l));
                assertThat(index.find("test3"), is(3l));
            }
        });
    }

    @Test
    public void testFullTextIndexFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexFieldSchemaConfiguration("field1", new FullTextIndexSchemaConfiguration("test", 0))));

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

        final IDocumentSchema schema = Documents.doc().numericField("field").stored().type(
                com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.INT).end().toSchema();
        final long[] ids = new long[2];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                IIndexField field1 = node1.node.getField(1);
                IFullTextIndex index = field1.getIndex();
                index.add(schema.createDocument(100));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
                assertThat(indexManager.getIndexes().size(), is(5));

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> nodeIndex = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));

                TestPeriodNode node1 = nodeIndex.find(new Location(1, 1));
                IIndexField field1 = node1.node.getField(1);
                IFullTextIndex index = field1.getIndex();

                assertThat(index.search(Queries.term("field", "100").toQuery(schema), 1).getTotalCount(), is(1));
            }
        });
    }

    @Test
    public void testIndexedFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field", "field", null, 0),
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 250, null, null, null, 0, IndexType.BTREE,
                        false, true, false, true, false, null, false, false, null, null)), null);
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                new IndexedLocationFieldSchemaConfiguration("field", "field", null, 0),
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 250, null, null, null, 0, IndexType.BTREE,
                        false, true, false, true, false, null, false, false, null, null)), null);

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));
                INodeIndex<Location, TestPeriodNode> index11 = period.getIndex(period.getSpace().getSchema().findNode("node2").findField("field"));
                INodeIndex<String, TestPeriodNode> index2 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field1"));
                INodeIndex<String, TestPeriodNode> index3 = period.getIndex(period.getSpace().getSchema().findNode("node2").findField("field1"));
                assertThat(index1 != index11, is(true));

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(1);
                field11.set("test11");

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(1);
                field21.set("test21");

                TestPeriodNode node3 = period.addNode(new Location(4, 4), 1);
                ids[2] = node3.node.getId();
                StringField field31 = node3.node.getField(1);
                field31.set("test31");

                assertThat(index1.find(new Location(1, 1)) == node1, is(true));
                assertThat(index1.find(new Location(2, 2)) == node2, is(true));
                assertThat(index1.find(new Location(4, 4)) != node3, is(true));
                assertThat(index2.find("test11") == node1, is(true));
                assertThat(index2.find("test21") == node2, is(true));
                assertThat(index3.find("test31") == node3, is(true));

                field11.set("test12");
                field21.set("test22");
                field31.set("test32");

                assertThat(index2.find("test12") == node1, is(true));
                assertThat(index2.find("test22") == node2, is(true));
                assertThat(index3.find("test32") == node3, is(true));
                assertThat(index2.find("test11"), nullValue());
                assertThat(index2.find("test21"), nullValue());
                assertThat(index3.find("test31"), nullValue());

                field11.set("test13");
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        try {
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                    Period period = cycle.getCurrentPeriod();

                    INodeIndex<Location, TestPeriodNode> index1 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));
                    INodeIndex<String, TestPeriodNode> index2 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field1"));

                    TestPeriodNode node1 = period.findNodeById(ids[0]);
                    StringField field11 = node1.node.getField(1);

                    assertThat(index1.find(new Location(1, 1)) == node1, is(true));
                    assertThat(index2.find("test13") == node1, is(true));
                    assertThat(index2.find("test11"), nullValue());
                    assertThat(index2.find("test12"), nullValue());

                    TestPeriodNode node2 = period.findNodeById(ids[1]);
                    assertThat(index2.find("test22") == node2, is(true));
                    assertThat(index1.find(new Location(2, 2)) == node2, is(true));

                    field11.set("test23");

                    TestPeriodNode node3 = period.addNode(new Location(3, 3), 0);
                    assertThat(index1.find(new Location(3, 3)) == node3, is(true));

                    throw new RuntimeException("test");
                }
            });
        } catch (RawDatabaseException e) {
            assertThat(e.getCause() instanceof RuntimeException, is(true));
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index1 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));
                INodeIndex<String, TestPeriodNode> index2 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field1"));

                TestPeriodNode node1 = period.findNodeById(ids[0]);

                assertThat(index1.find(new Location(1, 1)) == node1, is(true));
                assertThat(index2.find("test13") == node1, is(true));
                assertThat(index1.find(new Location(3, 3)), nullValue());
            }
        });
    }

    @Test
    public void testFullTextIndexedFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, null, true, false, null, null),
                        new IndexedStringFieldSchemaConfiguration("field2", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, new CollatorSchemaConfiguration("ru_RU",
                                com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength.PRIMARY), true, true, null, null),
                        new IndexedNumericFieldSchemaConfiguration("field3", "field3", null, DataType.INT, null, null, null, null, 0, null, false, false,
                                false, false, false, true, null, null)));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, null, true, false, null, null),
                        new IndexedStringFieldSchemaConfiguration("field2", true, true, 0, 256, null, null, null, 0, null,
                                false, false, false, false, false, new CollatorSchemaConfiguration("ru_RU",
                                com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength.PRIMARY), true, true, null, null),
                        new IndexedNumericFieldSchemaConfiguration("field3", "field3", null, DataType.INT, null, null, null, null, 0, null, false, false,
                                false, false, false, true, null, null)));

        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        final long[] ids1 = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(1);
                field11.set("test11");
                StringField field12 = node1.node.getField(2);
                field12.set("B Hello world!!!");
                IPrimitiveField field13 = node1.node.getField(3);
                field13.setInt(12345);

                TestPeriodNode node11 = period.addNode(new Location(1, 1), 1);
                ids1[0] = node11.node.getId();
                StringField field111 = node11.node.getField(1);
                field111.set("test111");
                StringField field112 = node11.node.getField(2);
                field112.set("1B Hello world!!!");
                IPrimitiveField field113 = node11.node.getField(3);
                field113.setInt(112345);

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(1);
                field21.set("test22");
                StringField field22 = node2.node.getField(2);
                field22.set("A Hello world!!!");
                IPrimitiveField field23 = node2.node.getField(3);
                field23.setInt(2222);

                TestPeriodNode node12 = period.addNode(new Location(2, 2), 1);
                ids1[1] = node12.node.getId();
                StringField field121 = node12.node.getField(1);
                field121.set("test122");
                StringField field122 = node12.node.getField(2);
                field122.set("1A Hello world!!!");
                IPrimitiveField field123 = node12.node.getField(3);
                field123.setInt(12222);

                cycle.addPeriod();
                period = cycle.getCurrentPeriod();

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 0);
                ids[2] = node3.node.getId();
                StringField field31 = node3.node.getField(1);
                field31.set("test33");
                StringField field32 = node3.node.getField(2);
                field32.set("0 Hello world!!!");
                IPrimitiveField field33 = node3.node.getField(3);
                field33.setInt(3333);

                TestPeriodNode node13 = period.addNode(new Location(3, 3), 1);
                ids1[2] = node13.node.getId();
                StringField field131 = node13.node.getField(1);
                field131.set("test133");
                StringField field132 = node13.node.getField(2);
                field132.set("10 Hello world!!!");
                IPrimitiveField field133 = node13.node.getField(3);
                field133.setInt(13333);
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
                Period period1 = cycle.getPeriod(0);
                Period period2 = cycle.getPeriod(1);

                INodeFullTextIndex index = period1.getSpace().getFullTextIndex();
                IDocumentSchema schema = cycleSchema.findNode("node1").getFullTextSchema();
                IDocumentSchema schema1 = cycleSchema.findNode("node2").getFullTextSchema();

                TestPeriodNode node1 = period1.findNodeById(ids[0]);
                TestPeriodNode node2 = period1.findNodeById(ids[1]);
                TestPeriodNode node3 = period2.findNodeById(ids[2]);

                TestPeriodNode node11 = period1.findNodeById(ids1[0]);
                TestPeriodNode node12 = period1.findNodeById(ids1[1]);
                TestPeriodNode node13 = period2.findNodeById(ids1[2]);

                INodeSearchResult result = index.search(Queries.term("field1", "test11").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node1, is(true));

                result = index.search(Queries.expression("field3", "*:*").toQuery(schema), new Sort("field2"), 10);
                assertThat(result.getTotalCount(), is(3));
                assertThat(result.getTopElements().get(0).get() == node3, is(true));
                assertThat(result.getTopElements().get(1).get() == node2, is(true));
                assertThat(result.getTopElements().get(2).get() == node1, is(true));

                IPrimitiveField field33 = node3.node.getField(3);
                field33.setInt(54321);

                INodeSearchResult result1 = index.search(Queries.term("field1", "test111").toQuery(schema1), 1);
                assertThat(result1.getTotalCount(), is(1));
                assertThat(result1.getTopElements().get(0).get() == node11, is(true));

                result1 = index.search(Queries.expression("field3", "*:*").toQuery(schema1), new Sort("field2"), 10);
                assertThat(result1.getTotalCount(), is(3));
                assertThat(result1.getTopElements().get(0).get() == node13, is(true));
                assertThat(result1.getTopElements().get(1).get() == node12, is(true));
                assertThat(result1.getTopElements().get(2).get() == node11, is(true));
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                INodeFullTextIndex index = period.getSpace().getFullTextIndex();
                IDocumentSchema schema = cycleSchema.findNode("node1").getFullTextSchema();

                TestPeriodNode node1 = period.findNodeById(ids[0]);
                TestPeriodNode node2 = period.findNodeById(ids[1]);

                INodeSearchResult result = index.search(Queries.term("field1", "test11").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node1, is(true));

                result = index.search(Queries.expression("field3", "*:*").toQuery(schema), new Sort("field2"), 10);
                assertThat(result.getTotalCount(), is(2));
                assertThat(result.getTopElements().get(0).get() == node2, is(true));
                assertThat(result.getTopElements().get(1).get() == node1, is(true));
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
                Period period = cycle.getPeriod(0);

                INodeFullTextIndex index = period.getSpace().getFullTextIndex();
                IDocumentSchema schema = cycleSchema.findNode("node1").getFullTextSchema();

                TestPeriodNode node3 = period.findNodeById(ids[2]);

                INodeSearchResult result = index.search(Queries.term("field3", "54321").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node3, is(true));

                result = index.search(Queries.term(CycleSchema.PERIOD_FIELD_NAME, "0").toQuery(schema), 10);
                assertThat(result.getTotalCount(), is(2));

                result = index.search(Queries.term(CycleSchema.PERIOD_FIELD_NAME, "1").toQuery(schema), 10);
                assertThat(result.getTotalCount(), is(1));
            }
        });
    }

    @Test
    public void testRootNode() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new TestFieldSchemaConfiguration("field1")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2")));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new TestFieldSchemaConfiguration("field1"), new TestFieldSchemaConfiguration("field2"),
                        new TestFieldSchemaConfiguration("field3")));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        "node3", null, 1000000, 2, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space2 = new PeriodSpaceSchemaConfiguration("space2", "space2", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1, space2)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema2 = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                CycleSchema cycleSchema2 = (CycleSchema) periodSchema2.getCycles().get(0);

                PeriodSpace cycle2 = cycleSchema2.getCurrentCycle().getSpace();
                Period period2 = cycle2.getCurrentPeriod();
                assertThat(period2.getRootNode(), nullValue());

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.getRootNode();
                PeriodNode periodNode1 = (PeriodNode) node1.getNode();
                assertThat(((TestPeriodNode) period.findNodeById(periodNode1.getId())).getNode(), is((INode) periodNode1));
                assertThat(periodNode1.getPeriod() == period, is(true));
                assertThat(periodNode1.getFieldCount(), is(4));
                assertThat(periodNode1.getSchema().getConfiguration().getName(), is("node3"));
                assertThat(periodNode1.getScope(), is((IScopeName) ScopeName.root()));
                assertThat(periodNode1.getLocation(), is(new Location(0, 0)));
                TestField field1 = periodNode1.getField(1);
                assertThat(field1.field.getNode() == periodNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                cycle.addPeriod();
                period = cycle.getCurrentPeriod();

                node1 = period.getRootNode();
                periodNode1 = (PeriodNode) node1.getNode();
                assertThat(((TestPeriodNode) period.findNodeById(periodNode1.getId())).getNode(), is((INode) periodNode1));
                assertThat(periodNode1.getPeriod() == period, is(true));
                assertThat(periodNode1.getFieldCount(), is(4));
                assertThat(periodNode1.getSchema().getConfiguration().getName(), is("node3"));
                assertThat(periodNode1.getScope(), is((IScopeName) ScopeName.root()));
                assertThat(periodNode1.getLocation(), is(new Location(0, 0)));
                field1 = periodNode1.getField(1);
                assertThat(field1.field.getNode() == periodNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema2 = transaction.getCurrentSchema().findDomain("test").findSpace("space2");
                CycleSchema cycleSchema2 = (CycleSchema) periodSchema2.getCycles().get(0);

                PeriodSpace cycle2 = cycleSchema2.getCurrentCycle().getSpace();
                Period period2 = cycle2.getCurrentPeriod();
                assertThat(period2.getRootNode(), nullValue());

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCurrentPeriod();

                TestPeriodNode node1 = period.getRootNode();
                PeriodNode periodNode1 = (PeriodNode) node1.getNode();
                assertThat(((TestPeriodNode) period.findNodeById(periodNode1.getId())).getNode(), is((INode) periodNode1));
                assertThat(periodNode1.getPeriod() == period, is(true));
                assertThat(periodNode1.getFieldCount(), is(4));
                assertThat(periodNode1.getSchema().getConfiguration().getName(), is("node3"));
                assertThat(periodNode1.getScope(), is((IScopeName) ScopeName.root()));
                assertThat(periodNode1.getLocation(), is(new Location(0, 0)));
                TestField field1 = periodNode1.getField(1);
                assertThat(field1.field.getNode() == periodNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));

                period = cycle.getPeriod(0);

                node1 = period.getRootNode();
                periodNode1 = (PeriodNode) node1.getNode();
                assertThat(((TestPeriodNode) period.findNodeById(periodNode1.getId())).getNode(), is((INode) periodNode1));
                assertThat(periodNode1.getPeriod() == period, is(true));
                assertThat(periodNode1.getFieldCount(), is(4));
                assertThat(periodNode1.getSchema().getConfiguration().getName(), is("node3"));
                assertThat(periodNode1.getScope(), is((IScopeName) ScopeName.root()));
                assertThat(periodNode1.getLocation(), is(new Location(0, 0)));
                field1 = periodNode1.getField(1);
                assertThat(field1.field.getNode() == periodNode1, is(true));
                assertThat(field1.field.getSchema().getConfiguration().getName(), is("field1"));
            }
        });
    }

    private void writeRegion(int base, IRawWriteRegion region) {
        for (int i = 0; i < region.getLength(); i++)
            region.writeByte(i, (byte) (base + i));
    }

    private void checkRegion(IRawReadRegion region, ByteArray array) {
        assertThat(region.readByteArray(0, array.getLength()), is(array));
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

    private INodeIndex<Location, TestPeriodNode> getIndex(Period period, String nodeType, String fieldName) {
        return period.getIndex(period.getSpace().getSchema().findNode(nodeType).findField(fieldName));
    }

    private INodeIndex<Integer, TestNode> getIndex(IObjectSpace space, String nodeType, String fieldName) {
        return space.getIndex(space.getSchema().findNode(nodeType).findField(fieldName));
    }

    public static class PeriodTestNodeSchemaConfiguration extends PeriodNodeSchemaConfiguration {
        public PeriodTestNodeSchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields) {
            super(name, new IndexedLocationFieldSchemaConfiguration("field"), fields, null);
        }

        public PeriodTestNodeSchemaConfiguration(String name, IndexedLocationFieldSchemaConfiguration primaryField,
                                                 List<? extends FieldSchemaConfiguration> fields, String documentType) {
            super(name, name, null, primaryField, fields, documentType);
        }

        public PeriodTestNodeSchemaConfiguration(String name, String alias, String description, IndexedLocationFieldSchemaConfiguration primaryField,
                                                 List<? extends FieldSchemaConfiguration> fields, String documentType) {
            super(name, alias, description, primaryField, fields, documentType);
        }

        @Override
        public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            return new TestPeriodNodeSchema(this, index, fields, documentSchema);
        }

        @Override
        public INodeObject createNode(INode node) {
            return new TestPeriodNode(node);
        }
    }

    public static class TestPeriodNodeSchema extends PeriodNodeSchema {
        public boolean invalid;

        public TestPeriodNodeSchema(PeriodNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            super(configuration, index, fields, documentSchema);
        }

        @Override
        public void validate(INode node) {
            super.validate(node);

            if (invalid)
                throw new RuntimeException("test");
        }
    }

    public static class TestPeriodNode implements INodeObject {
        public final Node node;
        public boolean flushed;

        public TestPeriodNode(INode node) {
            this.node = (Node) node;
        }

        @Override
        public INode getNode() {
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestPeriodNode))
                return false;

            TestPeriodNode n = (TestPeriodNode) o;
            return node.equals(n.node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public void onBeforeFlush() {
            flushed = true;
        }

        @Override
        public void onAfterFlush() {
        }

        @Override
        public String toString() {
            return node.toString();
        }

        @Override
        public void onCreated(Object primaryKey, Object[] args) {
        }

        @Override
        public void onOpened() {
        }

        @Override
        public void onDeleted() {
        }

        @Override
        public void onBeforeCreated(Object primaryKey, Object[] args, Object[] fieldInitializers) {
            if (args != null && args.length == 4) {
                IFileFieldInitializer fileInitializer = (IFileFieldInitializer) fieldInitializers[1];
                fileInitializer.setPathIndex((Integer) args[0]);
                fileInitializer.setMaxFileSize((Integer) args[2]);
                fileInitializer.setDirectory((String) args[3]);
            }

            if (args != null && args.length == 1) {
                IBlobFieldInitializer blobInitializer1 = (IBlobFieldInitializer) fieldInitializers[1];
                blobInitializer1.setStore(args[0]);
                if (fieldInitializers.length > 2) {
                    IBlobFieldInitializer blobInitializer2 = (IBlobFieldInitializer) fieldInitializers[2];
                    blobInitializer2.setStore(args[0]);
                }
            }
        }

        @Override
        public void onUnloaded() {
        }

        @Override
        public void validate() {
        }

        @Override
        public void dump(IJsonHandler json, IDumpContext context) {
        }


        @Override
        public boolean allowModify() {
            return true;
        }

        @Override
        public boolean allowDeletion() {
            return true;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public void onMigrated() {
        }

        @Override
        public void onBeforeMigrated(Object primaryKey) {
        }
    }

    public static class TestFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
        public TestFieldSchemaConfiguration(String name) {
            super(name, name, null, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, 0);
        }

        @Override
        public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
            return new TestFieldSchema(this, index, offset);
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
        public Object createInitializer() {
            return null;
        }
    }

    public static class TestFieldSchema extends FieldSchema {
        public TestFieldSchema(TestFieldSchemaConfiguration configuration, int index, int offset) {
            super(configuration, index, offset);
        }

        @Override
        public TestFieldSchemaConfiguration getConfiguration() {
            return (TestFieldSchemaConfiguration) super.getConfiguration();
        }

        @Override
        public IFieldObject createField(IField field) {
            return new TestField((IComplexField) field);
        }

        @Override
        public void resolveDependencies() {
        }

        @Override
        public void validate(IField field) {
        }

        @Override
        public int getIndexTotalIndex() {
            return -1;
        }
    }

    public static class TestField implements IFieldObject {
        public final IComplexField field;

        public TestField(IComplexField field) {
            this.field = field;
        }

        @Override
        public void flush() {
        }

        @Override
        public void onCreated(Object primaryKey, Object initialzer) {
        }

        @Override
        public void onAfterCreated(Object primaryKey, Object initializer) {
        }

        @Override
        public void onOpened() {
        }

        @Override
        public void onDeleted() {
        }

        @Override
        public void onUnloaded() {
        }
    }

    private static class TestJsonValidatorConfiguration extends JsonValidatorSchemaConfiguration {
        public TestJsonValidatorConfiguration(String name) {
            super(name);
        }

        @Override
        public IJsonValidator createValidator() {
            return new TestJsonValidator();
        }
    }

    private static class TestJsonValidator implements IJsonValidator {
        @Override
        public boolean supports(Class clazz) {
            return clazz == Long.class;
        }

        @Override
        public void validate(JsonType type, Object instance, IJsonValidationContext context) {
            IJsonDiagnostics diagnostics = context.getDiagnostics();

            Long l = (Long) instance;
            if (l < 10 || l > 100)
                diagnostics.addError(new NonLocalizedMessage("Property is not in range [10..100]."));
        }
    }
}
