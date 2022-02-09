/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.jobs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobExecution;
import com.exametrika.api.exadb.jobs.IJobService;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tasks.ITaskContext;
import com.exametrika.common.tasks.impl.TaskScheduler;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.jobs.JobActivationCondition;
import com.exametrika.impl.exadb.jobs.JobService;
import com.exametrika.impl.exadb.jobs.JobTask;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.spi.exadb.jobs.IAsynchronousJobOperation;
import com.exametrika.spi.exadb.jobs.IInterruptible;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;


/**
 * The {@link JobTaskTests} are tests for {@link JobTask}.
 *
 * @author Medvedev-A
 * @see JobTask
 * @see JobActivationCondition
 */
public class JobTaskTests {
    private Database database;
    private DatabaseConfiguration parameters;
    private DatabaseConfigurationBuilder builder;
    private static boolean run;
    private static Object result;
    private static RuntimeException exception;

    @Before
    public void setUp() {
        run = false;
        result = null;
        exception = null;
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
    public void testJobTask() throws Throwable {
        final JobSchemaBuilder configuration = new JobSchemaBuilder()
                .operation(new TestJobOperationConfiguration()).schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm")
                        .parse("and(month(2,4,6),dayOfMonth(10),dayOfWeek(4,5),or(time(5:10..8:30), time(20:00..23:40)),date(15.01.2010))"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).restartCount(1);

        final JobService[] jobServices = new JobService[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobServices[0] = jobService;
            }
        });

