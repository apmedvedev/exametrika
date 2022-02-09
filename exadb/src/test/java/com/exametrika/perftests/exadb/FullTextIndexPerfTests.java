/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.config.schema.Documents;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.exadb.index.IIndexDatabaseExtension;


/**
 * The {@link FullTextIndexPerfTests} are performance tests for fulltext indexing support.
 *
 * @author Medvedev-A
 */
public class FullTextIndexPerfTests {
    private static final int COUNT = 1000000;
    private Database database;
    private DatabaseConfiguration parameters;
    private DatabaseConfigurationBuilder builder;
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
        parameters = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(null, parameters);
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
                .toSchema();

        long l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < COUNT; i++)
                    index.add(schema.createDocument("value" + i, i));
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("add:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                assertThat(index.search(Queries.expression("field2", "field2:[500000 TO 500099]").toQuery(schema), 1000).getTotalCount(), is(100));
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("search range:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);
                assertThat(index.search(Queries.term("field1", "value500000").toQuery(schema), 1000).getTotalCount(), is(1));
            }
        });

        l = Times.getCurrentTime() - l;
        System.out.println("search term:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < COUNT; i++)
                    index.update("field2", Integer.toString(i), schema.createDocument("p" + (i + 10 * COUNT), i + 10 * COUNT));
            }
        });
        l = Times.getCurrentTime() - l;
        System.out.println("update:" + l);

        l = Times.getCurrentTime();
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IFullTextIndex index = indexManager.getIndex(id[0]);

                for (int i = 0; i < COUNT; i++)
                    index.remove("field2", Integer.toString(i + 10 * COUNT));
            }
        });

        l = Times.getCurrentTime() - l;
        System.out.println("remove:" + l);
    }
}
