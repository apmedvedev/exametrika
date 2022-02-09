/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.aggregator.Location;
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
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.INodeSortedIndex;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField.IStructuredIterator;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.objectdb.config.schema.RecordIndexerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.PeriodTestNodeSchemaConfiguration;
import com.exametrika.tests.aggregator.perfdb.PeriodNodeTests.TestPeriodNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link StructuredBlobTests} are tests for structured blob field.
 *
 * @author Medvedev-A
 */
public class StructuredBlobTests {
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
    public void testPeriodStructuredBlobField() {
        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodTestNodeSchemaConfiguration("node1",
                Arrays.asList(new BlobStoreFieldSchemaConfiguration("field2", "field2", null, 0, Integer.MAX_VALUE, null,
                        PageType.NORMAL, false, Collections.<String, String>emptyMap(), true)));
        PeriodNodeSchemaConfiguration nodeConfiguration2 = new PeriodTestNodeSchemaConfiguration("node2",
                Arrays.asList(new StructuredBlobFieldSchemaConfiguration("field3", "field3", null, "node1", "field2", true, true,
                        Collections.singleton(TestRecord.class.getName()), false, 0,
                        Arrays.asList(new StructuredBlobIndexSchemaConfiguration("index1", 0, IndexType.BTREE,
                                        false, 256, new StringKeyNormalizerSchemaConfiguration(), true, true, "testIndex1"),
                                new StructuredBlobIndexSchemaConfiguration("index2", 0, IndexType.BTREE,
                                        true, 8, new NumericKeyNormalizerSchemaConfiguration(NumericKeyNormalizerSchemaConfiguration.DataType.LONG),
                                        true, true, "testIndex2")), true, new TestRecordIndexerSchemaConfiguration())));
        PeriodNodeSchemaConfiguration nodeConfiguration3 = new PeriodTestNodeSchemaConfiguration("node3",
                Arrays.asList(new IndexedStringFieldSchemaConfiguration("field1", true, true, 0, 256, null, null, null, 0,
                        null, false, false, false, false, false, null, true, false, null, null)));
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1, nodeConfiguration2, nodeConfiguration3)),
                        null, null, 10000000, 100, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        final long[] ids = new long[3];
        final long[] rids2 = new long[10];
        final long[] rids3 = new long[10];
        final long[] nids = new long[10];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                Period period = (Period) periodSchema.findCycle("p1").getCurrentCycle().getSpace().getCurrentPeriod();

                TestPeriodNode node1 = period.addNode(new Location(1, 1), 0);
                ids[0] = node1.node.getId();

                TestPeriodNode node2 = period.findOrCreateNode(new Location(2, 2), period.getSpace().getSchema().findNode("node2"), node1);
                IStructuredBlobField<TestRecord> field2 = node2.node.getField(1);
                assertThat(field2.getStore() == node1, is(true));
                ids[1] = node2.node.getId();

                TestPeriodNode node3 = period.addNode(new Location(3, 3), 1);
                IStructuredBlobField<TestRecord> field3 = node3.node.getField(1);
                assertThat(field3.getStore(), nullValue());
                field3.setStore(node1);
                assertThat(field2.getStore() == node1, is(true));
                ids[2] = node3.node.getId();

                for (int i = 0; i < 10; i++) {
                    rids2[i] = field2.add(new TestRecord("test-" + i, i));
                    rids3[i] = field3.add(new TestRecord("test2-" + 2 * i, 2 * i));

                    TestPeriodNode node = period.addNode(new Location(i, i), 2);
                    IStringField field = node.node.getField(1);
                    field.set("node-" + i);
                    nids[i] = node.node.getId();
                }

                assertThat(field2.getCurrent(), is(new TestRecord("test-" + 9, 9)));
                assertThat(field3.getCurrent(), is(new TestRecord("test2-" + 2 * 9, 2 * 9)));

                for (int i = 0; i < 10; i++) {
                    assertThat(field2.get(rids2[i]), is(new TestRecord("test-" + i, i)));
                    assertThat(field3.get(rids3[i]), is(new TestRecord("test2-" + 2 * i, 2 * i)));
                }

                IStructuredIterator<TestRecord> it2 = field2.getRecords().iterator();
                assertThat(it2.getStartId(), is(rids2[0]));
                IStructuredIterator<TestRecord> it3 = field3.getRecords().iterator();
                assertThat(it3.getStartId(), is(rids3[0]));

                IStructuredIterator<TestRecord> it4 = field2.getRecords(rids2[3], rids2[7]).iterator();
                assertThat(it4.getStartId(), is(rids2[3]));
                assertThat(it4.getEndId(), is(rids2[7]));

                IStructuredIterator<TestRecord> rit2 = field2.getReverseRecords().iterator();
                assertThat(rit2.getStartId(), is(rids2[9]));
                assertThat(rit2.getEndId(), is(rids2[0]));
                IStructuredIterator<TestRecord> rit3 = field3.getReverseRecords().iterator();
                assertThat(rit3.getStartId(), is(rids3[9]));
                assertThat(rit3.getEndId(), is(rids3[0]));

                IStructuredIterator<TestRecord> rit4 = field2.getReverseRecords(rids2[7], rids2[3]).iterator();
                assertThat(rit4.getStartId(), is(rids2[7]));
                assertThat(rit4.getEndId(), is(rids2[3]));

                for (int i = 0; i < 10; i++) {
                    assertThat(it2.hasNext(), is(true));
                    assertThat(it3.hasNext(), is(true));

                    assertThat(it2.next(), is(new TestRecord("test-" + i, i)));
                    assertThat(it3.next(), is(new TestRecord("test2-" + 2 * i, 2 * i)));

                    assertThat(it2.getId(), is(rids2[i]));
                    assertThat(it3.getId(), is(rids3[i]));

                    assertThat(it2.get(), is(new TestRecord("test-" + i, i)));
                    if (i > 0)
                        assertThat(it2.getPrevious(), is(new TestRecord("test-" + (i - 1), i - 1)));
                    else
                        assertThat(it2.getPrevious(), nullValue());
                    assertThat(it3.get(), is(new TestRecord("test2-" + 2 * i, 2 * i)));
                }

                for (int i = 3; i < 8; i++) {
                    assertThat(it4.hasNext(), is(true));
                    assertThat(it4.next(), is(new TestRecord("test-" + i, i)));
                }

                for (int i = 9; i >= 0; i--) {
                    assertThat(rit2.hasNext(), is(true));
                    assertThat(rit3.hasNext(), is(true));

                    assertThat(rit2.next(), is(new TestRecord("test-" + i, i)));
                    assertThat(rit3.next(), is(new TestRecord("test2-" + 2 * i, 2 * i)));

                    assertThat(rit2.getId(), is(rids2[i]));
                    assertThat(rit3.getId(), is(rids3[i]));

                    assertThat(rit2.get(), is(new TestRecord("test-" + i, i)));
                    if (i < 9)
                        assertThat(rit2.getPrevious(), is(new TestRecord("test-" + (i + 1), i + 1)));
                    else
                        assertThat(rit2.getPrevious(), nullValue());
                    assertThat(rit3.get(), is(new TestRecord("test2-" + 2 * i, 2 * i)));
                }

                for (int i = 7; i >= 3; i--) {
                    assertThat(rit4.hasNext(), is(true));
                    assertThat(rit4.next(), is(new TestRecord("test-" + i, i)));
                }

                assertThat(it2.hasNext(), is(false));
                assertThat(it3.hasNext(), is(false));
                assertThat(it4.hasNext(), is(false));

                it4.setNext(it4.getStartId());
                assertThat(it4.next(), is(new TestRecord("test-" + 3, 3)));

                it4.setNext(it4.getEndId());
                assertThat(it4.hasNext(), is(true));

                it4.setNext(rids2[5]);
                assertThat(it4.next(), is(new TestRecord("test-" + 5, 5)));

                assertThat(rit2.hasNext(), is(false));
                assertThat(rit3.hasNext(), is(false));
                assertThat(rit4.hasNext(), is(false));

                rit4.setNext(rit4.getStartId());
                assertThat(rit4.next(), is(new TestRecord("test-" + 7, 7)));

                rit4.setNext(rit4.getEndId());
                assertThat(rit4.hasNext(), is(true));

                rit4.setNext(rids2[5]);
                assertThat(rit4.next(), is(new TestRecord("test-" + 5, 5)));

                checkIndexes(rids2, rids3, field2, field3);
            }
        });

        database.close();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");

                PeriodSpace space = (PeriodSpace) periodSchema.findCycle("p1").getCurrentCycle().getSpace();
                Period period = space.getCurrentPeriod();

                TestPeriodNode node1 = period.findNodeById(ids[0]);

                TestPeriodNode node2 = period.findNodeById(ids[1]);
                IStructuredBlobField<TestRecord> field2 = node2.node.getField(1);
                assertThat(field2.getStore() == node1, is(true));

                TestPeriodNode node3 = period.findNodeById(ids[2]);
                IStructuredBlobField<TestRecord> field3 = node3.node.getField(1);
                assertThat(field3.getStore() == node1, is(true));

                assertThat(field2.getCurrent(), is(new TestRecord("test-" + 9, 9)));
                assertThat(field3.getCurrent(), is(new TestRecord("test2-" + 2 * 9, 2 * 9)));

                for (int i = 0; i < 10; i++) {
                    assertThat(field2.get(rids2[i]), is(new TestRecord("test-" + i, i)));
                    assertThat(field3.get(rids3[i]), is(new TestRecord("test2-" + 2 * i, 2 * i)));
                }

                IStructuredIterator<TestRecord> it2 = field2.getRecords().iterator();
                assertThat(it2.getStartId(), is(rids2[0]));
                IStructuredIterator<TestRecord> it3 = field3.getRecords().iterator();
                assertThat(it3.getStartId(), is(rids3[0]));

                IStructuredIterator<TestRecord> it4 = field2.getRecords(rids2[3], rids2[7]).iterator();
                assertThat(it4.getStartId(), is(rids2[3]));
                assertThat(it4.getEndId(), is(rids2[7]));

                IStructuredIterator<TestRecord> rit2 = field2.getReverseRecords().iterator();
                assertThat(rit2.getStartId(), is(rids2[9]));
                assertThat(rit2.getEndId(), is(rids2[0]));
                IStructuredIterator<TestRecord> rit3 = field3.getReverseRecords().iterator();
                assertThat(rit3.getStartId(), is(rids3[9]));
                assertThat(rit3.getEndId(), is(rids3[0]));

                IStructuredIterator<TestRecord> rit4 = field2.getReverseRecords(rids2[7], rids2[3]).iterator();
                assertThat(rit4.getStartId(), is(rids2[7]));
                assertThat(rit4.getEndId(), is(rids2[3]));

                for (int i = 0; i < 10; i++) {
                    assertThat(it2.hasNext(), is(true));
                    assertThat(it3.hasNext(), is(true));

                    assertThat(it2.next(), is(new TestRecord("test-" + i, i)));
                    assertThat(it3.next(), is(new TestRecord("test2-" + 2 * i, 2 * i)));

                    assertThat(it2.getId(), is(rids2[i]));
                    assertThat(it3.getId(), is(rids3[i]));

                    assertThat(it2.get(), is(new TestRecord("test-" + i, i)));
                    if (i > 0)
                        assertThat(it2.getPrevious(), is(new TestRecord("test-" + (i - 1), i - 1)));
                    else
                        assertThat(it2.getPrevious(), nullValue());
                    assertThat(it3.get(), is(new TestRecord("test2-" + 2 * i, 2 * i)));
                }

                for (int i = 3; i < 8; i++) {
                    assertThat(it4.hasNext(), is(true));
                    assertThat(it4.next(), is(new TestRecord("test-" + i, i)));
                }

                for (int i = 9; i >= 0; i--) {
                    assertThat(rit2.hasNext(), is(true));
                    assertThat(rit3.hasNext(), is(true));

                    assertThat(rit2.next(), is(new TestRecord("test-" + i, i)));
                    assertThat(rit3.next(), is(new TestRecord("test2-" + 2 * i, 2 * i)));

                    assertThat(rit2.getId(), is(rids2[i]));
                    assertThat(rit3.getId(), is(rids3[i]));

                    assertThat(rit2.get(), is(new TestRecord("test-" + i, i)));
                    if (i < 9)
                        assertThat(rit2.getPrevious(), is(new TestRecord("test-" + (i + 1), i + 1)));
                    else
                        assertThat(rit2.getPrevious(), nullValue());
                    assertThat(rit3.get(), is(new TestRecord("test2-" + 2 * i, 2 * i)));
                }

                for (int i = 7; i >= 3; i--) {
                    assertThat(rit4.hasNext(), is(true));
                    assertThat(rit4.next(), is(new TestRecord("test-" + i, i)));
                }

                assertThat(it2.hasNext(), is(false));
                assertThat(it3.hasNext(), is(false));
                assertThat(it4.hasNext(), is(false));

                it4.setNext(it4.getStartId());
                assertThat(it4.next(), is(new TestRecord("test-" + 3, 3)));

                it4.setNext(it4.getEndId());
                assertThat(it4.hasNext(), is(true));

                it4.setNext(rids2[5]);
                assertThat(it4.next(), is(new TestRecord("test-" + 5, 5)));

                assertThat(rit2.hasNext(), is(false));
                assertThat(rit3.hasNext(), is(false));
                assertThat(rit4.hasNext(), is(false));

                rit4.setNext(rit4.getStartId());
                assertThat(rit4.next(), is(new TestRecord("test-" + 7, 7)));

                rit4.setNext(rit4.getEndId());
                assertThat(rit4.hasNext(), is(true));

                rit4.setNext(rids2[5]);
                assertThat(rit4.next(), is(new TestRecord("test-" + 5, 5)));

                checkIndexes(rids2, rids3, field2, field3);

                INodeFullTextIndex fullText2 = field2.getFullTextIndex();
                INodeFullTextIndex fullText3 = field3.getFullTextIndex();
                TestRecordIndexer indexer2 = (TestRecordIndexer) field2.getRecordIndexer();
                TestRecordIndexer indexer3 = (TestRecordIndexer) field3.getRecordIndexer();

                INodeSearchResult result = fullText2.search(Queries.term("field1", "test-5").toQuery(indexer2.documentSchema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat((Pair<Long, TestRecord>) result.getTopElements().get(0).get(), is(new Pair<Long, TestRecord>(rids2[5], new TestRecord("test-" + 5, 5))));

                result = fullText3.search(Queries.term("field1", "test2-10").toQuery(indexer3.documentSchema), 1);
                assertThat(result.getTotalCount(), is(1));
                assertThat((Pair<Long, TestRecord>) result.getTopElements().get(0).get(), is(new Pair<Long, TestRecord>(rids3[5], new TestRecord("test2-" + 10, 10))));

                result = fullText2.search(Queries.expression("field1", "*:*").toQuery(indexer2.documentSchema), new Sort("field2"), 100);
                assertThat(result.getTotalCount(), is(10));
                for (int i = 0; i < 10; i++)
                    assertThat((Pair<Long, TestRecord>) result.getTopElements().get(i).get(), is(new Pair<Long, TestRecord>(rids2[i], new TestRecord("test-" + i, i))));

                result = fullText3.search(Queries.expression("field1", "*:*").toQuery(indexer3.documentSchema), new Sort("field2"), 100);
                assertThat(result.getTotalCount(), is(10));
                for (int i = 0; i < 10; i++)
                    assertThat((Pair<Long, TestRecord>) result.getTopElements().get(i).get(), is(new Pair<Long, TestRecord>(rids3[i], new TestRecord("test2-" + 2 * i, 2 * i))));

                IDocumentSchema fullTextSchema = space.getSchema().findNode("node3").getFullTextSchema();
                result = space.getFullTextIndex().search(Queries.expression("field1", "*:*").toQuery(fullTextSchema), new Sort("field1"), 100);
                assertThat(result.getTotalCount(), is(10));
                for (int i = 0; i < 10; i++)
                    assertThat(result.getTopElements().get(i).get(), is((Object)period.findNodeById(nids[i])));
            }
        });
    }

    private void checkIndexes(final long[] rids2, final long[] rids3, IStructuredBlobField<TestRecord> field2,
                              IStructuredBlobField<TestRecord> field3) {
        INodeSortedIndex<String, Pair<Long, TestRecord>> index21 = (INodeSortedIndex<String, Pair<Long, TestRecord>>) field2.getIndex(0);
        INodeSortedIndex<Long, Pair<Long, TestRecord>> index22 = (INodeSortedIndex<Long, Pair<Long, TestRecord>>) field2.getIndex(1);

        INodeSortedIndex<String, Pair<Long, TestRecord>> index31 = (INodeSortedIndex<String, Pair<Long, TestRecord>>) field3.getIndex(0);
        INodeSortedIndex<Long, Pair<Long, TestRecord>> index32 = (INodeSortedIndex<Long, Pair<Long, TestRecord>>) field3.getIndex(1);

        assertThat(index21.findFirstValue(), is(new Pair<Long, TestRecord>(rids2[0], new TestRecord("test-" + 0, 0))));
        assertThat(index21.findLastValue(), is(new Pair<Long, TestRecord>(rids2[9], new TestRecord("test-" + 9, 9))));
        assertThat(index21.findFloorValue(null, true), is(new Pair<Long, TestRecord>(rids2[9], new TestRecord("test-" + 9, 9))));
        assertThat(index21.findFloorValue(null, false), is(new Pair<Long, TestRecord>(rids2[8], new TestRecord("test-" + 8, 8))));
        assertThat(index21.findFloorValue("test3", false), is(new Pair<Long, TestRecord>(rids2[9], new TestRecord("test-" + 9, 9))));
        assertThat(index21.findFloorValue("test", false), nullValue());
        assertThat(index21.findCeilingValue(null, true), is(new Pair<Long, TestRecord>(rids2[0], new TestRecord("test-" + 0, 0))));
        assertThat(index21.findCeilingValue(null, false), is(new Pair<Long, TestRecord>(rids2[1], new TestRecord("test-" + 1, 1))));
        List list1 = com.exametrika.common.utils.Collections.toList(index21.findValues(null, true, null, true).iterator());
        List list2 = com.exametrika.common.utils.Collections.toList(index21.findValues(null, false, null, false).iterator());
        List list3 = com.exametrika.common.utils.Collections.toList(index21.findValues("test-2", true, "test-4", true).iterator());
        List list4 = com.exametrika.common.utils.Collections.toList(index21.findValues("test-2", false, "test-4", false).iterator());
        checkList(rids2, list1, 0, 10);
        checkList(rids2, list2, 1, 8);
        checkList(rids2, list3, 2, 3);
        checkList(rids2, list4, 3, 1);

        assertThat(index31.findFirstValue(), is(new Pair<Long, TestRecord>(rids3[0], new TestRecord("test2-" + 0, 0))));
        assertThat(index31.findLastValue(), is(new Pair<Long, TestRecord>(rids3[4], new TestRecord("test2-" + 8, 8))));
        assertThat(index31.findFloorValue(null, true), is(new Pair<Long, TestRecord>(rids3[4], new TestRecord("test2-" + 8, 8))));
        assertThat(index31.findFloorValue(null, false), is(new Pair<Long, TestRecord>(rids3[3], new TestRecord("test2-" + 6, 6))));
        assertThat(index31.findFloorValue("test3", false), is(new Pair<Long, TestRecord>(rids3[4], new TestRecord("test2-" + 8, 8))));
        assertThat(index31.findFloorValue("test", false), nullValue());
        assertThat(index31.findCeilingValue(null, true), is(new Pair<Long, TestRecord>(rids3[0], new TestRecord("test2-" + 0, 0))));
        assertThat(index31.findCeilingValue(null, false), is(new Pair<Long, TestRecord>(rids3[5], new TestRecord("test2-" + 10, 10))));
        list1 = com.exametrika.common.utils.Collections.toList(index31.findValues(null, true, null, true).iterator());
        list2 = com.exametrika.common.utils.Collections.toList(index31.findValues(null, false, null, false).iterator());
        list3 = com.exametrika.common.utils.Collections.toList(index31.findValues("test2-4", true, "test2-8", true).iterator());
        list4 = com.exametrika.common.utils.Collections.toList(index31.findValues("test2-4", false, "test2-8", false).iterator());
        assertThat(list1.size(), is(10));
        assertThat(list2.size(), is(8));
        assertThat(list3.size(), is(3));
        assertThat(list4.size(), is(1));

        for (int i = 0; i < 10; i++) {
            assertThat(index21.find("test-" + i), is(new Pair<Long, TestRecord>(rids2[i], new TestRecord("test-" + i, i))));
            assertThat(index22.find((long) i), is(new Pair<Long, TestRecord>(rids2[i], new TestRecord("test-" + i, i))));

            assertThat(index31.find("test2-" + (2 * i)), is(new Pair<Long, TestRecord>(rids3[i], new TestRecord("test2-" + (2 * i), 2 * i))));
            assertThat(index32.find(2l * i), is(new Pair<Long, TestRecord>(rids3[i], new TestRecord("test2-" + (2 * i), 2 * i))));

            assertThat(index21.find("test2-" + (2 * i)), nullValue());
            assertThat(index31.find("test-" + i), nullValue());
        }
    }

    private void checkList(final long[] rids2, List<Pair<Long, TestRecord>> list, int start, int count) {
        for (int i = 0; i < count; i++)
            assertThat(list.get(i), is(new Pair<Long, TestRecord>(rids2[i + start], new TestRecord("test-" + (i + start), (i + start)))));
    }

    private static class TestRecord implements Serializable {
        private final String field1;
        private final long field2;

        public TestRecord(String field1, long field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRecord))
                return false;

            TestRecord record = (TestRecord) o;
            return field1.equals(record.field1) && field2 == record.field2;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(field1, field2);
        }

        @Override
        public String toString() {
            return field1 + ":" + field2;
        }
    }

    public static final class TestRecordIndexerSchemaConfiguration extends RecordIndexerSchemaConfiguration {
        @Override
        public IRecordIndexer createIndexer(IField field, IRecordIndexProvider indexProvider) {
            return new TestRecordIndexer(field, indexProvider);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRecordIndexerSchemaConfiguration))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    private static class TestRecordIndexer implements IRecordIndexer {
        private final IRecordIndexProvider indexProvider;
        private final IDocumentSchema documentSchema;

        public TestRecordIndexer(IField field, IRecordIndexProvider indexProvider) {
            this.indexProvider = indexProvider;
            this.documentSchema = indexProvider.createDocumentSchema(new DocumentSchemaConfiguration("test", Arrays.asList(
                    new StringFieldSchemaConfiguration("field1", Enums.of(Option.INDEX_DOCUMENTS, Option.INDEXED, Option.OMIT_NORMS), null),
                    new NumericFieldSchemaConfiguration("field2", DataType.LONG, true, true)),
                    new StandardAnalyzerSchemaConfiguration()));
        }

        @Override
        public void addRecord(Object r, long id) {
            TestRecord record = (TestRecord) r;

            indexProvider.add(0, record.field1, id);
            indexProvider.add(1, record.field2, id);
            indexProvider.add(documentSchema, id, record.field1, record.field2);
        }

        @Override
        public void removeRecord(Object r) {
            TestRecord record = (TestRecord) r;

            indexProvider.remove(0, record.field1);
            indexProvider.remove(1, record.field2);
        }

        @Override
        public void reindex(Object r, long id) {
            TestRecord record = (TestRecord) r;
            indexProvider.add(documentSchema, id, record.field1, record.field2);
        }
    }
}