        TaskScheduler<JobTask> scheduler = Tests.get(jobServices[0].getJobManager(), "scheduler");
        Timer timer = Tests.get(scheduler, "timer");
        timer.stop();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                jobServices[0].addJob(configuration.name("job1").toJob());
            }
        });

        JobTask task2 = scheduler.findTask("job1");

        Tests.set(task2, "active", true);
        task2.cancel();

        result = "Succeeded";
        task2.run();
        assertThat(task2.getStartTime() > 0, is(true));
        assertThat(task2.getEndTime() > 0, is(true));
        assertThat(task2.getExecutionCount(), is(1l));
        assertThat(run, is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime() > 0, is(true));
                assertThat(job1.getLastEndTime() > 0, is(true));
                assertThat(job1.getExecutionCount(), is(1l));
                assertThat(job1.getRestartCount(), is(0));
                List<IJobExecution> list2 = com.exametrika.common.utils.Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(1));
                assertThat(list2.get(0).getStartTime() > 0, is(true));
                assertThat(list2.get(0).getEndTime() > 0, is(true));
                assertThat(list2.get(0).getError(), nullValue());
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));
            }

            ;
        });

        run = false;
        result = null;
        exception = new RuntimeException("Failed");
        task2.run();
        assertThat(task2.getStartTime() > 0, is(true));
        assertThat(task2.getEndTime() > 0, is(true));
        assertThat(task2.getExecutionCount(), is(1l));
        assertThat(task2.getRestartCount(), is(1l));
        assertThat(run, is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime() > 0, is(true));
                assertThat(job1.getLastEndTime() > 0, is(true));
                assertThat(job1.getExecutionCount(), is(1l));
                assertThat(job1.getRestartCount(), is(1));
                List<IJobExecution> list2 = com.exametrika.common.utils.Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(2));
                assertThat(list2.get(0).getStartTime() > 0, is(true));
                assertThat(list2.get(0).getEndTime() > 0, is(true));
                assertThat(list2.get(0).getError().contains("Failed"), is(true));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.FAILED));
            }

            ;
        });

        run = false;
        result = null;
        exception = new RuntimeException("test");
        task2.run();
        assertThat(task2.getStartTime() > 0, is(true));
        assertThat(task2.getEndTime() > 0, is(true));
        assertThat(task2.getExecutionCount(), is(2l));
        assertThat(task2.getRestartCount(), is(0l));
        assertThat(run, is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime() > 0, is(true));
                assertThat(job1.getLastEndTime() > 0, is(true));
                assertThat(job1.getExecutionCount(), is(2l));
                assertThat(job1.getRestartCount(), is(0));
                List<IJobExecution> list2 = com.exametrika.common.utils.Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(3));
                assertThat(list2.get(0).getStartTime() > 0, is(true));
                assertThat(list2.get(0).getEndTime() > 0, is(true));
                assertThat(list2.get(0).getError() != null, is(true));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.FAILED));
            }

            ;
        });
    }

    @Test
    public void testAsyncJobTask() throws Throwable {
        final JobSchemaBuilder configuration = new JobSchemaBuilder()
                .operation(new TestAsyncJobOperationConfiguration()).schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm")
                        .parse("and(month(2,4,6),dayOfMonth(10),dayOfWeek(4,5),or(time(5:10..8:30), time(20:00..23:40)),date(15.01.2010))"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).restartCount(1);

        final JobService[] jobServices = new JobService[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobServices[0] = jobService;
            }
        });

        TaskScheduler<JobTask> scheduler = Tests.get(jobServices[0].getJobManager(), "scheduler");
        Timer timer = Tests.get(scheduler, "timer");
        timer.stop();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                jobServices[0].addJob(configuration.name("job1").toJob());
            }
        });

        JobTask task2 = scheduler.findTask("job1");

        task2.run();
        assertThat((Boolean) Tests.get(task2, "active"), is(true));
        assertThat(run, is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(true));
            }

            ;
        });

        task2.onSucceeded("Succeeded");
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime() > 0, is(true));
                assertThat(job1.getLastEndTime() > 0, is(true));
                assertThat(job1.getExecutionCount(), is(1l));
                assertThat(job1.getRestartCount(), is(0));
                List<IJobExecution> list2 = com.exametrika.common.utils.Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(1));
                assertThat(list2.get(0).getStartTime() > 0, is(true));
                assertThat(list2.get(0).getEndTime() > 0, is(true));
                assertThat(list2.get(0).getError(), nullValue());
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.SUCCEEDED));
            }

            ;
        });
        assertThat(task2.getStartTime() > 0, is(true));
        assertThat(task2.getEndTime() > 0, is(true));
        assertThat(task2.getExecutionCount(), is(1l));

        run = false;
        result = null;
        task2.run();
        assertThat((Boolean) Tests.get(task2, "active"), is(true));


        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(true));
            }

            ;
        });

        task2.onFailed(new RuntimeException("Failed"));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime() > 0, is(true));
                assertThat(job1.getLastEndTime() > 0, is(true));
                assertThat(job1.getExecutionCount(), is(1l));
                assertThat(job1.getRestartCount(), is(1));
                List<IJobExecution> list2 = com.exametrika.common.utils.Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(2));
                assertThat(list2.get(0).getStartTime() > 0, is(true));
                assertThat(list2.get(0).getEndTime() > 0, is(true));
                assertThat(list2.get(0).getError().contains("Failed"), is(true));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.FAILED));
            }

            ;
        });

        assertThat(task2.getStartTime() > 0, is(true));
        assertThat(task2.getEndTime() > 0, is(true));
        assertThat(task2.getExecutionCount(), is(1l));
        assertThat(task2.getRestartCount(), is(1l));
        assertThat(run, is(true));

        run = false;
        result = null;
        exception = new RuntimeException("test");
        task2.run();
        assertThat(task2.getStartTime() > 0, is(true));
        assertThat(task2.getEndTime() > 0, is(true));
        assertThat(task2.getExecutionCount(), is(2l));
        assertThat(task2.getRestartCount(), is(0l));
        assertThat(run, is(true));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                IJob job1 = jobService.findJob("job1");

                assertThat(job1.getJobSchema().getName(), is("job1"));
                assertThat(job1.isActive(), is(false));
                assertThat(job1.getLastStartTime() > 0, is(true));
                assertThat(job1.getLastEndTime() > 0, is(true));
                assertThat(job1.getExecutionCount(), is(2l));
                assertThat(job1.getRestartCount(), is(0));
                List<IJobExecution> list2 = com.exametrika.common.utils.Collections.<IJobExecution>toList(job1.getExecutions().iterator());
                assertThat(list2.size(), is(3));
                assertThat(list2.get(0).getStartTime() > 0, is(true));
                assertThat(list2.get(0).getEndTime() > 0, is(true));
                assertThat(list2.get(0).getError() != null, is(true));
                assertThat(list2.get(0).getStatus(), is(IJobExecution.Status.FAILED));
            }

            ;
        });
    }

    @Test
    public void testJobActivationCondition() throws Throwable {
        final JobSchemaBuilder configuration = new JobSchemaBuilder()
                .operation(new TestAsyncJobOperationConfiguration()).schedule(new ScheduleExpressionParser(null, "HH:mm")
                        .parse("time(20:00..23:40)")).group("test")
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).restartCount(1)
                .restartPeriod(10).maxExecutionPeriod(100).repeatCount(1);

        final JobService[] jobServices = new JobService[1];
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JobService jobService = transaction.findDomainService(IJobService.NAME);
                jobServices[0] = jobService;
            }
        });

        TaskScheduler<JobTask> scheduler = Tests.get(jobServices[0].getJobManager(), "scheduler");
        Timer timer = Tests.get(scheduler, "timer");
        timer.stop();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                jobServices[0].addJob(configuration.name("job1").toJob());
            }
        });

        JobTask task = scheduler.findTask("job1");

        TestTaskContext context = new TestTaskContext();

        JobActivationCondition condition = new JobActivationCondition(task);
        assertThat(condition.canActivate(time("20:00", "HH:mm"), context), is(true));
        assertThat(condition.canActivate(time("20:00", "HH:mm"), context), is(false));
        condition.onCompleted(context);
        assertThat(condition.canActivate(time("20:00", "HH:mm"), context), is(true));
        condition.onCompleted(context);

        assertThat(condition.canActivate(time("19:59", "HH:mm"), context), is(false));

        Tests.set(task, "executionCount", 10);
        assertThat(condition.canActivate(time("20:00", "HH:mm"), context), is(false));
        Tests.set(task, "executionCount", 0);

        Tests.set(task, "restartCount", 1);
        Tests.set(task, "endTime", time("20:00", "HH:mm"));
        assertThat(condition.canActivate(time("20:00", "HH:mm"), context), is(false));
        assertThat(condition.canActivate(time("20:00", "HH:mm") + 10, context), is(true));
        condition.onCompleted(context);

        Tests.set(task, "restartCount", 0);
        assertThat(condition.canActivate(time("20:59", "HH:mm"), context), is(false));
        assertThat(condition.canActivate(time("21:00", "HH:mm"), context), is(true));
        condition.onCompleted(context);

        task.run();
        Tests.set(task, "startTime", time("20:00", "HH:mm"));
        condition.tryInterrupt(time("20:00", "HH:mm") + 99);
        assertThat((Boolean) Tests.get(task, "canceled"), is(false));

        condition.tryInterrupt(time("20:00", "HH:mm") + 110);
        assertThat((Boolean) Tests.get(task, "canceled"), is(true));
        TestJobOperation operation = Tests.get(task, "operation");
        assertThat(operation.interrupted, is(true));
    }

    private long time(String value, String pattern) throws Throwable {
        DateFormat format = new SimpleDateFormat(pattern);
        format.getCalendar().clear();
        format.parse(value);
        return format.getCalendar().getTimeInMillis();
    }

    public static class TestJobOperationConfiguration extends JobOperationSchemaConfiguration {
        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        public Runnable createOperation(IJobContext context) {
            return new TestJobOperation(context);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestJobOperationConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public static class TestJobOperation implements Runnable, IInterruptible {
        private final IJobContext context;
        private boolean interrupted;

        public TestJobOperation(IJobContext context) {
            this.context = context;
        }

        @Override
        public void interrupt() {
            interrupted = true;
        }

        @Override
        public void run() {
            run = true;

            if (result != null)
                context.onSucceeded(result);
            else if (exception != null)
                throw exception;
        }
    }

    public static class TestAsyncJobOperationConfiguration extends TestJobOperationConfiguration {
        @Override
        public boolean isAsync() {
            return true;
        }

        @Override
        public Runnable createOperation(IJobContext context) {
            return new TestAsyncJobOperation(context);
        }
    }

    public static class TestAsyncJobOperation extends TestJobOperation implements IAsynchronousJobOperation {
        public TestAsyncJobOperation(IJobContext context) {
            super(context);
        }
    }

    private static class TestTaskContext implements ITaskContext {
        private final Map<String, Object> parameters = new HashMap<String, Object>();

        @Override
        public Map<String, Object> getParameters() {
            return parameters;
        }
    }
}
