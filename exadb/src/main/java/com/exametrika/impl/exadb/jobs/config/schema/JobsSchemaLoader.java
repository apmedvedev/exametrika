/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.config.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.OneTimeJobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.RecurrentJobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.schema.JobServiceSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.config.schema.SchemaLoadContext;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.JobOperationSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link JobsSchemaLoader} is a configuration loader for jobs schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobsSchemaLoader extends AbstractExtensionLoader {
    private static final IMessages messages = Messages.get(IMessages.class);

    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("JobService")) {
            String alias = element.get("alias", JobServiceSchemaConfiguration.NAME);
            String description = element.get("description", null);
            JsonObject predefinedJobsElement = element.get("predefinedJobs", null);
            Map<String, JobSchemaConfiguration> predefinedJobs = new LinkedHashMap<String, JobSchemaConfiguration>();
            if (predefinedJobsElement != null) {
                for (Map.Entry<String, Object> entry : predefinedJobsElement)
                    predefinedJobs.put(entry.getKey(), loadJob(entry.getKey(), (JsonObject) entry.getValue(), context));
            }

            return new JobServiceSchemaConfiguration(alias, description, predefinedJobs, false);
        } else if (type.equals("PredefinedJobs")) {
            SchemaLoadContext loadContext = context.get(ModuleSchemaConfiguration.SCHEMA);
            DatabaseSchemaConfiguration schema = loadContext.getCurrentModule().getSchema();
            DomainSchemaConfiguration domainSchema = null;
            JobServiceSchemaConfiguration jobServiceSchema = null;
            for (Map.Entry<String, Object> entry : element) {
                if (domainSchema == null) {
                    domainSchema = schema.findDomain("system");
                    if (domainSchema == null) {
                        domainSchema = new DomainSchemaConfiguration("system", "system", null, Collections.<SpaceSchemaConfiguration>emptySet(),
                                Collections.<DomainServiceSchemaConfiguration>emptySet(), false);
                        schema.addDomain(domainSchema);
                    }
                }

                if (jobServiceSchema == null) {
                    jobServiceSchema = (JobServiceSchemaConfiguration) domainSchema.findDomainService("JobService");
                    if (jobServiceSchema == null) {
                        jobServiceSchema = new JobServiceSchemaConfiguration("JobService", null,
                                Collections.<String, JobSchemaConfiguration>emptyMap(), false);
                        domainSchema.addDomainService(jobServiceSchema);
                    }
                }

                jobServiceSchema.addPredefinedJob(entry.getKey(), loadJob(entry.getKey(), (JsonObject) entry.getValue(), context));
            }

            return null;
        } else if (type.equals("Jobs")) {
            Set<JobSchemaConfiguration> jobs = new LinkedHashSet<JobSchemaConfiguration>();
            for (Map.Entry<String, Object> entry : element)
                jobs.add(loadJob(entry.getKey(), (JsonObject) entry.getValue(), context));

            return jobs;
        } else if (type.equals("StandardSchedule"))
            return loadStandardSchedule(element, context);
        else if (type.equals("StandardSchedulePeriod"))
            return loadStandardSchedulePeriod(element, context);
        else
            throw new InvalidConfigurationException();
    }

    private JobSchemaConfiguration loadJob(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        String description = element.get("description", null);
        String group = element.get("group", null);
        Map<String, Object> parameters = loadParameters((JsonObject) element.get("parameters", null));
        JobOperationSchemaConfiguration operation = loadOperation((JsonObject) element.get("operation"), context);
        ScheduleSchemaConfiguration schedule = loadSchedule((JsonObject) element.get("schedule"), context);
        boolean enabled = element.get("enabled");
        int restartCount = ((Long) element.get("restartCount")).intValue();
        long restartPeriod = element.get("restartPeriod");
        Long maxExecutionPeriod = element.get("maxExecutionPeriod", null);

        if (type.equals("OneTimeJob")) {
            return new OneTimeJobSchemaConfiguration(name, description, group, parameters, operation, schedule, enabled,
                    restartCount, restartPeriod, maxExecutionPeriod != null ? maxExecutionPeriod : 0);
        } else if (type.equals("RecurrentJob")) {
            long repeatCount = element.get("repeatCount");
            SchedulePeriodSchemaConfiguration period = loadSchedulePeriod((JsonObject) element.get("period"), context);
            return new RecurrentJobSchemaConfiguration(name, description, group, parameters, operation, schedule, enabled,
                    restartCount, restartPeriod, maxExecutionPeriod != null ? maxExecutionPeriod : 0, repeatCount, period);
        } else
            throw new InvalidConfigurationException(messages.invalidJobType(type));
    }

    private Map<String, Object> loadParameters(JsonObject element) {
        if (element == null)
            return java.util.Collections.emptyMap();
        else
            return JsonUtils.toMap(element);
    }

    private JobOperationSchemaConfiguration loadOperation(JsonObject element, ILoadContext context) {
        return (JobOperationSchemaConfiguration) load(null, null, element, context);
    }

    private ScheduleSchemaConfiguration loadSchedule(JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("StandardSchedule"))
            return loadStandardSchedule(element, context);
        else
            return (ScheduleSchemaConfiguration) load(null, type, element, context);
    }

    private ScheduleSchemaConfiguration loadStandardSchedule(JsonObject element, ILoadContext context) {
        String dateFormat = element.get("dateFormat", null);
        String timeFormat = element.get("timeFormat", null);

        String timeZone = element.get("timeZone", null);
        if (timeZone == null)
            timeZone = context.findParameter("timeZone");

        String locale = element.get("locale", null);
        if (locale == null)
            locale = context.findParameter("locale");

        ScheduleExpressionParser parser = new ScheduleExpressionParser(timeZone, locale, dateFormat, timeFormat);
        return parser.parse((String) element.get("expression"));
    }

    private SchedulePeriodSchemaConfiguration loadSchedulePeriod(JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("StandardSchedulePeriod"))
            return loadStandardSchedulePeriod(element, context);
        else
            return (SchedulePeriodSchemaConfiguration) load(null, type, element, context);
    }

    private SchedulePeriodSchemaConfiguration loadStandardSchedulePeriod(JsonObject element, ILoadContext context) {
        StandardSchedulePeriodSchemaConfiguration.UnitType unitType = loadUnitType((String) element.get("type"));
        StandardSchedulePeriodSchemaConfiguration.Kind kind = loadKind((String) element.get("kind"));
        int amount = ((Long) element.get("amount")).intValue();
        String timeZone = element.get("timeZone", null);
        if (timeZone == null)
            timeZone = context.findParameter("timeZone");

        return new StandardSchedulePeriodSchemaConfiguration(unitType, kind, amount, timeZone);
    }

    private StandardSchedulePeriodSchemaConfiguration.Kind loadKind(String value) {
        if (value.equals("relative"))
            return StandardSchedulePeriodSchemaConfiguration.Kind.RELATIVE;
        else if (value.equals("absolute"))
            return StandardSchedulePeriodSchemaConfiguration.Kind.ABSOLUTE;
        else
            return Assert.error();
    }

    private StandardSchedulePeriodSchemaConfiguration.UnitType loadUnitType(String value) {
        if (value.equals("millisecond"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.MILLISECOND;
        else if (value.equals("second"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.SECOND;
        else if (value.equals("minute"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.MINUTE;
        else if (value.equals("hour"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.HOUR;
        else if (value.equals("day"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.DAY;
        else if (value.equals("week"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.WEEK;
        else if (value.equals("month"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.MONTH;
        else if (value.equals("year"))
            return StandardSchedulePeriodSchemaConfiguration.UnitType.YEAR;
        else
            return Assert.error();
    }

    private interface IMessages {
        @DefaultMessage("Job type ''{0}'' is not valid.")
        ILocalizedMessage invalidJobType(String type);
    }
}
