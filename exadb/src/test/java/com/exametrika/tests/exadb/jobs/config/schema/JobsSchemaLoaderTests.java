/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.jobs.config.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.exametrika.api.exadb.core.config.schema.BackupOperationSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleDependencySchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.NullArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.api.exadb.jobs.config.schema.JobServiceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.CompactionOperationSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader;
import com.exametrika.impl.exadb.jobs.config.schema.JobsSchemaLoader;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.IInterruptible;
import com.exametrika.spi.exadb.jobs.IJobContext;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;

/**
 * The {@link JobsSchemaLoaderTests} are tests for {@link JobsSchemaLoader}.
 *
 * @author Medvedev-A
 * @see JobsSchemaLoader
 */
public class JobsSchemaLoaderTests {
    public static class TestJobsConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("agent.jobs", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/jobs-extension.dbschema", false));
            TestJobsProcessor processor = new TestJobsProcessor();
            parameters.typeLoaders.put("TestSchedule", processor);
            parameters.typeLoaders.put("TestSchedulePeriod", processor);
            parameters.typeLoaders.put("TestJobOperation", processor);
            return parameters;
        }
    }

    public static class TestScheduleConfiguration extends ScheduleSchemaConfiguration {
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestScheduleConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public ISchedule createSchedule() {
            return null;
        }

        @Override
        public String toString(Locale locale) {
            return null;
        }
    }

    public static class TestSchedulePeriodConfiguration extends SchedulePeriodSchemaConfiguration {
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestSchedulePeriodConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public ISchedulePeriod createPeriod() {
            return null;
        }
    }

    public static class TestJobOperationConfiguration extends JobOperationSchemaConfiguration {
        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        public Runnable createOperation(IJobContext context) {
            return new TestJobOperation();
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
        public static int count;

        @Override
        public void interrupt() {
        }

        @Override
        public void run() {
            count++;
        }
    }

    @Test
    public void testJobsConfigurationLoad() {
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ModuleSchemaLoader loader = new ModuleSchemaLoader();
        Set<ModuleSchemaConfiguration> modules = loader.loadModules("classpath:" + getResourcePath() + "/config2.conf");

        Map<String, JobSchemaConfiguration> predefinedJobs1 = new HashMap<String, JobSchemaConfiguration>();
        predefinedJobs1.put("job1", new JobSchemaBuilder()
                .name("job1").description("Test description").group("TestGroup").parameters().put("param1", 100l).put("param2", "string value").end()
                .operation(new TestJobOperationConfiguration()).schedule(new TestScheduleConfiguration()).enabled(true)
                .maxExecutionPeriod(10000).restartCount(4).restartPeriod(1000).oneTime().toJob());

        predefinedJobs1.put("backup", new JobSchemaBuilder()
                .name("backup")
                .operation(new BackupOperationSchemaConfiguration(new NullArchiveStoreSchemaConfiguration())).schedule(
                        new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(20:00..23:40)"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).toJob());
        predefinedJobs1.put("compact", new JobSchemaBuilder()
                .name("compact")
                .operation(new CompactionOperationSchemaConfiguration()).schedule(
                        new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(20:00..23:40)"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).toJob());

        DatabaseSchemaConfiguration schema1 = new DatabaseSchemaConfiguration("module1", "module1", null, Collections.asSet(
                new DomainSchemaConfiguration("system", "system", null, Collections.<SpaceSchemaConfiguration>asSet(),
                        Collections.<DomainServiceSchemaConfiguration>asSet(new JobServiceSchemaConfiguration("JobService", null, predefinedJobs1)))),
                java.util.Collections.<DatabaseExtensionSchemaConfiguration>emptySet());
        ModuleSchemaConfiguration module1 = new ModuleSchemaConfiguration("module1", "module1", null, Version.parse("1"),
                schema1, Collections.<ModuleDependencySchemaConfiguration>asSet());

        Map<String, JobSchemaConfiguration> predefinedJobs2 = new HashMap<String, JobSchemaConfiguration>();
        predefinedJobs2.put("job2", new JobSchemaBuilder()
                .name("job2")
                .operation(new TestJobOperationConfiguration()).schedule(new TestScheduleConfiguration()).recurrent().repeatCount(100)
                .period(new TestSchedulePeriodConfiguration()).toJob());
        predefinedJobs2.put("job3", new JobSchemaBuilder()
                .name("job3")
                .operation(new TestJobOperationConfiguration()).schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm")
                        .parse("and(month(2,4,6),dayOfMonth(10),dayOfWeek(4,5),or(time(5:10..8:30), time(20:00..23:40)),date(15.01.2010))"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1)).toJob());

        DatabaseSchemaConfiguration schema2 = new DatabaseSchemaConfiguration("module2", "module2", null, Collections.asSet(
                new DomainSchemaConfiguration("system", "system", null, Collections.<SpaceSchemaConfiguration>asSet(),
                        Collections.<DomainServiceSchemaConfiguration>asSet(new JobServiceSchemaConfiguration("JobService", null, predefinedJobs2)))),
                java.util.Collections.<DatabaseExtensionSchemaConfiguration>emptySet());
        ModuleSchemaConfiguration module2 = new ModuleSchemaConfiguration("module2", "module2", null, Version.parse("1"),
                schema2, Collections.<ModuleDependencySchemaConfiguration>asSet());

        assertThat(modules, is(Collections.asSet(module1, module2)));

        ModularDatabaseSchemaConfiguration modular = new ModularDatabaseSchemaConfiguration("test", modules);

        Map<String, JobSchemaConfiguration> predefinedJobs = new HashMap<String, JobSchemaConfiguration>();
        predefinedJobs.putAll(predefinedJobs1);
        predefinedJobs.putAll(predefinedJobs2);

        DatabaseSchemaConfiguration schema = new DatabaseSchemaConfiguration("test", "test", null, Collections.asSet(
                new DomainSchemaConfiguration("system", "system", null, Collections.<SpaceSchemaConfiguration>asSet(),
                        Collections.<DomainServiceSchemaConfiguration>asSet(new JobServiceSchemaConfiguration("JobService", null, predefinedJobs)))),
                java.util.Collections.<DatabaseExtensionSchemaConfiguration>emptySet());

        assertThat(modular.getCombinedSchema(), is(schema));
    }

    private static String getResourcePath() {
        String className = JobsSchemaLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }

    private static class TestJobsProcessor implements IExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context) {
            JsonObject element = (JsonObject) object;
            if (element.contains("instanceOf"))
                type = element.get("instanceOf");

            if (type.equals("TestSchedule"))
                return new TestScheduleConfiguration();
            else if (type.equals("TestSchedulePeriod"))
                return new TestSchedulePeriodConfiguration();
            else if (type.equals("TestJobOperation"))
                return new TestJobOperationConfiguration();
            else
                throw new InvalidConfigurationException();
        }

        @Override
        public void setExtensionLoader(IExtensionLoader extensionLoader) {
        }
    }
}
