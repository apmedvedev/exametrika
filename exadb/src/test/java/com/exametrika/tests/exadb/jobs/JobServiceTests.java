/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.jobs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobExecution;
import com.exametrika.api.exadb.jobs.IJobService;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tasks.impl.TaskScheduler;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.jobs.JobService;
import com.exametrika.impl.exadb.jobs.JobTask;
import com.exametrika.impl.exadb.jobs.model.JobsModuleSchemaBuilder;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.tests.exadb.jobs.config.schema.JobsSchemaLoaderTests.TestJobOperation;
import com.exametrika.tests.exadb.jobs.config.schema.JobsSchemaLoaderTests.TestJobOperationConfiguration;
import com.exametrika.tests.exadb.jobs.config.schema.JobsSchemaLoaderTests.TestScheduleConfiguration;
import com.exametrika.tests.exadb.jobs.config.schema.JobsSchemaLoaderTests.TestSchedulePeriodConfiguration;


/**
 * The {@link JobServiceTests} are tests for {@link JobService}.
 *
 * @author Medvedev-A
 */
public class JobServiceTests {
    private Database database;
    private DatabaseConfiguration parameters;
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
        parameters = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(null, parameters);
        database.open();

    }

    @After
    public void tearDown() {
        IOs.close(database);
    }

    @Test
    public void testJobs() throws Throwable {
        final JobSchemaBuilder configuration1 = new JobSchemaBuilder()
                .operation(new TestJobOperationConfiguration()).schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.MILLISECOND, Kind.RELATIVE, 100));

        TestJobOperation.count = 0;

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new JobsModuleSchemaBuilder().createModule("test", new Version(1, 0, 0),
                        new MapBuilder().put("job1", configuration1.name("job1").toJob()).toMap()), null);
            }
        });

        Thread.sleep(200);
        assertThat(TestJobOperation.count > 0, is(true));

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new JobsModuleSchemaBuilder().createModule("test", new Version(1, 0, 1),
                        java.util.Collections.<String, JobSchemaConfiguration>emptyMap()), null);
            }
        });
        Thread.sleep(200);
        TestJobOperation.count = 0;
        Thread.sleep(200);
        assertThat(TestJobOperation.count, is(0));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobService.addJob(configuration1.name("job2").toJob());
            }
        });

        Thread.sleep(200);
        assertThat(TestJobOperation.count > 0, is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobService.findJob("job2").delete();
            }
        });

        Thread.sleep(200);
        TestJobOperation.count = 0;
        Thread.sleep(200);
        assertThat(TestJobOperation.count, is(0));
    }

    @Test
    public void testJobService() throws Throwable {
        final JobSchemaBuilder builder1 = new JobSchemaBuilder()
                .name("job1").description("Test description").group("TestGroup").parameters().put("param1", 100l).put("param2", "string value").end()
                .operation(new TestJobOperationConfiguration()).schedule(new TestScheduleConfiguration()).enabled(true)
                .maxExecutionPeriod(10000).restartCount(4).restartPeriod(1000).oneTime();

        final JobSchemaBuilder builder2 = new JobSchemaBuilder()
                .name("job2").operation(new TestJobOperationConfiguration()).schedule(new TestScheduleConfiguration()).recurrent().repeatCount(100)
                .period(new TestSchedulePeriodConfiguration());

        final JobSchemaBuilder builder3 = new JobSchemaBuilder()
                .operation(new TestJobOperationConfiguration()).schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm")
                        .parse("and(month(2,4,6),dayOfMonth(10),dayOfWeek(4,5),or(time(5:10..8:30), time(20:00..23:40)),date(15.01.2010))"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).restartCount(1);

        final JobSchemaConfiguration configuration1 = builder1.name("job1").toJob();
        final JobSchemaConfiguration configuration2 = builder2.name("job2").toJob();
        final JobSchemaConfiguration configuration3 = builder3.name("job3").toJob();
        final JobSchemaConfiguration configuration4 = builder3.name("job4").toJob();
        final JobSchemaConfiguration configuration5 = builder3.name("job5").toJob();
        final JobSchemaConfiguration configuration13 = builder3.name("job1").toJob();
        final JobSchemaConfiguration configuration31 = builder1.name("job3").toJob();
        final JobSchemaConfiguration configuration32 = builder2.name("job3").description("job32").toJob();

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new JobsModuleSchemaBuilder().createModule("test", new Version(1, 0, 0),
                        new MapBuilder().put("job3", configuration3).put("job4", configuration4).toMap()), null);
            }
        });

        final JobService[] jobServices = new JobService[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobServices[0] = jobService;
            }
        });

        final TaskScheduler<JobTask> scheduler = Tests.get(jobServices[0].getJobManager(), "scheduler");
        Timer timer = Tests.get(scheduler, "timer");
        timer.stop();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.addJob(configuration1);
                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.getJobSchema(), is(configuration1));
                assertThat(job1.isPredefined(), is(false));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getExecutionCount(), is(0l));
                assertThat(job1.getExecutions().iterator().hasNext(), is(false));

                IJob job2 = jobService.addJob(configuration2);
                IJob job3 = jobService.findJob("job3");
                assertThat(job3.getJobSchema().getName(), is("job3"));
                assertThat(job3.getJobSchema(), is(configuration3));
                assertThat(job3.isPredefined(), is(true));
                assertThat(job3.isActive(), is(false));
                assertThat(job3.getExecutionCount(), is(0l));
                assertThat(job3.getExecutions().iterator().hasNext(), is(false));

                IJob job4 = jobService.findJob("job4");
                assertThat(job4 != null, is(true));

                List list = Collections.toList(jobService.getJobs().iterator());
                assertThat(list.size(), is(4));
                assertThat(list.contains(job1), is(true));
                assertThat(list.contains(job2), is(true));
                assertThat(list.contains(job3), is(true));
                assertThat(list.contains(job4), is(true));

                assertThat(jobService.findJob("job1") == job1, is(true));
                assertThat(jobService.findJob("job2") == job2, is(true));
                assertThat(jobService.findJob("job3") == job3, is(true));
                assertThat(jobService.findJob("job4") == job4, is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");
                IJob job2 = jobService.findJob("job2");
                IJob job3 = jobService.findJob("job3");
                final IJob job4 = jobService.findJob("job4");
                IJob job5 = jobService.addJob(configuration5);

                job2.delete();

                try {
                    new Expected(UnsupportedOperationException.class, new Runnable() {
                        @Override
                        public void run() {
                            job4.delete();
                        }
                    });
                } catch (Throwable e) {
                    new RuntimeException(e);
                }

                jobService.addJob(configuration13);
                jobService.addJob(configuration31);

                List list = Collections.toList(jobService.getJobs().iterator());
                assertThat(list.size(), is(4));
                assertThat(list.contains(job1), is(true));
                assertThat(list.contains(job3), is(true));
                assertThat(list.contains(job5), is(true));

                assertThat(jobService.findJob("job1") == job1, is(true));
                assertThat(jobService.findJob("job2"), nullValue());
                assertThat(jobService.findJob("job3") == job3, is(true));
                assertThat(jobService.findJob("job5") == job5, is(true));
            }
        });

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new JobsModuleSchemaBuilder().createModule("test", new Version(1, 0, 1),
                        new MapBuilder().put("job3", configuration3).toMap()), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                assertThat(jobService.findJob("job4"), nullValue());
                List list = Collections.toList(jobService.getJobs().iterator());
                assertThat(list.size(), is(3));

                IJob job3 = jobService.findJob("job3");
                assertThat(job3.getJobSchema(), is(configuration3));
            }
        });

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new JobsModuleSchemaBuilder().createModule("test", new Version(1, 0, 2),
                        new MapBuilder().put("job3", configuration32).toMap()), null);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                List list = Collections.toList(jobService.getJobs().iterator());
                assertThat(list.size(), is(3));

                IJob job3 = jobService.findJob("job3");
                assertThat(job3.getJobSchema(), is(configuration32));
            }
        });

        jobServices[0].onJobExecutionStarted("job1", 10);
        jobServices[0].onJobExecutionStarted("job3", 10);
        jobServices[0].onJobExecutionStarted("job5", 10);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");
                assertThat(job1.isActive(), is(true));
                assertThat(job1.getLastStartTime(), is(10l));
                assertThat(job1.getLastEndTime(), is(0l));
                assertThat(job1.getExecutionCount(), is(0l));
            }
        });

        jobServices[0].onJobExecutionSucceeded("job3", 100, "Succeeded");
        jobServices[0].onJobExecutionFailed("job1", 100, "Failed");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job5 = jobService.findJob("job5");
                job5.cancel();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime(), is(10l));
                assertThat(job1.getLastEndTime(), is(100l));
                assertThat(job1.getExecutionCount(), is(0l));
                assertThat(job1.getRestartCount(), is(1));
                List<IJobExecution> list = Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list.size(), is(1));
                assertThat(list.get(0).getStartTime(), is(10l));
                assertThat(list.get(0).getEndTime(), is(100l));
                assertThat(list.get(0).getError(), is("Failed"));
                assertThat(list.get(0).getStatus(), is(IJobExecution.Status.FAILED));

                IJob job3 = jobService.findJob("job3");
                assertThat(job3.isActive(), is(false));
                assertThat(job3.getLastStartTime(), is(10l));
                assertThat(job3.getLastEndTime(), is(100l));
                assertThat(job3.getExecutionCount(), is(1l));
                assertThat(job3.getRestartCount(), is(0));
                list = Collections.<IJobExecution>toList(job3.getExecutions().iterator());
                assertThat(list.size(), is(1));
                assertThat(list.get(0).getStartTime(), is(10l));
                assertThat(list.get(0).getEndTime(), is(100l));
                assertThat(list.get(0).getError(), nullValue());
                assertThat((String) list.get(0).getResult(), is("Succeeded"));
                assertThat(list.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));

                IJob job5 = jobService.findJob("job5");
                assertThat(job5.isActive(), is(false));
                assertThat(job5.getLastStartTime(), is(10l));
                assertThat(job5.getLastEndTime() > 10l, is(true));
                assertThat(job5.getExecutionCount(), is(1l));
                assertThat(job5.getRestartCount(), is(0));
                list = Collections.<IJobExecution>toList(job5.getExecutions().iterator());
                assertThat(list.size(), is(1));
                assertThat(list.get(0).getStartTime(), is(10l));
                assertThat(list.get(0).getEndTime() > 10, is(true));
                assertThat(list.get(0).getError(), nullValue());
                assertThat(list.get(0).getResult(), nullValue());
                assertThat(list.get(0).getStatus(), is(IJobExecution.Status.CANCELED));
            }
        });

        jobServices[0].onJobExecutionStarted("job1", 200);
        jobServices[0].onJobExecutionStarted("job3", 200);
        jobServices[0].onJobExecutionStarted("job5", 200);

        Thread.sleep(100);

        jobServices[0].onJobExecutionSucceeded("job3", 300, "Succeeded");
        jobServices[0].onJobExecutionFailed("job1", 300, "Failed");
        jobServices[0].onJobExecutionFailed("job5", 300, "Failed");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime(), is(200l));
                assertThat(job1.getLastEndTime(), is(300l));
                assertThat(job1.getExecutionCount(), is(1l));
                assertThat(job1.getRestartCount(), is(0));
                List<IJobExecution> list = Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list.size(), is(2));
                assertThat(list.get(0).getStartTime(), is(200l));
                assertThat(list.get(0).getEndTime(), is(300l));
                assertThat(list.get(0).getError(), is("Failed"));
                assertThat(list.get(0).getStatus(), is(IJobExecution.Status.FAILED));
                assertThat(list.get(1).getStartTime(), is(10l));
                assertThat(list.get(1).getEndTime(), is(100l));
                assertThat(list.get(1).getError(), is("Failed"));
                assertThat(list.get(1).getStatus(), is(IJobExecution.Status.FAILED));

                IJob job3 = jobService.findJob("job3");
                assertThat(job3.isActive(), is(false));
                assertThat(job3.getLastStartTime(), is(200l));
                assertThat(job3.getLastEndTime(), is(300l));
                assertThat(job3.getExecutionCount(), is(2l));
                assertThat(job3.getRestartCount(), is(0));
                list = Collections.<IJobExecution>toList(job3.getExecutions().iterator());
                assertThat(list.size(), is(2));
                assertThat(list.get(0).getStartTime(), is(200l));
                assertThat(list.get(0).getEndTime(), is(300l));
                assertThat(list.get(0).getError(), nullValue());
                assertThat((String) list.get(0).getResult(), is("Succeeded"));
                assertThat(list.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));
                assertThat(list.get(1).getStartTime(), is(10l));
                assertThat(list.get(1).getEndTime(), is(100l));
                assertThat(list.get(1).getError(), nullValue());
                assertThat((String) list.get(1).getResult(), is("Succeeded"));
                assertThat(list.get(1).getStatus(), is(IJobExecution.Status.SUCCEEDED));

                IJob job5 = jobService.findJob("job5");
                assertThat(job5.isActive(), is(false));
                assertThat(job5.getLastStartTime(), is(200l));
                assertThat(job5.getLastEndTime(), is(300l));
                assertThat(job5.getExecutionCount(), is(1l));
                assertThat(job5.getRestartCount(), is(1));
                list = Collections.<IJobExecution>toList(job5.getExecutions().iterator());
                assertThat(list.size(), is(2));
                assertThat(list.get(0).getStartTime(), is(200l));
                assertThat(list.get(0).getEndTime(), is(300l));
                assertThat(list.get(0).getError(), is("Failed"));
                assertThat(list.get(0).getResult(), nullValue());
                assertThat(list.get(0).getStatus(), is(IJobExecution.Status.FAILED));
                assertThat(list.get(1).getStartTime(), is(10l));
                assertThat(list.get(1).getEndTime() > 10, is(true));
                assertThat(list.get(1).getError(), nullValue());
                assertThat(list.get(1).getResult(), nullValue());
                assertThat(list.get(1).getStatus(), is(IJobExecution.Status.CANCELED));

                job3.clearExecutions(1);
                list = Collections.<IJobExecution>toList(job3.getExecutions().iterator());
                assertThat(list.size(), is(1));
                assertThat(list.get(0).getStartTime(), is(200l));
            }
        });

        jobServices[0].onJobExecutionStarted("job5", 400);

        Thread.sleep(100);

        jobServices[0].onJobExecutionSucceeded("job5", 500, "Succeeded");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job5 = jobService.findJob("job5");
                assertThat(job5.isActive(), is(false));
                assertThat(job5.getLastStartTime(), is(400l));
                assertThat(job5.getLastEndTime(), is(500l));
                assertThat(job5.getExecutionCount(), is(2l));
                assertThat(job5.getRestartCount(), is(0));
                List<IJobExecution> list2 = Collections.<IJobExecution>toList(job5.getExecutions().iterator());
                assertThat(list2.size(), is(3));
                assertThat(list2.get(0).getStartTime(), is(400l));
                assertThat(list2.get(0).getEndTime(), is(500l));
                assertThat(list2.get(0).getError(), nullValue());
                assertThat((String) list2.get(0).getResult(), is("Succeeded"));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));
                assertThat(list2.get(1).getStartTime(), is(200l));
                assertThat(list2.get(1).getEndTime(), is(300l));
                assertThat(list2.get(1).getError(), is("Failed"));
                assertThat(list2.get(1).getResult(), nullValue());
                assertThat(list2.get(1).getStatus(), is(IJobExecution.Status.FAILED));
                assertThat(list2.get(2).getStartTime(), is(10l));
                assertThat(list2.get(2).getEndTime() > 10, is(true));
                assertThat(list2.get(2).getError(), nullValue());
                assertThat(list2.get(2).getResult(), nullValue());
                assertThat(list2.get(2).getStatus(), is(IJobExecution.Status.CANCELED));
            }
        });

        JobTask task1 = scheduler.findTask("job1");
        assertThat(task1.getSchema(), is(configuration13));
        assertThat(task1.isPredefined(), is(false));

        assertThat(scheduler.findTask("job2"), nullValue());

        JobTask task3 = scheduler.findTask("job3");
        assertThat(task3.getSchema(), is(configuration32));
        assertThat(task3.isPredefined(), is(true));

        assertThat(scheduler.findTask("job4"), nullValue());

        JobTask task5 = scheduler.findTask("job5");
        assertThat(task5.getSchema(), is(configuration5));
        assertThat(task5.isPredefined(), is(false));

        database.close();

        database = new DatabaseFactory().createDatabase(null, parameters);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobServices[0] = jobService;
                for (@SuppressWarnings("unused") IJob job : jobService.getJobs())
                    ;

                IJob job1 = jobService.findJob("job1");
                IJob job3 = jobService.findJob("job3");
                IJob job5 = jobService.findJob("job5");
                List list = Collections.toList(jobService.getJobs().iterator());
                assertThat(list.size(), is(3));
                assertThat(list.contains(job1), is(true));
                assertThat(list.contains(job3), is(true));
                assertThat(list.contains(job5), is(true));

                assertThat(jobService.findJob("job1") == job1, is(true));
                assertThat(jobService.findJob("job3") == job3, is(true));
                assertThat(jobService.findJob("job5") == job5, is(true));

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.getJobSchema(), is(configuration13));
                assertThat(job1.isPredefined(), is(false));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime(), is(200l));
                assertThat(job1.getLastEndTime(), is(300l));
                assertThat(job1.getExecutionCount(), is(1l));
                assertThat(job1.getRestartCount(), is(0));
                List<IJobExecution> list2 = Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(2));
                assertThat(list2.get(0).getStartTime(), is(200l));
                assertThat(list2.get(0).getEndTime(), is(300l));
                assertThat(list2.get(0).getError(), is("Failed"));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.FAILED));
                assertThat(list2.get(1).getStartTime(), is(10l));
                assertThat(list2.get(1).getEndTime(), is(100l));
                assertThat(list2.get(1).getError(), is("Failed"));
                assertThat(list2.get(1).getStatus(), is(IJobExecution.Status.FAILED));

                assertThat(job3.getJobSchema(), is(configuration32));
                assertThat(job3.isPredefined(), is(true));
                assertThat(job3.isActive(), is(false));
                assertThat(job3.getLastStartTime(), is(200l));
                assertThat(job3.getLastEndTime(), is(300l));
                assertThat(job3.getExecutionCount(), is(2l));
                assertThat(job3.getRestartCount(), is(0));
                list2 = Collections.<IJobExecution>toList(job3.getExecutions().iterator());
                assertThat(list2.size(), is(1));
                assertThat(list2.get(0).getStartTime(), is(200l));
                assertThat(list2.get(0).getEndTime(), is(300l));
                assertThat(list2.get(0).getError(), nullValue());
                assertThat((String) list2.get(0).getResult(), is("Succeeded"));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));

                assertThat(job5.getJobSchema(), is(configuration5));
                assertThat(job5.isPredefined(), is(false));
                assertThat(job5.isActive(), is(false));
                assertThat(job5.getLastStartTime(), is(400l));
                assertThat(job5.getLastEndTime(), is(500l));
                assertThat(job5.getExecutionCount(), is(2l));
                assertThat(job5.getRestartCount(), is(0));
                list2 = Collections.<IJobExecution>toList(job5.getExecutions().iterator());
                assertThat(list2.size(), is(3));
                assertThat(list2.get(0).getStartTime(), is(400l));
                assertThat(list2.get(0).getEndTime(), is(500l));
                assertThat(list2.get(0).getError(), nullValue());
                assertThat((String) list2.get(0).getResult(), is("Succeeded"));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));
                assertThat(list2.get(1).getStartTime(), is(200l));
                assertThat(list2.get(1).getEndTime(), is(300l));
                assertThat(list2.get(1).getError(), is("Failed"));
                assertThat(list2.get(1).getResult(), nullValue());
                assertThat(list2.get(1).getStatus(), is(IJobExecution.Status.FAILED));
                assertThat(list2.get(2).getStartTime(), is(10l));
                assertThat(list2.get(2).getEndTime() > 10, is(true));
                assertThat(list2.get(2).getError(), nullValue());
                assertThat(list2.get(2).getResult(), nullValue());
                assertThat(list2.get(2).getStatus(), is(IJobExecution.Status.CANCELED));
            }
        });

        TaskScheduler<JobTask> scheduler2 = Tests.get(jobServices[0].getJobManager(), "scheduler");

        task1 = scheduler2.findTask("job1");
        assertThat(task1.getSchema(), is(configuration13));
        assertThat(task1.isPredefined(), is(false));

        assertThat(scheduler2.findTask("job2"), nullValue());

        task3 = scheduler2.findTask("job3");
        assertThat(task3.getSchema(), is(configuration32));
        assertThat(task3.isPredefined(), is(true));

        assertThat(scheduler2.findTask("job4"), nullValue());

        task5 = scheduler2.findTask("job5");
        assertThat(task5.getSchema(), is(configuration5));
        assertThat(task5.isPredefined(), is(false));
    }

    @Test
    public void testAsyncJob() throws Throwable {
        final boolean[] executed = new boolean[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobService.execute(new Runnable() {
                    @Override
                    public void run() {
                        executed[0] = true;
                    }
                });
            }
        });

        Thread.sleep(500);

        assertThat(executed[0], is(true));
    }
}
