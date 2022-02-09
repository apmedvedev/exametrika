/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
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
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodCycle;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.schema.NodeSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.PeriodTestNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNode;
import com.exametrika.tests.exadb.SchemaSpaceTests.TestFieldSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link PeriodSpaceTests} are tests for period space.
 *
 * @author Medvedev-A
 * @see PeriodSpace
 */
public class PeriodSpaceTests {
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
    public void testPeriodSpace() throws Throwable {
        PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1000000, 2, false, null)), 0, 0);

        final long t = Times.getCurrentTime();
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
                assertThat(cycleSpace1 != null, is(true));
                assertThat(cycleSchema.getCurrentCycle().getSpace() == cycleSpace1, is(true));
                assertThat(cycleSpace1.getFileIndex(), is(14));
                assertThat(cycleSpace1.getPreviousCycle(), nullValue());
                assertThat(cycleSpace1.getStartTime() >= t && cycleSpace1.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace1.getEndTime(), is(0l));
                assertThat(cycleSpace1.getPeriodsCount(), is(1));
                assertThat(cycleSpace1.getPeriod(0).getPeriodIndex(), is(0));
                Period period1 = cycleSpace1.getCurrentPeriod();
                assertThat(cycleSpace1.getCurrentPeriod() == period1, is(true));
                assertThat(cycleSpace1.getPeriod(period1.getPeriodIndex()) == period1, is(true));
                assertThat(period1.getPeriodIndex(), is(0));
                assertThat(period1.getSpace() == cycleSpace1, is(true));
                assertThat(period1.getStartTime() >= cycleSpace1.getStartTime(), is(true));
                assertThat(period1.getEndTime(), is(0l));

                Iterable<IPeriodCycle> cycles = cycleSchema.getCycles();
                Iterator<IPeriodCycle> it = cycles.iterator();
                assertThat(it.hasNext(), is(true));
                assertThat(it.next(), is((IPeriodCycle) cycleSchema.getCurrentCycle()));
                assertThat(it.hasNext(), is(false));

                cycleSpace1.addPeriod();
                Period period2 = cycleSpace1.getCurrentPeriod();
                assertThat(period2.getPeriodIndex(), is(1));
                assertThat(period2.getSpace() == cycleSpace1, is(true));
                assertThat(period2.getStartTime() > 0, is(true));
                assertThat(period2.getEndTime(), is(0l));
                assertThat(cycleSpace1.getCurrentPeriod() == period2, is(true));
                assertThat(cycleSpace1.getPeriodsCount(), is(2));
                assertThat(cycleSpace1.getPeriod(1).getPeriodIndex(), is(1));
                assertThat(cycleSpace1.getPeriod(1) == period2, is(true));

                assertThat(period1.getEndTime() > cycleSpace1.getStartTime(), is(true));

                cycleSchema.addPeriod();
                Period period3 = cycleSchema.getCurrentCycle().getSpace().getCurrentPeriod();
                IPeriodCycle cycle1 = cycleSchema.getCurrentCycle().getSpace().getPreviousCycle();
                assertThat(cycle1.getEndTime() > cycleSpace1.getStartTime(), is(true));
                assertThat(cycle1.getSchema() == cycleSchema, is(true));
                assertThat(cycleSchema.getCurrentCycle().getPreviousCycle() == cycle1, is(true));
                cycleSpace1 = (PeriodSpace) cycle1.getSpace();
                assertThat(cycleSpace1.getStartTime(), is(cycle1.getStartTime()));
                assertThat(cycleSpace1.getEndTime(), is(cycle1.getEndTime()));
                period2 = cycleSpace1.getCurrentPeriod();
                assertThat(period2.getEndTime() <= cycleSpace1.getEndTime(), is(true));

                assertThat(cycleSpace1.findPeriod(period1.getStartTime() - 1), nullValue());
                assertThat(cycleSpace1.findPeriod(period1.getStartTime()) == period1, is(true));
                assertThat(cycleSpace1.findPeriod(period2.getStartTime() - 1) == period1, is(true));
                assertThat(cycleSpace1.findPeriod(period2.getStartTime()) == period2, is(true));
                assertThat(cycleSpace1.findPeriod(period2.getStartTime() + 1) == period2, is(true));

                assertThat(period3.getSpace() != cycleSpace1, is(true));
                PeriodSpace cycleSpace2 = period3.getSpace();
                assertThat(cycleSpace2.getSchema() == cycleSchema, is(true));
                assertThat(cycleSchema.getCurrentCycle().getSpace().getFileIndex() == cycleSpace2.getFileIndex(), is(true));

                cycles = cycleSchema.getCycles();
                it = cycles.iterator();
                assertThat(it.hasNext(), is(true));
                assertThat(it.next(), is((IPeriodCycle) cycleSchema.getCurrentCycle()));
                assertThat(it.hasNext(), is(true));
                assertThat(it.next() == cycle1, is(true));
                assertThat(it.hasNext(), is(false));

                assertThat(cycleSpace2.getPreviousCycle() == cycle1, is(true));
                assertThat(cycleSpace2.getStartTime() >= t && cycleSpace2.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace2.getEndTime(), is(0l));
                assertThat(cycleSpace2.getPeriodsCount(), is(1));
                assertThat(cycleSpace2.getPeriod(0).getPeriodIndex(), is(0));
                assertThat(cycleSpace2.getCurrentPeriod() == period3, is(true));
                assertThat(period3.getPeriodIndex(), is(0));
                assertThat(period3.getSpace() == cycleSpace2, is(true));
                assertThat(period3.getStartTime() >= cycleSpace2.getStartTime(), is(true));
                assertThat(period3.getEndTime(), is(0l));
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

                    PeriodSpace cycleSpace = cycleSchema.getCurrentCycle().getSpace();
                    assertThat(cycleSchema.getCurrentCycle().getSpace() == cycleSpace, is(true));
                    assertThat(cycleSpace.getPreviousCycle().getSpace().getFileIndex(), is(14));
                    assertThat(cycleSpace.isClosed(), is(false));
                    assertThat(cycleSpace.getStartTime() >= t && cycleSpace.getStartTime() <= Times.getCurrentTime(), is(true));
                    assertThat(cycleSpace.getEndTime(), is(0l));
                    assertThat(cycleSpace.getPeriodsCount(), is(1));
                    assertThat(cycleSpace.getPeriod(0).getPeriodIndex(), is(0));
                    assertThat(cycleSpace.getCurrentPeriod().getPeriodIndex(), is(0));
                    Period period = cycleSpace.getCurrentPeriod();
                    assertThat(period.getPeriodIndex(), is(0));
                    assertThat(period.isClosed(), is(false));
                    assertThat(period.getSpace() == cycleSpace, is(true));
                    assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                    assertThat(period.getEndTime(), is(0l));

                    IPeriodCycle cycle11 = cycleSpace.getPreviousCycle();
                    assertThat(cycle11 == cycleSpace.getPreviousCycle(), is(true));
                    cycleSpace = (PeriodSpace) cycle11.getSpace();
                    assertThat(cycleSpace != null, is(true));
                    assertThat(cycleSpace.getPreviousCycle(), nullValue());
                    assertThat(cycleSpace.isClosed(), is(true));
                    assertThat(cycleSpace.getStartTime() >= t && cycleSpace.getStartTime() <= Times.getCurrentTime(), is(true));
                    assertThat(cycleSpace.getEndTime() > cycleSpace.getStartTime(), is(true));
                    assertThat(cycleSpace.getPeriodsCount(), is(2));
                    assertThat(cycleSpace.getPeriod(0).getPeriodIndex(), is(0));
                    assertThat(cycleSpace.getPeriod(1).getPeriodIndex(), is(1));
                    period = cycleSpace.getPeriod(1);
                    assertThat(period.getPeriodIndex(), is(1));
                    assertThat(period.isClosed(), is(true));
                    assertThat(period.getSpace() == cycleSpace, is(true));
                    assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                    assertThat(period.getEndTime(), is(cycleSpace.getEndTime()));
                    assertThat(cycleSpace.findPeriod(period.getStartTime() - 1) != period, is(true));
                    assertThat(cycleSpace.findPeriod(period.getStartTime()) == period, is(true));
                    assertThat(cycleSpace.findPeriod(period.getStartTime() + 1) == period, is(true));

                    period = cycleSpace.getPeriod(0);
                    assertThat(period.getPeriodIndex(), is(0));
                    assertThat(period.isClosed(), is(true));
                    assertThat(period.getSpace() == cycleSpace, is(true));
                    assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                    assertThat(period.getEndTime() >= cycleSpace.getStartTime(), is(true));

                    assertThat(cycleSpace.findPeriod(period.getStartTime() - 1), nullValue());
                    assertThat(cycleSpace.findPeriod(period.getStartTime()) == period, is(true));

                    assertThat(cycleSpace.getPreviousCycle(), nullValue());

                    periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    final CycleSchema cycleSchema2 = (CycleSchema) periodSchema.getCycles().get(0);
                    new Expected(IllegalArgumentException.class, new Runnable() {
                        @Override
                        public void run() {
                            cycleSchema2.getCurrentCycle().getSpace().getPreviousCycle().getSpace().addPeriod();
                        }
                    });
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
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycleSpace = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycleSpace != null, is(true));
                assertThat(cycleSpace.getPreviousCycle(), nullValue());
                assertThat(cycleSpace.getPreviousCycle(), nullValue());
                assertThat(cycleSpace.isClosed(), is(true));
                assertThat(cycleSpace.getStartTime() >= t && cycleSpace.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace.getEndTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(cycleSpace.getPeriodsCount(), is(2));
                assertThat(cycleSpace.getPeriod(0).getPeriodIndex(), is(0));
                assertThat(cycleSpace.getPeriod(1).getPeriodIndex(), is(1));
                Period period = cycleSpace.getPeriod(1);
                assertThat(period.getPeriodIndex(), is(1));
                assertThat(cycleSpace.isClosed(), is(true));
                assertThat(period.getSpace() == cycleSpace, is(true));
                assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(period.getEndTime(), is(cycleSpace.getEndTime()));
                period = cycleSpace.getPeriod(0);
                assertThat(period.getPeriodIndex(), is(0));
                assertThat(cycleSpace.isClosed(), is(true));
                assertThat(period.getSpace() == cycleSpace, is(true));
                assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(period.getEndTime() >= cycleSpace.getStartTime(), is(true));

                assertThat(cycleSpace.getPreviousCycle(), nullValue());
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
                cycleSchema.addPeriod();
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

                PeriodSpace cycleSpace = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycleSpace.getPreviousCycle().getSpace().getFileIndex(), is(14));
                assertThat(cycleSpace.isClosed(), is(false));
                assertThat(cycleSpace.getStartTime() >= t && cycleSpace.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace.getEndTime(), is(0l));
                assertThat(cycleSpace.getPeriodsCount(), is(1));
                assertThat(cycleSpace.getPeriod(0).getPeriodIndex(), is(0));
                assertThat(cycleSpace.getCurrentPeriod().getPeriodIndex(), is(0));
                Period period = cycleSpace.getCurrentPeriod();
                assertThat(period.getPeriodIndex(), is(0));
                assertThat(period.isClosed(), is(true));
                assertThat(period.getSpace() == cycleSpace, is(true));
                assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(period.getEndTime() >= cycleSpace.getStartTime(), is(true));

                assertThat(cycleSpace.findPeriod(period.getStartTime() - 1), nullValue());
                assertThat(cycleSpace.findPeriod(period.getStartTime()) == period, is(true));
                assertThat(cycleSpace.findPeriod(Long.MAX_VALUE) == period, is(true));

                cycleSpace = cycleSpace.getPreviousCycle().getSpace();
                assertThat(cycleSpace != null, is(true));
                assertThat(cycleSpace.getPreviousCycle(), nullValue());
                assertThat(cycleSpace.isClosed(), is(true));
                assertThat(cycleSpace.getStartTime() >= t && cycleSpace.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace.getEndTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(cycleSpace.getPeriodsCount(), is(2));
                assertThat(cycleSpace.getPeriod(0).getPeriodIndex(), is(0));
                assertThat(cycleSpace.getPeriod(1).getPeriodIndex(), is(1));
                period = cycleSpace.getPeriod(1);
                assertThat(period.getPeriodIndex(), is(1));
                assertThat(period.isClosed(), is(true));
                assertThat(period.getSpace() == cycleSpace, is(true));
                assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(period.getEndTime(), is(cycleSpace.getEndTime()));
                period = cycleSpace.getPeriod(0);
                assertThat(period.getPeriodIndex(), is(0));
                assertThat(period.isClosed(), is(true));
                assertThat(period.getSpace() == cycleSpace, is(true));
                assertThat(period.getStartTime() >= cycleSpace.getStartTime(), is(true));
                assertThat(period.getEndTime() >= cycleSpace.getStartTime(), is(true));

                assertThat(cycleSpace.getPreviousCycle(), nullValue());
            }
        });
    }

    @Test
    public void testNonAggregatingPeriodSpace() throws Throwable {
        PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1000000, 1, true, null)), 0, 0);

        final long t = Times.getCurrentTime();
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
                assertThat(cycleSpace1 != null, is(true));
                assertThat(cycleSchema.getCurrentCycle().getSpace() == cycleSpace1, is(true));
                assertThat(cycleSpace1.getFileIndex(), is(14));
                assertThat(cycleSpace1.getPreviousCycle(), nullValue());
                assertThat(cycleSpace1.getStartTime() >= t && cycleSpace1.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace1.getEndTime(), is(0l));
                assertThat(cycleSpace1.getPeriodsCount(), is(1));
                assertThat(cycleSpace1.getPeriod(0).getPeriodIndex(), is(0));
                Period period1 = cycleSpace1.getCurrentPeriod();
                assertThat(cycleSpace1.getCurrentPeriod() == period1, is(true));
                assertThat(cycleSpace1.getPeriod(period1.getPeriodIndex()) == period1, is(true));
                assertThat(period1.getPeriodIndex(), is(0));
                assertThat(period1.getSpace() == cycleSpace1, is(true));
                assertThat(period1.getStartTime() >= cycleSpace1.getStartTime(), is(true));
                assertThat(period1.getEndTime(), is(0l));

                Iterable<IPeriodCycle> cycles = cycleSchema.getCycles();
                Iterator<IPeriodCycle> it = cycles.iterator();
                assertThat(it.hasNext(), is(true));
                assertThat(it.next(), is((IPeriodCycle) cycleSchema.getCurrentCycle()));
                assertThat(it.hasNext(), is(false));

                cycleSchema.addPeriod();
                Period period3 = cycleSchema.getCurrentCycle().getSpace().getCurrentPeriod();
                IPeriodCycle cycle1 = cycleSchema.getCurrentCycle().getSpace().getPreviousCycle();
                assertThat(cycle1.getEndTime() > cycleSpace1.getStartTime(), is(true));
                assertThat(cycle1.getSchema() == cycleSchema, is(true));
                assertThat(cycleSchema.getCurrentCycle().getPreviousCycle() == cycle1, is(true));
                cycleSpace1 = (PeriodSpace) cycle1.getSpace();
                assertThat(cycleSpace1.getStartTime(), is(cycle1.getStartTime()));
                assertThat(cycleSpace1.getEndTime(), is(cycle1.getEndTime()));
                Period period2 = cycleSpace1.getCurrentPeriod();
                assertThat(period2.getEndTime() <= cycleSpace1.getEndTime(), is(true));

                assertThat(cycleSpace1.findPeriod(period1.getStartTime() - 1), nullValue());
                assertThat(cycleSpace1.findPeriod(period1.getStartTime()) == period1, is(true));
                assertThat(cycleSpace1.findPeriod(Long.MAX_VALUE) == period1, is(true));

                assertThat(period3.getSpace() != cycleSpace1, is(true));
                PeriodSpace cycleSpace2 = period3.getSpace();
                assertThat(cycleSpace2.getSchema() == cycleSchema, is(true));
                assertThat(cycleSchema.getCurrentCycle().getSpace().getFileIndex() == cycleSpace2.getFileIndex(), is(true));

                cycles = cycleSchema.getCycles();
                it = cycles.iterator();
                assertThat(it.hasNext(), is(true));
                assertThat(it.next(), is((IPeriodCycle) cycleSchema.getCurrentCycle()));
                assertThat(it.hasNext(), is(true));
                assertThat(it.next() == cycle1, is(true));
                assertThat(it.hasNext(), is(false));

                assertThat(cycleSpace2.getPreviousCycle() == cycle1, is(true));
                assertThat(cycleSpace2.getStartTime() >= t && cycleSpace2.getStartTime() <= Times.getCurrentTime(), is(true));
                assertThat(cycleSpace2.getEndTime(), is(0l));
                assertThat(cycleSpace2.getPeriodsCount(), is(1));
                assertThat(cycleSpace2.getPeriod(0).getPeriodIndex(), is(0));
                assertThat(cycleSpace2.getCurrentPeriod() == period3, is(true));
                assertThat(period3.getPeriodIndex(), is(0));
                assertThat(period3.getSpace() == cycleSpace2, is(true));
                assertThat(period3.getStartTime() >= cycleSpace2.getStartTime(), is(true));
                assertThat(period3.getEndTime(), is(0l));
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycleSpace1 = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycleSpace1 != null, is(true));
                assertThat(cycleSpace1.getPeriodsCount(), is(1));
                assertThat(cycleSpace1.getCurrentPeriod().getPeriodIndex(), is(0));
                assertThat(cycleSpace1.getPeriod(0) == cycleSpace1.getCurrentPeriod(), is(true));
                assertThat(cycleSpace1.findPeriod(0), nullValue());
                assertThat(cycleSpace1.findPeriod(Long.MAX_VALUE) == cycleSpace1.getCurrentPeriod(), is(true));
                assertThat(cycleSpace1.findPeriod(cycleSpace1.getCurrentPeriod().getStartTime()) == cycleSpace1.getCurrentPeriod(), is(true));
            }
        });
    }

    @Test
    public void testSchemaChange() throws Throwable {
        PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1, 2, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space2 = new PeriodSpaceSchemaConfiguration("space2", "space2", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1, 2, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space3 = new PeriodSpaceSchemaConfiguration("space3", "space3", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1, space2, space3)));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        PeriodSpaceSchemaConfiguration space22 = new PeriodSpaceSchemaConfiguration("space2", "space2", null,
                Arrays.asList(new PeriodSchemaConfiguration("p2", new HashSet(Arrays.asList(node1)),
                        null, null, 1, 2, false, null)), 0, 0);
        PeriodSpaceSchemaConfiguration space4 = new PeriodSpaceSchemaConfiguration("space4", "space4", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 1, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space22, space3, space4)));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1), configuration2), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema22 = transaction.getSchemas().get(1).findDomain("test").findSpace("space2");
                CycleSchema cycleSchema22 = (CycleSchema) periodSchema22.getCycles().get(0);
                PeriodSpace cycle22 = cycleSchema22.getCurrentCycle().getSpace();
                assertThat(cycle22.getPreviousCycle(), nullValue());

                IPeriodSpaceSchema periodSchema33 = transaction.getSchemas().get(1).findDomain("test").findSpace("space3");
                CycleSchema cycleSchema33 = (CycleSchema) periodSchema33.getCycles().get(0);
                PeriodSpace cycle33 = cycleSchema33.getCurrentCycle().getSpace();
                assertThat(cycle33.getPreviousCycle(), nullValue());

                IPeriodSpaceSchema periodSchema4 = transaction.getSchemas().get(1).findDomain("test").findSpace("space4");
                CycleSchema cycleSchema4 = (CycleSchema) periodSchema4.getCycles().get(0);
                PeriodSpace cycle4 = cycleSchema4.getCurrentCycle().getSpace();
                assertThat(cycle4.getPreviousCycle(), nullValue());
            }
        });
    }

    @Test
    public void testNonStructuredSchemaChange() throws Throwable {
        PeriodTestNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", "node1", null,
                new IndexedLocationFieldSchemaConfiguration("location"),
                Arrays.asList(new StringFieldSchemaConfiguration("field2", 256), new JsonFieldSchemaConfiguration("field3"),
                        new SingleReferenceFieldSchemaConfiguration("field4", null)), null);
        NodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2", "node2", null,
                new IndexedLocationFieldSchemaConfiguration("location"), Arrays.asList(
                new ReferenceFieldSchemaConfiguration("field2", null)), null);
        NodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3", "node3", null,
                new IndexedLocationFieldSchemaConfiguration("location"),
                Arrays.asList(
                        new ReferenceFieldSchemaConfiguration("field2", null)), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", "",
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2,
                        nodeConfiguration3)), "node2", null, 1, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final int COUNT = 10000;
        final int[] fileIndex = new int[1];
        final long[] ids = new long[COUNT];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);
                PeriodSpace space = cycleSchema.getCurrentCycle().getSpace();
                Period period = space.getCurrentPeriod();

                TestPeriodNode node = period.getRootNode();
                IReferenceField<TestPeriodNode> field = node.node.getField(1);

                period.addNode(new Location(0, 0), 2);

                TestPeriodNode[] nodes = new TestPeriodNode[COUNT];
                Set<TestPeriodNode> nodesSet = new HashSet<TestPeriodNode>();
                for (int i = 0; i < COUNT; i++) {
                    TestPeriodNode node1 = period.addNode(new Location(0, i), 0);
                    ids[i] = node1.node.getId();
                    nodes[i] = node1;
                    nodesSet.add(node1);
                    IStringField field2 = node1.node.getField(1);
                    field2.set("Hello world!!!");
                    IJsonField field3 = node1.node.getField(2);
                    field3.set(Json.object().put("key", i).toObject());
                    ISingleReferenceField field4 = node1.node.getField(3);
                    field4.set(node);
                    field.add(node1);
                }

                fileIndex[0] = space.getFileIndex();
            }
        });

        NodeSchemaConfiguration nodeConfiguration4 = new PeriodTestNodeSchemaConfiguration("anode4", "node4Alias", "node4Desciption",
                new IndexedLocationFieldSchemaConfiguration("location"), Arrays.<FieldSchemaConfiguration>asList(), null);
        nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1", "node1Alias", "node1Desciption",
                new IndexedLocationFieldSchemaConfiguration("location"), Arrays.asList(
                new StringFieldSchemaConfiguration("field2", 256),
                new JsonFieldSchemaConfiguration("field3"),
                new SingleReferenceFieldSchemaConfiguration("field4", null)), null);

        nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2", "node2", null,
                new IndexedLocationFieldSchemaConfiguration("location"),
                Arrays.asList(
                        new ReferenceFieldSchemaConfiguration("field2", null)), null);
        nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3", "node3", null, new IndexedLocationFieldSchemaConfiguration("location"),
                Arrays.asList(
                        new ReferenceFieldSchemaConfiguration("field2", null)), null);
        space1 = new PeriodSpaceSchemaConfiguration("space1", "space1Alias", "space1Description",
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration4, nodeConfiguration1, nodeConfiguration2,
                        nodeConfiguration3)), "node2", null, 1, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration2 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1), configuration2), null);
            }

            @Override
            public int getSize() {
                return 1;
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);
                PeriodSpace space = cycleSchema.getCurrentCycle().getSpace();
                Period period = space.getCurrentPeriod();

                assertThat(transaction.getCurrentSchema().findDomain("test").findSpaceByAlias("space1Alias") == periodSchema, is(true));
                NodeSchema nodeSchema = (NodeSchema) cycleSchema.findNode("node1");
                assertThat(cycleSchema.findNodeByAlias("node1Alias") == nodeSchema, is(true));
                assertThat(nodeSchema.getIndex(), is(0));
                assertThat(cycleSchema.findNode("anode4").getIndex(), is(3));

                IFieldSchema fieldSchema = nodeSchema.findField("field1");
                assertThat(nodeSchema.findFieldByAlias("field1Alias") == fieldSchema, is(true));

                assertThat(space.getFileIndex(), is(fileIndex[0]));

                for (int i = 0; i < COUNT; i++) {
                    TestPeriodNode node1 = period.findNodeById(ids[i]);
                    assertThat(node1.node.getSchema() == nodeSchema, is(true));
                }
            }
        });
    }

    @Test
    public void testCycleReconciling() throws Throwable {
        IOs.close(database);
        builder.setTimerPeriod(10);
        configuration = builder.toConfiguration();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 500, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
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

                PeriodSpace cycle1 = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycle1 != null, is(true));
                assertThat(cycle1.getFileIndex(), is(14));
                assertThat(cycle1.getPeriodsCount(), is(1));
            }
        });

        database.close();

        Thread.sleep(1300);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        Thread.sleep(1300);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle1 = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycle1 != null, is(true));
                assertThat(cycle1.getFileIndex(), is(24));
                assertThat(cycle1.getPeriodsCount() >= 1 && cycle1.getPeriodsCount() <= 2, is(true));
            }
        });

        database.close();

        Thread.sleep(1300);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodSpace cycle1 = cycleSchema.getCurrentCycle().getSpace();
                assertThat(cycle1 != null, is(true));
                assertThat(cycle1.getPeriodsCount() >= 1 && cycle1.getPeriodsCount() <= 2, is(true));

                PeriodSpace cycle2 = cycle1.getPreviousCycle().getSpace();
                Period period = cycle2.getPeriod(1);
                assertThat(period.isClosed(), is(true));
            }
        });
    }

    @Test
    public void testTimerCycleReconciling() throws Throwable {
        IOs.close(database);
        builder.setTimerPeriod(10);
        configuration = builder.toConfiguration();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        PeriodNodeSchemaConfiguration node1 = new PeriodNodeSchemaConfiguration("node1",
                new IndexedLocationFieldSchemaConfiguration("field"),
                Arrays.asList(new TestFieldSchemaConfiguration("field1")), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(node1)),
                        null, null, 500, 100, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodCycle cycle1 = cycleSchema.getCurrentCycle();
                assertThat(cycle1, nullValue());
            }
        });

        Thread.sleep(13000);

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodCycle cycle1 = cycleSchema.getCurrentCycle();
                assertThat(cycle1 != null, is(true));
                assertThat(cycle1.getSpace().getFileIndex(), is(14));
                assertThat(cycle1.getSpace().getPeriodsCount() >= 1, is(true));
            }
        });

        Thread.sleep(13500);

        database.transactionSync(new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                PeriodCycle cycle1 = cycleSchema.getCurrentCycle();
                assertThat(cycle1 != null, is(true));
                assertThat(cycle1.getSpace().getFileIndex(), is(14));
                assertThat(cycle1.getSpace().getPeriodsCount() >= 2, is(true));
            }
        });
    }
}
