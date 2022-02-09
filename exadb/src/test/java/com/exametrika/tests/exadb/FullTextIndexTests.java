/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.fulltext.FieldSort.Kind;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.ISearchResult;
import com.exametrika.api.exadb.fulltext.ISearchResultElement;
import com.exametrika.api.exadb.fulltext.IStringField;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength;
import com.exametrika.api.exadb.fulltext.config.schema.Documents;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.fulltext.IFullTextIndexControl;
import com.exametrika.spi.exadb.fulltext.IndexQuery;
import com.exametrika.spi.exadb.index.IIndexDatabaseExtension;


/**
 * The {@link FullTextIndexTests} are tests for fulltext indexing support.
 *
 * @author Medvedev-A
 */
public class FullTextIndexTests {
    private Database database;
    private DatabaseConfiguration configuration;
    private DatabaseConfigurationBuilder builder;
    private IDatabaseFactory.Parameters parameters;
    private IIndexManager indexManager;

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

        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testFullTextIndex() throws Throwable {
        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema = Documents.doc()
                .stringField("field1").indexed().stored().end()
                .numericField("field2").stored().type(DataType.INT).end()
                .textField("field3").tokenized().end()
                .stringField("field4").tokenized().end()
                .toSchema();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i <= 1000; i++) {
                    index.add(schema.createDocument("value" + i, i,
                            new StringReader("Hello world with " + i), "String text " + i));
                }
            }
        });

        Thread.sleep(200);

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                assertThat(index.search(Queries.term("field1", "value100").toQuery(schema), 10).getTotalCount(), is(1));
                assertThat(index.search(Queries.term("field2", "100").toQuery(schema), 10).getTotalCount(), is(1));
                assertThat(index.search(Queries.expression("field3", "Hello*").toQuery(schema), 10).getTotalCount(), is(1001));
                assertThat(index.search(Queries.expression("field4", "\"String text 10\"").toQuery(schema), 10).getTotalCount(), is(1));
                assertThat(index.search(new IndexQuery(schema, new MatchAllDocsQuery()), 10).getTotalCount(), is(1001));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i <= 1000; i++) {
                    index.update("field2", Integer.toString(i), schema.createDocument("value" + (i + 10000), i + 10000,
                            new StringReader("Hello world with " + (i + 10000)), "String text " + (i + 10000)));
                }
            }
        });

        Thread.sleep(200);

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                assertThat(index.search(Queries.term("field1", "value11000").toQuery(schema), 10).getTotalCount(), is(1));
                assertThat(index.search(Queries.term("field2", "11000").toQuery(schema), 10).getTotalCount(), is(1));
                assertThat(index.search(new IndexQuery(schema, new MatchAllDocsQuery()), 10).getTotalCount(), is(1001));
                assertThat(index.search(Queries.expression("field4", "\"String text 11000\"").toQuery(schema), 10).getTotalCount(), is(1));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i <= 1000; i++) {
                    index.remove("field2", Integer.toString(i + 10000));
                }
            }
        });

        Thread.sleep(200);

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                assertThat(index.search(new IndexQuery(schema, new MatchAllDocsQuery()), 10).getTotalCount(), is(0));
            }
        });
    }

    @Test
    public void testSearch() throws Throwable {
        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema = Documents.doc().numericField("field").stored().type(DataType.INT).end().toSchema();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < 1000; i++) {
                    index.add(schema.createDocument(Arrays.asList(999 - i)));
                    Threads.sleep(10);
                }
            }
        });

        Thread.sleep(200);

        database.close();
        builder.setTimerPeriod(10);
        builder.addExtension(new IndexDatabaseExtensionConfiguration(600000, new FullTextIndexConfiguration(60000,
                100, 60000, 16000000)));
        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        final ISearchResult result[] = new ISearchResult[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                result[0] = index.search(Queries.term("field", "100").toQuery(schema), 1);
                index.add(schema.createDocument(Arrays.asList(1000)));
            }
        });

        Thread.sleep(200);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            result[0].getTopElements().get(0).getDocument();
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                ISearchResult result = index.search(Queries.term("field", "100").toQuery(schema), 1);
                assertThat(result.getSort(), nullValue());
                assertThat(result.getTotalCount(), is(1));
                List<ISearchResultElement> list = result.getTopElements();
                assertThat(list.size(), is(1));
                IDocument document = list.get(0).getDocument();
                INumericField field = document.findField("field");
                assertThat((Integer) field.get(), is(100));

                result = index.search(Queries.expression("field", "[100 TO 200}").toQuery(schema), new Sort(Kind.DOCUMENT), 1000);
                assertThat(result.getSort(), is(new Sort(Kind.DOCUMENT)));
                assertThat(result.getTotalCount(), is(100));
                list = result.getTopElements();
                assertThat(list.size(), is(100));

                for (int i = 0; i < 100; i++) {
                    document = list.get(i).getDocument();
                    field = document.findField("field");
                    assertThat((Integer) field.get(), is(199 - i));
                }

                result = index.search(Queries.expression("field", "[100 TO 200}").toQuery(schema), new Sort("field"), 1000);
                assertThat(result.getSort(), is(new Sort("field")));
                assertThat(result.getTotalCount(), is(100));
                list = result.getTopElements();
                assertThat(list.size(), is(100));

                for (int i = 0; i < 100; i++) {
                    document = list.get(i).getDocument();
                    field = document.findField("field");
                    assertThat((Integer) field.get(), is(i + 100));
                }

                result = index.search(Queries.expression("field", "[100 TO 200}").toQuery(schema), new Sort("field", false), 1000);
                assertThat(result.getSort(), is(new Sort("field", false)));
                assertThat(result.getTotalCount(), is(100));
                list = result.getTopElements();
                assertThat(list.size(), is(100));

                for (int i = 0; i < 100; i++) {
                    document = list.get(i).getDocument();
                    field = document.findField("field");
                    assertThat((Integer) field.get(), is(199 - i));
                }
            }
        });
    }

    @Test
    public void testICUAnalyzer() throws Throwable {
        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema1 = Documents.doc().stringField("field1").tokenized().stored()
                .analyzer(new CollationKeyAnalyzerSchemaConfiguration("ar", Strength.PRIMARY)).end().toSchema();
        final IDocumentSchema schema2 = Documents.doc().stringField("tracer").indexed().stored().end()
                .stringField("contents").tokenized().stored().analyzer(
                        new CollationKeyAnalyzerSchemaConfiguration("da_DK", Strength.PRIMARY)).end().toSchema();

        final String[] tracer = new String[]{"qA", "qB", "qC", "qD", "qE"};
        final String[] data = new String[]{"HAT", "HUT", "H\u00C5T", "H\u00D8T", "HOT"};
        final String[] sortedTracerOrder = new String[]{"qA", "qE", "qB", "qD", "qC"};

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                index.add(schema1.createDocument("\u0633\u0627\u0628"));
            }
        });

        Thread.sleep(200);

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                assertThat(index.search(Queries.expression("field1", "[ \u062F TO \u0698 ]").toQuery(schema1), 100).getTotalCount(), is(0));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < data.length; ++i)
                    index.add(schema2.createDocument(tracer[i], data[i]));
            }
        });

        Thread.sleep(200);

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                ISearchResult result = index.search(Queries.expression("tracer", "tracer:q*").toQuery(schema2), new Sort("contents"), 1000);
                int i = 0;
                assertThat(result.getTotalCount(), is(sortedTracerOrder.length));
                for (ISearchResultElement element : result.getTopElements()) {
                    IStringField field = element.getDocument().findField("tracer");
                    assertThat(field.get(), is(sortedTracerOrder[i]));
                    i++;
                }
            }
        });
    }

    @Test
    public void testIndexUnload() throws Throwable {
        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema = Documents.doc().numericField("field").stored().type(DataType.INT).end().toSchema();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < 1000; i++)
                    index.add(schema.createDocument(Arrays.asList(999 - i)));

                index.unload();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                ISearchResult result = index.search(Queries.term("field", "100").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
            }

            ;
        });
    }

    @Test
    public void testIndexDeletion() throws Throwable {
        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema = Documents.doc().numericField("field").stored().type(DataType.INT).end().toSchema();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < 1000; i++)
                    index.add(schema.createDocument(Arrays.asList(999 - i)));
            }
        });

        Thread.sleep(200);

        database.close();
        builder.setTimerPeriod(10);
        builder.addExtension(new IndexDatabaseExtensionConfiguration(600000, new FullTextIndexConfiguration(60000,
                3000, 2000, 16000000)));
        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        try {
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IFullTextIndex index = indexManager.getIndex(id[0]);
                    index.delete();
                    throw new RawRollbackException(new NonLocalizedMessage("test"));
                }
            });
        } catch (RawDatabaseException e) {
            assertThat(e.getCause() instanceof RawRollbackException, is(true));
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                ISearchResult result = index.search(Queries.term("field", "100").toQuery(schema), 1);
                assertThat(result.getTotalCount(), is(1));
                index.delete();
            }
        });
        database.close();
        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        Thread.sleep(2000);

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        assertThat(new File(tempDir, "test-" + id[0]).exists(), is(false));
    }

    @Test
    public void testTimerOperations() throws Throwable {
        database.close();
        builder.setTimerPeriod(10);
        builder.addExtension(new IndexDatabaseExtensionConfiguration(600000, new FullTextIndexConfiguration(1000,
                500, 60000, 16000000)));
        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema = Documents.doc().numericField("field").stored().type(DataType.INT).end().toSchema();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < 1000; i++)
                    index.add(schema.createDocument(Arrays.asList(i)));
            }
        });

        Thread.sleep(500);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                assertThat(index.search(Queries.term("field", "100").toQuery(schema), 1).getTotalCount(), is(1));

                for (int i = 0; i < 1000; i++)
                    index.remove("field", Integer.toString(i));
            }
        });

        Thread.sleep(1000);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                assertThat(index.search(Queries.term("field", "100").toQuery(schema), 1).getTotalCount(), is(0));
            }
        });
    }

    @Test
    public void testDocumentSpace() throws Throwable {
        database.close();
        builder.setTimerPeriod(10);
        builder.addExtension(new IndexDatabaseExtensionConfiguration(600000, new FullTextIndexConfiguration(1000,
                500, 60000, 16000000)));
        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        final int id[] = new int[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.createIndex("test", new FullTextIndexSchemaConfiguration("test", 0));
                id[0] = index.getId();
            }
        });

        final IDocumentSchema schema = Documents.doc().numericField("id").indexed().stored().type(DataType.INT).end().toSchema();
        final TestFullTextDocumentSpace documentSpace = new TestFullTextDocumentSpace(id[0], schema);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                ((IFullTextIndexControl) index).setDocumentSpace(documentSpace);

                for (int i = 0; i < 1000; i++)
                    index.add(schema.createDocument(Arrays.asList(i)));

                assertThat(index.search(Queries.term("id", "100").toQuery(schema), 1).getTotalCount(), is(1));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < 1000; i++)
                    index.remove("id", Integer.toString(i));
            }
        });

        database.close();
        database = new DatabaseFactory().createDatabase(parameters, builder.toConfiguration());
        database.open();
        indexManager = ((IIndexDatabaseExtension) database.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME)).getIndexManager();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndexControl index = indexManager.getIndex(id[0]);
                index.setDocumentSpace(documentSpace);
                index.reindex();

                assertThat(((IFullTextIndex) index).search(Queries.term("id", "100").toQuery(schema), 1).getTotalCount(), is(0));
            }
        });
    }

    private class TestFullTextDocumentSpace implements IFullTextDocumentSpace {
        private final int indexId;
        private final IDocumentSchema schema;

        public TestFullTextDocumentSpace(int indexId, IDocumentSchema schema) {
            this.indexId = indexId;
            this.schema = schema;
        }

        @Override
        public void write(IDataSerialization serialization, IDocument document) {
            long id = ((INumericField) document.getFields().get(0)).get().longValue();
            serialization.writeLong(id);
        }

        @Override
        public void readAndReindex(IDataDeserialization deserialization) {
            long id = deserialization.readLong();

            IFullTextIndex index = indexManager.getIndex(indexId);

            index.update("id", Long.toString(id), schema.createDocument(id));
        }
    }
}
