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
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.CyclePeriod;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodNode;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.fields.StringField;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.PeriodTestNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestField;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestFieldSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNode;
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
 * The {@link CyclePeriodTests} are tests for cycle period.
 *
 * @author Medvedev-A
 * @see PeriodSpace
 */
public class CyclePeriodTests {
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
    public void testCyclePeriod() throws Throwable {
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

                Period period1 = cycle.getCyclePeriod();
                assertThat(cycle.getPeriod(period1.getPeriodIndex()) == period1, is(true));
                assertThat(period1.getPeriodIndex(), is(CyclePeriod.PERIOD_INDEX));
                assertThat(period1.getSpace() == cycle, is(true));
                assertThat(period1.getStartTime() >= cycle.getStartTime(), is(true));
                assertThat(period1.getEndTime(), is(0l));
                assertThat(period1.isClosed(), is(false));

                cycle.addPeriod();
                assertThat(cycle.getCyclePeriod() == period1, is(true));
                assertThat(period1.isClosed(), is(false));

                Period period = cycle.getCyclePeriod();
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
                Period period = cycle.getCyclePeriod();

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

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle1 = cycleSchema.getCurrentCycle().getSpace();
                Period period1 = cycle1.getCyclePeriod();
                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period1, "node1", "field");

                cycleSchema.addPeriod();
                assertThat(cycle1.isClosed(), is(true));
                assertThat(period1.isClosed(), is(true));

                PeriodSpace cycle2 = cycleSchema.getCurrentCycle().getSpace();
                Period period2 = cycle2.getCyclePeriod();

                TestPeriodNode node11 = index1.find(new Location(1, 1));
                assertThat(node11.node.isReadOnly(), is(true));
                TestPeriodNode node21 = period2.addNode(new Location(1, 1), 0);
                assertThat(node21.node.isReadOnly(), is(false));

