/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawDatabaseFactory;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.RawOperation;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawDatabaseConfigurationBuilder;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawDatabaseFactory;
import com.exametrika.common.rawdb.impl.RawTransactionLog;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.StaticInterceptorAllocator;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.tests.instrument.instrumentors.TestClassLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * The {@link RawDatabaseLogTests} are tests for {@link RawDatabase} components.
 *
 * @author Medvedev-A
 * @see RawDatabase
 */
public class RawDatabaseLogTests {
    private RawDatabase database;
    private RawDatabaseConfiguration configuration;
    private RawDatabaseConfigurationBuilder builder;

    @Before
    public void setUp() throws Throwable {
        File tempDir1 = new File(System.getProperty("java.io.tmpdir"), "db/p1");
        File tempDir2 = new File(System.getProperty("java.io.tmpdir"), "db/p2");
        Files.emptyDir(tempDir1);
        Files.emptyDir(tempDir2);

        builder = new RawDatabaseConfigurationBuilder().addPath(tempDir1.getPath()).addPath(tempDir1.getPath()).setFlushPeriod(1000)
                .addPageType("normal", 0x800)
                .getDefaultPageCategory().setMaxPageIdlePeriod(10000).setMinPageCachePercentage(90).end()
                .end()
                .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                        new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 204800).toConfiguration())
                        .toConfiguration());
        configuration = builder.toConfiguration();
        database = new RawDatabaseFactory().createDatabase(configuration);
        database.start();
        database.flush();

        Timer timer = database.getPageManager().getTimer();
        timer.suspend();
        timer = Tests.get(database.getCompartment().getGroup(), "timer");
        timer.suspend();
        Thread.sleep(200);
    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testTransactionLog() throws Throwable {
        IOs.close(database);
        int pos1 = 139;// long pos = transactionLogFile.getFilePointer();
        int pos2 = 158;// transactionLogFile.writeInt(flushedPages.size());
        int pos3 = 188;// transactionLogFile.writeLong(startPos);
        int pos4 = 208;// transactionLogFile.writeUTF(file.getName());
        int pos5 = 217;// перед transactionLogFile.getFD().sync();
        int pos6 = 254;// transactionLogFile.setLength(0);
        int pos7 = 239;// transactionLogFile.writeBoolean(true);
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(RawTransactionLog.class.getName()),
                new MemberFilter("flush"));
        LinePointcut pointcut1 = new LinePointcut("test1", filter, new StaticInterceptorConfiguration(TransactionLogInterceptor.class),
                pos1, pos2, false);
        LinePointcut pointcut2 = new LinePointcut("test2", filter, new StaticInterceptorConfiguration(TransactionLogInterceptor.class), pos3, pos4, false);

        LinePointcut pointcut3 = new LinePointcut("test4", filter, new StaticInterceptorConfiguration(TransactionLogInterceptor.class), pos5, pos6, false);

        StaticInterceptorAllocator interceptorAllocator = new StaticInterceptorAllocator();
        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorAllocator, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut1, pointcut2, pointcut3),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(RawTransactionLog.class.getPackage().getName(), classTransformer);
        IRawDatabaseFactory factory = (IRawDatabaseFactory) classLoader.loadClass(RawDatabaseFactory.class.getName()).newInstance();

        File tempDir1 = new File(System.getProperty("java.io.tmpdir"), "db/p1");
        Files.emptyDir(tempDir1);

        builder = new RawDatabaseConfigurationBuilder().addPath(tempDir1.getPath()).setFlushPeriod(Long.MAX_VALUE)
                .addPageType("normal", 0x2000)
                .getDefaultPageCategory().setMaxPageIdlePeriod(10000).setMinPageCachePercentage(90).end()
                .end()
                .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setDefaultPolicy(
                        new FixedAllocationPolicyConfigurationBuilder().addQuota("<default>", 2000000).toConfiguration())
                        .toConfiguration());

        IRawDatabase db0 = factory.createDatabase(builder.toConfiguration());
        db0.start();
        db0.transactionSync(new RawOperation(IRawOperation.FLUSH) {
            @Override
            public void run(IRawTransaction transaction) {
                for (int i = 0; i < 10; i++) {
                    for (int k = 0; k < 10; k++) {
                        IRawPage page = transaction.getPage(i, k);
                        page.getWriteRegion().writeByteArray(0, createBuffer(i * k, 0x2000));
                    }
                }
            }
        });
        db0.stop();
        Map<String, Long> map = getFiles(db0.getConfiguration().getPaths().get(0));
        final List<IJoinPoint> joinPoints = interceptorAllocator.getJoinPoints();

        for (int i = 0; i < joinPoints.size(); i++) {
            final boolean committed = joinPoints.get(i).getSourceLineNumber() > pos7;
            TransactionLogInterceptor.failureIndex = (i == 0 ? i : -1);
            final IRawDatabase db = factory.createDatabase(builder.toConfiguration());
            db.start();
            try {
                final int a = i;
                db.transactionSync(new RawOperation(IRawOperation.FLUSH) {
                    @Override
                    public void run(IRawTransaction transaction) {
                        System.out.println("Failing at line: " + joinPoints.get(a).getSourceLineNumber());

                        for (int i = committed ? 2 : 0; i < 20; i++) {
                            for (int k = 0; k < 20; k++) {
                                IRawPage page = transaction.getPage(i, k);
                                page.getWriteRegion().writeByteArray(0, createBuffer(i * k + 0x1717171, 0x2000));
                            }
                        }

                        transaction.getFile(0).delete();
                        transaction.getFile(1).truncate(1);
                        TransactionLogInterceptor.failureIndex = a;
                    }
                });
            } catch (RawDatabaseException e) {
                Assert.checkState(e.getCause().getClass() == RuntimeException.class);
            }

            TransactionLogInterceptor.failureIndex = -1;
            ((List) Tests.get(Tests.get(db, "pageManager"), "flushedPages")).clear();
            ((List) Tests.get(Tests.get(db, "pageManager"), "flushedFiles")).clear();
            db.stop();
            IRawDatabase db2 = factory.createDatabase(builder.toConfiguration());
            db2.start();
            if (!committed) {
                db2.transactionSync(new RawOperation(true) {
                    @Override
                    public void run(IRawTransaction transaction) {
                        for (int i = 0; i < 10; i++) {
                            for (int k = 0; k < 10; k++) {
                                IRawPage page = transaction.getPage(i, k);
                                Assert.checkState(page.getReadRegion().readByteArray(0, 0x2000).equals(createBuffer(i * k, 0x2000)));
                            }
                        }
                    }
                });

                db2.stop();

                assertThat(getFiles(db2.getConfiguration().getPaths().get(0)), is(map));
            } else {
                db2.transactionSync(new RawOperation(true) {
                    @Override
                    public void run(IRawTransaction transaction) {
                        for (int i = 2; i < 20; i++) {
                            for (int k = 0; k < 20; k++) {
                                IRawPage page = transaction.getPage(i, k);
                                Assert.checkState(page.getReadRegion().readByteArray(0, 0x2000).equals(createBuffer(i * k + 0x1717171, 0x2000)));
                            }
                        }
                    }
                });

                db2.stop();

                assertTrue(!new File(db2.getConfiguration().getPaths().get(0), "db-0.dat").exists());
                assertTrue(new File(db2.getConfiguration().getPaths().get(0), "db-1.dat").length() == 1);
            }
        }
    }

    private Map<String, Long> getFiles(String pathName) {
        File path = new File(pathName);
        Map<String, Long> map = new HashMap<String, Long>();
        for (File file : path.listFiles())
            map.put(file.getPath(), file.length());

        return map;
    }

    private ByteArray createBuffer(int base, int length) {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte) (base + i);

        return new ByteArray(buffer);
    }

    public static class TransactionLogInterceptor {
        private static int failureIndex = -1;

        public static void onLine(int index, int version, Object instance) {
            if (index == failureIndex)
                throw new RuntimeException("test");
        }
    }
}