/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.exadb.security.IRole;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.impl.exadb.security.model.RoleNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.jobs.ISchedule;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;
import com.exametrika.spi.exadb.security.IRoleMappingStrategy;


/**
 * The {@link ScheduleRoleMappingStrategy} is a role mapping strategy based on role schedule.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ScheduleRoleMappingStrategy implements IRoleMappingStrategy {
    private IDatabaseContext context;

    public ScheduleRoleMappingStrategy(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public Set<String> getRoles(ISubject subject) {
        Set<String> roles = new HashSet<String>();
        for (IRole role : subject.getRoles()) {
            if (isSubjectInRole(role))
                roles.add(role.getName());
        }

        return roles;
    }

    @Override
    public boolean isSubjectInRole(IRole role) {
        long currentTime = Times.getCurrentTime();

        RoleNode roleNode = (RoleNode) role;
        ISchedule schedule = roleNode.getData();
        if (schedule == null && roleNode.getMetadata() != null) {
            JsonObject element = roleNode.getMetadata().get("schedule", null);
            if (element != null) {
                schedule = loadStandardSchedule(element).createSchedule();
                roleNode.setData(schedule);
            }
        }

        if (schedule == null || schedule.evaluate(currentTime))
            return true;
        else
            return false;
    }

    private ScheduleSchemaConfiguration loadStandardSchedule(JsonObject element) {
        String dateFormat = element.get("dateFormat", null);
        String timeFormat = element.get("timeFormat", null);
        String timeZone = element.get("timeZone", null);
        if (timeZone == null)
            timeZone = context.getSchemaSpace().getCurrentSchema().getConfiguration().getTimeZone();

        String locale = element.get("locale", null);
        if (locale == null)
            locale = context.getSchemaSpace().getCurrentSchema().getConfiguration().getLocale();

        ScheduleExpressionParser parser = new ScheduleExpressionParser(timeZone, locale, dateFormat, timeFormat);
        return parser.parse((String) element.get("expression"));
    }
}