                List<Pair<IPeriod, TestPeriodNode>> list = Collections.toList(((IPeriodNode) node21.node).<TestPeriodNode>getPeriodNodes().iterator());
                assertThat(list.size(), is(2));
                assertThat(list.get(0), is(new Pair(period2, node21)));
                assertThat(list.get(1), is(new Pair(period1, node11)));
            }
        });
    }

    @Test
    public void testNonAggregatingCyclePeriod() throws Throwable {
        PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1000000, 1, true, null)), 0, 0);

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

                PeriodSpace cycleSpace1 = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycleSpace1.getCyclePeriod() == cycleSpace1.getCurrentPeriod(), is(true));
            }
        });
    }

    @Test
    public void testIndexedFields() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 250, null, null, null, 0, IndexType.BTREE,
                        false, true, false, true, false, null, false, false, null, null)));

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

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                INodeIndex<Location, TestPeriodNode> index1 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field"));
                INodeIndex<String, TestPeriodNode> index2 = period.getIndex(period.getSpace().getSchema().findNode("node1").findField("field1"));

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(1);
                field11.set("test11");

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(1);
                field21.set("test21");

                assertThat(index1.find(new Location(1, 1)) == node1, is(true));
                assertThat(index1.find(new Location(2, 2)) == node2, is(true));
                assertThat(index2.find("test11") == node1, is(true));
                assertThat(index2.find("test21") == node2, is(true));

                field11.set("test12");
                field21.set("test22");

                assertThat(index2.find("test12") == node1, is(true));
                assertThat(index2.find("test22") == node2, is(true));
                assertThat(index2.find("test11"), nullValue());
                assertThat(index2.find("test21"), nullValue());

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
                    Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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

        final long[] ids = new long[3];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();
                StringField field11 = node1.node.getField(1);
                field11.set("test11");
                StringField field12 = node1.node.getField(2);
                field12.set("B Hello world!!!");
                IPrimitiveField field13 = node1.node.getField(3);
                field13.setInt(12345);

                TestPeriodNode node2 = period.addNode(new Location(2, 2), 0);
                ids[1] = node2.node.getId();
                StringField field21 = node2.node.getField(1);
                field21.set("test22");
                StringField field22 = node2.node.getField(2);
                field22.set("A Hello world!!!");
                IPrimitiveField field23 = node2.node.getField(3);
                field23.setInt(2222);

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 0);
                ids[2] = node3.node.getId();
                StringField field31 = node3.node.getField(1);
                field31.set("test33");
                StringField field32 = node3.node.getField(2);
                field32.set("0 Hello world!!!");
                IPrimitiveField field33 = node3.node.getField(3);
                field33.setInt(3333);
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

                INodeFullTextIndex index = period.getSpace().getFullTextIndex();
                IDocumentSchema schema = cycleSchema.findNode("node1").getFullTextSchema();

                TestPeriodNode node1 = period.findNodeById(ids[0]);
                TestPeriodNode node2 = period.findNodeById(ids[1]);
                TestPeriodNode node3 = period.findNodeById(ids[2]);

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

                cycle.addPeriod();
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

                INodeFullTextIndex index = period.getSpace().getFullTextIndex();
                IDocumentSchema schema = cycleSchema.findNode("node1").getFullTextSchema();

                TestPeriodNode node1 = period.findNodeById(ids[0]);
                TestPeriodNode node2 = period.findNodeById(ids[1]);
                TestPeriodNode node3 = period.findNodeById(ids[2]);

                INodeSearchResult result = index.search(Queries.term("field1", "test11").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node1, is(true));

                result = index.search(Queries.expression("field3", "*:*").toQuery(schema), new Sort("field2"), 10);
                assertThat(result.getTotalCount(), is(3));
                assertThat(result.getTopElements().get(0).get() == node3, is(true));
                assertThat(result.getTopElements().get(1).get() == node2, is(true));
                assertThat(result.getTopElements().get(2).get() == node1, is(true));
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

                INodeFullTextIndex index = period.getSpace().getFullTextIndex();
                IDocumentSchema schema = cycleSchema.findNode("node1").getFullTextSchema();

                TestPeriodNode node3 = period.findNodeById(ids[2]);

                INodeSearchResult result = index.search(Queries.term("field3", "54321").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat(result.getTopElements().get(0).get() == node3, is(true));

                result = index.search(Queries.term(CycleSchema.PERIOD_FIELD_NAME, "-1").toQuery(schema), 10);
                assertThat(result.getTotalCount(), is(3));
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
                        null, "node3", 1000000, 2, false, null)), 0, 0);
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
                Period period2 = cycle2.getCyclePeriod();
                assertThat(period2.getRootNode(), nullValue());

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

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
                Period period2 = cycle2.getCyclePeriod();
                assertThat(period2.getRootNode(), nullValue());

                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                    Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                    Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

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
                Period period = cycle.getCyclePeriod();

                IObjectSpaceSchema dataSchema2 = transaction.getCurrentSchema().findDomain("test2").findSpace("space2");
                ObjectSpace space2 = (ObjectSpace) dataSchema2.getSpace();

                INodeIndex<Location, TestPeriodNode> index1 = getIndex(period, "node1", "field");
                INodeIndex<Integer, TestNode> index2 = getIndex(space2, "node2", "field");

                TestPeriodNode node1 = index1.find(new Location(COUNT + 1, 1));
                IReferenceField<TestNode> field1 = node1.node.getField(1);
                List<TestNode> refs1 = Collections.toList(field1.iterator());
                TestPeriodNode node2 = index1.find(new Location(COUNT + 2, 2));
                IReferenceField<TestNode> field2 = node2.node.getField(1);
                List<TestNode> refs2 = Collections.toList(field2.iterator());

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
    public void testCrossReferences() throws Throwable {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new ReferenceFieldSchemaConfiguration("field1", "node2"), new SingleReferenceFieldSchemaConfiguration("field2", "node2")));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.<FieldSchemaConfiguration>asList());
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

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle = cycleSchema.getCurrentCycle().getSpace();
                Period cyclePeriod = cycle.getCyclePeriod();
                Period currentPeriod1 = cycle.getCurrentPeriod();

                TestPeriodNode node = cyclePeriod.addNode(new Location(1, 1), 1);
                TestPeriodNode node1 = currentPeriod1.addNode(new Location(1, 1), 1);

                TestPeriodNode node11 = currentPeriod1.addNode(new Location(2, 2), 0);
                IReferenceField<TestPeriodNode> field11 = node11.node.getField(1);
                ISingleReferenceField<TestPeriodNode> field12 = node11.node.getField(2);

                field11.add(node);
                field11.add(node1);
                field12.set(node);

                cycle.addPeriod();

                Period currentPeriod2 = cycle.getCurrentPeriod();

                TestPeriodNode node2 = currentPeriod2.addNode(new Location(1, 1), 1);

                TestPeriodNode node21 = currentPeriod2.addNode(new Location(2, 2), 0);
                IReferenceField<TestPeriodNode> field21 = node21.node.getField(1);
                ISingleReferenceField<TestPeriodNode> field22 = node21.node.getField(2);

                field21.add(node);
                field21.add(node2);
                field22.set(node);
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
                Period cyclePeriod = cycle.getCyclePeriod();
                Period currentPeriod1 = cycle.getPeriod(0);
                Period currentPeriod2 = cycle.getCurrentPeriod();

                INodeIndex<Location, TestPeriodNode> index = getIndex(cyclePeriod, "node2", "field");

                INodeIndex<Location, TestPeriodNode> index11 = getIndex(currentPeriod1, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index12 = getIndex(currentPeriod1, "node2", "field");

                INodeIndex<Location, TestPeriodNode> index21 = getIndex(currentPeriod2, "node1", "field");
                INodeIndex<Location, TestPeriodNode> index22 = getIndex(currentPeriod2, "node2", "field");

                TestPeriodNode node11 = index11.find(new Location(2, 2));
                IReferenceField<TestPeriodNode> field11 = node11.node.getField(1);
                ISingleReferenceField<TestPeriodNode> field12 = node11.node.getField(2);

                List<TestPeriodNode> list1 = Collections.toList(field11.iterator());
                TestPeriodNode node1 = field12.get();
                TestPeriodNode node = index.find(new Location(1, 1));

                assertThat(node1 == node, is(true));
                assertThat(((IPeriodNode) node.node).getPeriod() == cyclePeriod, is(true));
                TestPeriodNode node12 = index12.find(new Location(1, 1));
                assertThat(((IPeriodNode) node12.node).getPeriod() == currentPeriod1, is(true));
                assertThat(list1, is(Arrays.asList(node, node12)));

                TestPeriodNode node21 = index21.find(new Location(2, 2));
                IReferenceField<TestPeriodNode> field21 = node21.node.getField(1);
                ISingleReferenceField<TestPeriodNode> field22 = node21.node.getField(2);

                List<TestPeriodNode> list2 = Collections.toList(field21.iterator());
                TestPeriodNode node2 = field22.get();

                assertThat(node2 == node, is(true));
                TestPeriodNode node22 = index22.find(new Location(1, 1));
                assertThat(((IPeriodNode) node22.node).getPeriod() == currentPeriod2, is(true));
                assertThat(list2, is(Arrays.asList(node, node22)));
            }
        });
    }

    private INodeIndex<Location, TestPeriodNode> getIndex(Period period, String nodeType, String fieldName) {
        return period.getIndex(period.getSpace().getSchema().findNode(nodeType).findField(fieldName));
    }

    private INodeIndex<Integer, TestNode> getIndex(IObjectSpace space, String nodeType, String fieldName) {
        return space.getIndex(space.getSchema().findNode(nodeType).findField(fieldName));
    }
}
