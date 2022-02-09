/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link DomainServiceTests} are tests for domain services.
 *
 * @author Medvedev-A
 */
public class DomainServiceTests {
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
        builder.setTimerPeriod(100);
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
    public void testServices() throws Throwable {
        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test",
                com.exametrika.common.utils.Collections.<SpaceSchemaConfiguration>asSet(),
                com.exametrika.common.utils.Collections.asSet(new TestDomainServiceSchemaConfiguration("test1", "value1"),
                        new TestDomainServiceSchemaConfiguration("test2", "value2"), new TestDomainServiceSchemaConfiguration("test3", "value3")));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });

        Thread.sleep(200);

        final TestDomainService services[] = new TestDomainService[5];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IDomainServiceSchema schema1 = transaction.getCurrentSchema().findDomain("test").findDomainService("test1");
                services[0] = schema1.getService();
                assertThat(services[0].started, is(true));
                assertThat(services[0].stopped, is(false));
                assertThat(services[0].timered, is(true));
                assertThat(services[0].cleared, is(true));

                IDomainServiceSchema schema2 = transaction.getCurrentSchema().findDomain("test").findDomainService("test2");
                services[1] = schema2.getService();
                assertThat(services[1].started, is(true));
                assertThat(services[1].timered, is(true));

                IDomainServiceSchema schema3 = transaction.getCurrentSchema().findDomain("test").findDomainService("test3");
                services[2] = schema3.getService();
                assertThat(services[2].started, is(true));
                assertThat(services[2].timered, is(true));
            }
        });

        final DomainSchemaConfiguration configuration12 = new DomainSchemaConfiguration("test",
                com.exametrika.common.utils.Collections.<SpaceSchemaConfiguration>asSet(),
                com.exametrika.common.utils.Collections.asSet(new TestDomainServiceSchemaConfiguration("test2", "value22"),
                        new TestDomainServiceSchemaConfiguration("test3", "value3"), new TestDomainServiceSchemaConfiguration("test4", "value4")));
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 1), configuration12), null);
            }
        });

        Thread.sleep(200);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IDomainServiceSchema schema1 = transaction.getCurrentSchema().findDomain("test").findDomainService("test1");
                assertThat(schema1, nullValue());
                assertThat(services[0].stopped, is(true));

                IDomainServiceSchema schema2 = transaction.getCurrentSchema().findDomain("test").findDomainService("test2");
                assertThat(((TestDomainServiceSchemaConfiguration) schema2.getConfiguration()).value, is("value22"));
                assertThat(services[1].stopped, is(false));
                assertThat(schema2.getService() == services[1], is(true));
                services[1] = schema2.getService();
                assertThat(services[1].started, is(true));
                assertThat(services[1].stopped, is(false));
                assertThat(services[1].timered, is(true));
                assertThat(transaction.getSchemas().get(0).findDomain("test").findDomainService("test2").getService(), nullValue());

                IDomainServiceSchema schema3 = transaction.getCurrentSchema().findDomain("test").findDomainService("test3");
                assertThat(services[2] == schema3.getService(), is(true));
                assertThat(services[2].getSchema() == schema3, is(true));
                assertThat(services[2].started, is(true));
                assertThat(services[2].stopped, is(false));
                assertThat(services[2].timered, is(true));

                IDomainServiceSchema schema4 = transaction.getCurrentSchema().findDomain("test").findDomainService("test4");
                assertThat(((TestDomainServiceSchemaConfiguration) schema4.getConfiguration()).getName(), is("test4"));
                assertThat(((TestDomainServiceSchemaConfiguration) schema4.getConfiguration()).value, is("value4"));
                services[3] = schema4.getService();
                assertThat(services[3].started, is(true));
            }
        });

        database.close();

        assertThat(services[1].stopped, is(true));
        assertThat(services[2].stopped, is(true));
        assertThat(services[3].stopped, is(true));

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IDomainServiceSchema schema2 = transaction.getCurrentSchema().findDomain("test").findDomainService("test2");
                services[1] = schema2.getService();
                assertThat(services[1].started, is(true));
                assertThat(services[1].stopped, is(false));

                IDomainServiceSchema schema3 = transaction.getCurrentSchema().findDomain("test").findDomainService("test3");
                services[2] = schema3.getService();
                assertThat(services[2].started, is(true));
                assertThat(services[2].stopped, is(false));

                IDomainServiceSchema schema4 = transaction.getCurrentSchema().findDomain("test").findDomainService("test4");
                services[3] = schema4.getService();
                assertThat(services[3].started, is(true));
                assertThat(services[3].stopped, is(false));

                assertThat(transaction.getSchemas().get(0).findDomain("test").findDomainService("test1").getService(), nullValue());
                assertThat(transaction.getSchemas().get(0).findDomain("test").findDomainService("test2").getService(), nullValue());
                assertThat(transaction.getSchemas().get(0).findDomain("test").findDomainService("test3").getService(), nullValue());
            }
        });

        database.clearCaches();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IDomainServiceSchema schema2 = transaction.getCurrentSchema().findDomain("test").findDomainService("test2");
                assertThat(services[1] == schema2.getService(), is(true));
                assertThat(services[1].cleared, is(true));
                services[1].cleared = false;

                IDomainServiceSchema schema3 = transaction.getCurrentSchema().findDomain("test").findDomainService("test3");
                assertThat(services[2] == schema3.getService(), is(true));
                assertThat(services[2].cleared, is(true));
                services[2].cleared = false;

                IDomainServiceSchema schema4 = transaction.getCurrentSchema().findDomain("test").findDomainService("test4");
                assertThat(services[3] == schema4.getService(), is(true));
                assertThat(services[3].cleared, is(true));
                services[3].cleared = false;
            }
        });

        builder.addDomainService(new TestDomainServiceConfiguration("test.test2", "value2"));
        builder.addDomainService(new TestDomainServiceConfiguration("test.test3", "value3"));
        database.setConfiguration(builder.toConfiguration());

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IDomainServiceSchema schema2 = transaction.getCurrentSchema().findDomain("test").findDomainService("test2");
                assertThat(services[1] == schema2.getService(), is(true));
                assertThat(services[1].getConfiguration(), is(new TestDomainServiceConfiguration("test.test2", "value2")));
                assertThat(services[1].cleared, is(false));

                IDomainServiceSchema schema3 = transaction.getCurrentSchema().findDomain("test").findDomainService("test3");
                assertThat(services[2] == schema3.getService(), is(true));
                assertThat(services[2].getConfiguration(), is(new TestDomainServiceConfiguration("test.test3", "value3")));
                assertThat(services[2].cleared, is(false));

                IDomainServiceSchema schema4 = transaction.getCurrentSchema().findDomain("test").findDomainService("test4");
                assertThat(services[3] == schema4.getService(), is(true));
                assertThat(services[3].getConfiguration(), is(new TestDomainServiceConfiguration("test.test4", "default")));
                assertThat(services[3].cleared, is(false));
            }
        });
    }

    public static class TestDomainServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
        private final String value;

        public TestDomainServiceSchemaConfiguration(String name, String value) {
            super(name);

            this.value = value;
        }

        @Override
        public IDomainService createService() {
            return new TestDomainService();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDomainServiceSchemaConfiguration))
                return false;

            TestDomainServiceSchemaConfiguration configuration = (TestDomainServiceSchemaConfiguration) o;
            return super.equals(configuration) && value.equals(configuration.value);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + value.hashCode();
        }
    }

    private static class TestDomainServiceConfiguration extends DomainServiceConfiguration {
        private final String value;

        TestDomainServiceConfiguration(String name, String value) {
            super(name);

            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDomainServiceConfiguration))
                return false;

            TestDomainServiceConfiguration configuration = (TestDomainServiceConfiguration) o;
            return super.equals(configuration) && value.equals(configuration.value);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + value.hashCode();
        }
    }

    private static class TestDomainService implements IDomainService {
        private IDomainServiceSchema schema;
        private TestDomainServiceConfiguration configuration;
        private boolean started;
        private boolean stopped;
        private boolean timered;
        private boolean cleared;

        public TestDomainService() {
        }

        @Override
        public IDomainServiceSchema getSchema() {
            return schema;
        }

        @Override
        public void setSchema(IDomainServiceSchema schema) {
            Assert.notNull(schema);

            this.schema = schema;
            this.configuration = new TestDomainServiceConfiguration(
                    schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName(), "default");
        }

        @Override
        public TestDomainServiceConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public void setConfiguration(DomainServiceConfiguration configuration, boolean clearCache) {
            if (configuration != null)
                this.configuration = (TestDomainServiceConfiguration) configuration;
            else
                this.configuration = new TestDomainServiceConfiguration(
                        schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName(), "default");
        }

        @Override
        public void start(IDatabaseContext context) {
            started = true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void onTimer(long currentTime) {
            timered = true;
        }

        @Override
        public void clearCaches() {
            cleared = true;
        }

        @Override
        public void onCreated() {
        }

        @Override
        public void onOpened() {
        }

        @Override
        public void onDeleted() {
        }
    }
}
