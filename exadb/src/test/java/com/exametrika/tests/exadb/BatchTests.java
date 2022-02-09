/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.BatchOperation;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.rawdb.RawBatchLock.Type;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.rawdb.impl.RawDbBatchOperation;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link BatchTests} are tests for batch transactions.
 *
 * @author Medvedev-A
 */
public class BatchTests {
    private Database database;
    private DatabaseConfiguration confifuration;
    private IDatabaseFactory.Parameters parameters;

    @Before
    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(10);
        confifuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put("disableModules", true);

        database = new DatabaseFactory().createDatabase(parameters, confifuration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testBatch() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", "field1", "", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", "field2", "", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), "node1", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new ISchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }

            @Override
            public int getSize() {
                return 1;
            }
        });

        final TestBatchOperation operation = new TestBatchOperation();
        operation.invalid = true;
        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(operation);
            }
        });

        assertThat(operation.rolledBackCount, is(1));

        final TestBatchOperation operation2 = new TestBatchOperation();
        database.transactionSync(operation2);

        assertThat(operation2.committedCount, is(1));
        assertThat(operation2.runCount, is(1000));
        RawDbBatchOperation batchControl = Tests.get(operation2.batchControl, "batchControl");
        assertThat(batchControl.isCompleted(), is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpace space = dataSchema.getSpace();

                IObjectNode node = space.getRootNode();
                IPrimitiveField field = node.getField(0);
                assertThat(field.getInt(), is(1000));
                field.setInt(0);
            }
        });

        final TestBatchOperation operation3 = new TestBatchOperation();
        operation3.exception = new RuntimeException("test");

        database.transactionSync(operation3);

        assertThat(operation3.committedCount, is(1));
        assertThat(operation3.runCount, is(5));
        batchControl = Tests.get(operation3.batchControl, "batchControl");
        assertThat(batchControl.isCompleted(), is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpace space = dataSchema.getSpace();

                IObjectNode node = space.getRootNode();
                IPrimitiveField field = node.getField(0);
                assertThat(field.getInt(), is(5));
                field.setInt(0);

            }
        });
        final TestBatchOperation operation4 = new TestBatchOperation();
        operation4.locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "test"));
        operation4.failure = true;
        try {
            database.transactionSync(operation4);
        } catch (RawDatabaseException e) {
        }

        try {
            database.close();
        } catch (Exception e) {
        }
        database = new DatabaseFactory().createDatabase(parameters, confifuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public List<String> getBatchLockPredicates() {
                return Arrays.asList("test");
            }

            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpace space = dataSchema.getSpace();

                IObjectNode node = space.getRootNode();
                IPrimitiveField field = node.getField(0);
                assertThat(field.getInt(), is(1000));
            }
        });
    }

    @Test
    public void testSequentialBatchExecution() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", "field1", "", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", "field2", "", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), "node1", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new ISchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }

            @Override
            public int getSize() {
                return 1;
            }
        });

        for (int i = 0; i < 10; i++) {
            TestBatchOperation2 operation = new TestBatchOperation2(i);
            operation.locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "test"));
            database.transaction(operation);
        }

        database.transactionSync(new Operation() {
            @Override
            public List<String> getBatchLockPredicates() {
                return Arrays.asList("test");
            }

            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpace space = dataSchema.getSpace();

                IObjectNode node = space.getRootNode();
                IPrimitiveField field = node.getField(0);
                assertThat(field.getInt(), is(10000));
            }
        });
    }

    @Test
    public void testCooperativeTransactionExecution() throws Throwable {
        final TestBatchOperation3 operation = new TestBatchOperation3();
        operation.locks = Arrays.asList(new RawBatchLock(Type.EXCLUSIVE, "a"));
        database.transaction(operation);

        final int[] count = new int[1];
        for (int i = 0; i < 100; i++) {
            final int n = i;
            database.transaction(new Operation() {
                List<String> locks = Arrays.asList("b");

                @Override
                public void run(ITransaction transaction) {
                    if (operation.runCount >= 7 && operation.runCount <= 12)
                        count[0]++;
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        Exceptions.wrapAndThrow(e);
                    }

                    System.out.println(n + " - " + Times.getCurrentTime());
                }

                @Override
                public List<String> getBatchLockPredicates() {
                    return locks;
                }
            });
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
            }
        });

        assertThat(count[0] > 10, is(true));
        assertThat(operation.exits > 0, is(true));
        System.out.println("final - " + Times.getCurrentTime() + ", exits - " + operation.exits);
    }

    @Test
    public void testNonCachedNodes() throws Throwable {
        NodeSchemaConfiguration nodeConfiguration1 = new ObjectNodeSchemaConfiguration("node1", Arrays.asList(
                new PrimitiveFieldSchemaConfiguration("field1", "field1", "", DataType.INT),
                new PrimitiveFieldSchemaConfiguration("field2", "field2", "", DataType.INT)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), "node1", 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new ISchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }

            @Override
            public int getSize() {
                return 1;
            }
        });

        database.clearCaches();

        database.transactionSync(new BatchOperation() {
            @Override
            public boolean run(ITransaction transaction, IBatchControl batchControl) {
                batchControl.setCachingEnabled(false);
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpace space = dataSchema.getSpace();
                ObjectNode node = (ObjectNode) ((ObjectNodeObject) space.getRootNode()).getNode();
                assertThat(node.isReadOnly(), is(true));
                assertThat(node.isCached(), is(false));
                assertThat(node != space.getRootNode(), is(true));

                batchControl.setCachingEnabled(true);
                assertThat(space.getRootNode() == space.getRootNode(), is(true));
                batchControl.setCachingEnabled(false);
                return true;
            }
        });

        database.clearCaches();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                IObjectSpace space = dataSchema.getSpace();
                ObjectNode node = (ObjectNode) ((ObjectNodeObject) space.getRootNode()).getNode();
                assertThat(node.isReadOnly(), is(false));
                assertThat(node.isCached(), is(true));
                assertThat(space.getRootNode() == space.getRootNode(), is(true));
            }
        });
    }

    private static class TestBatchOperation implements IBatchOperation, Serializable {
        private int runCount;
        private boolean invalid;
        private RuntimeException exception;
        private transient boolean failure;
        private int committedCount;
        private int rolledBackCount;
        private transient IRawBatchControl batchControl;
        private List<RawBatchLock> locks = Collections.emptyList();
        private transient boolean failed;

        @Override
        public void validate(ITransaction transaction) {
            if (invalid)
                throw new RawRollbackException(new NonLocalizedMessage("test"));
        }

        @Override
        public boolean run(ITransaction transaction, IBatchControl batchControl) {
            if (failed)
                return false;

            this.batchControl = batchControl;

            if (runCount == 5) {
                if (exception != null)
                    throw exception;
            }

            IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
            IObjectSpace space = dataSchema.getSpace();

            IObjectNode node = space.getRootNode();
            IPrimitiveField field = node.getField(0);
            assertThat(field.getInt(), is(runCount));
            field.setInt(field.getInt() + 1);
            runCount++;

            if (runCount == 6) {
                if (failure) {
                    failed = true;
                    try {
                        RawDbBatchOperation batch = Tests.get(batchControl, "batchControl");
                        System.out.println("Generating NullPointerException...");
                        field.setInt(5);
                        Tests.set(batch, "batchManager", null);
                    } catch (Exception e) {
                        Exceptions.wrapAndThrow(e);
                    }
                }
            }

            return runCount >= 1000;
        }

        @Override
        public void onCommitted() {
            committedCount++;
        }

        @Override
        public void onRolledBack() {
            rolledBackCount++;
        }

        @Override
        public int getOptions() {
            return 0;
        }

        @Override
        public int getSize() {
            return 1;
        }

        @Override
        public List<RawBatchLock> getLocks() {
            return locks;
        }
    }

    private static class TestBatchOperation2 extends BatchOperation implements Serializable {
        private final int n;
        int runCount;
        private List<RawBatchLock> locks = Collections.emptyList();

        public TestBatchOperation2(int n) {
            this.n = n;
        }

        @Override
        public boolean run(ITransaction transaction, IBatchControl batchControl) {
            IObjectSpaceSchema dataSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
            IObjectSpace space = dataSchema.getSpace();

            IObjectNode node = space.getRootNode();
            IPrimitiveField field = node.getField(0);
            assertThat(field.getInt(), is(n * 1000 + runCount));
            field.setInt(field.getInt() + 1);
            runCount++;

            return runCount >= 1000;
        }

        @Override
        public List<RawBatchLock> getLocks() {
            return locks;
        }
    }

    ;

    private static class TestBatchOperation3 extends BatchOperation implements Serializable {
        int runCount;
        int exits;
        private List<RawBatchLock> locks = Collections.emptyList();

        public TestBatchOperation3() {
        }

        @Override
        public boolean run(ITransaction transaction, IBatchControl batchControl) {
            while (batchControl.canContinue()) {
                System.out.println("batch run count - " + runCount + ", time - " + Times.getCurrentTime());
                runCount++;
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Exceptions.wrapAndThrow(e);
                }
            }

            exits++;
            return runCount >= 100;
        }

        @Override
        public List<RawBatchLock> getLocks() {
            return locks;
        }
    }

    ;
}
