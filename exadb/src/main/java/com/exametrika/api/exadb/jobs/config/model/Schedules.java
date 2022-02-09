/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link Schedules} represents a helper class to build schedule configurations.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Schedules {
    public static CompositeScheduleSchemaBuilder composite(CompositeScheduleSchemaConfiguration.Type type) {
        return new CompositeScheduleSchemaBuilder(type);
    }

    public static TimeScheduleSchemaBuilder time() {
        return new TimeScheduleSchemaBuilder();
    }

    public static DateScheduleSchemaBuilder date() {
        return new DateScheduleSchemaBuilder();
    }

    public static DayOfMonthScheduleSchemaBuilder dayOfMonth() {
        return new DayOfMonthScheduleSchemaBuilder();
    }

    public static DayOfWeekInMonthScheduleSchemaBuilder dayOfWeekInMonth() {
        return new DayOfWeekInMonthScheduleSchemaBuilder();
    }

    public static DayOfWeekScheduleSchemaBuilder dayOfWeek() {
        return new DayOfWeekScheduleSchemaBuilder();
    }

    public static DayOfYearScheduleSchemaBuilder dayOfYear() {
        return new DayOfYearScheduleSchemaBuilder();
    }

    public static MonthScheduleSchemaBuilder month() {
        return new MonthScheduleSchemaBuilder();
    }

    public static LowMemoryScheduleSchemaBuilder lowMemory() {
        return new LowMemoryScheduleSchemaBuilder();
    }

    public static LowDiskScheduleSchemaBuilder lowDisk() {
        return new LowDiskScheduleSchemaBuilder();
    }

    /**
     * Parses schedule expression. Schedule expression has the following format:
     * <schedule> ::= <time_schedule> | <date_schedule> | <day_of_month_schedule> | <day_of_year_schedule> |
     * <day_of_week_schedule> | <day_of_week_in_month_schedule> | <month_schedule> | <low_memory_schedule> | <low_disk_schedule> |
     * <composite_schedule>
     * <time_schedule> ::= 'time(' <from_time> '..' <to_time> ')' | 'time(' <time> ')'
     * <from_time> ::= <time>, <to_time> ::= <time>, <time> ::= time format of current locale
     * <date_schedule> ::= 'date(' <from_date> '..' <to_date> ')' | 'date(' <date> ')'
     * <from_date> ::= <date>, <to_date> ::= <date>, <date> ::= date and time format of current locale
     * <day_of_month_schedule> ::= 'dayOfMonth(' <from_day> '..' <to_day> ')' | 'dayOfMonth(' <day> ')'
     * <day_of_year_schedule> ::= 'dayOfYear(' <from_day> '..' <to_day> ')' | 'dayOfYear(' <day> ')'
     * <from_day> ::= <day>, <to_day> ::= <day>, <day> ::= <day_index> | <last_day> | <last_day> '-' <offset_index>
     * <last_day> ::= '*'
     * <day_index> ::= index of day starting from 1
     * <offset_index> ::= offset index starting from 0
     * <day_of_week_schedule> ::= 'dayOfWeek(' <day_index_list> ')'
     * <day_index_list> ::= <day_index_list> ',' <day_index> | <day_index>
     * <day_index> ::= index of day in week, 1 - locale specific first week day
     * <day_of_week_in_month_schedule> ::= 'dayOfWeekInMonth(' <from_day_of_week_in_month> '..' <to_day_of_week_in_month> ')'
     * | 'dayOfWeekInMonth(' <day_of_week_in_month> ')'
     * <from_day_of_week_in_month> ::= <day_of_week_in_month>, <to_day_of_week_in_month> ::= <day_of_week_in_month>,
     * <day_of_week_in_month> ::= <day_of_week_index> '/' <day_of_week_in_month_index>
     * <day_of_week_in_month_index> ::= <day_of_week_in_month_ordinal> | <last_day_of_week_in_month>
     * | <last_day_of_week_in_month> '-' <offset_index>
     * <last_day_of_week_in_month>> ::= '*'
     * <day_of_week_index> ::= index of day in week, 1 - locale specific first week day
     * <day_of_week_in_month_ordinal> ::= index of day of week in month starting from 1
     * <offset_index> ::= offset index starting from 0
     * <month_schedule> ::= 'month(' <month_index_list> ')'
     * <month_index_list> ::= <month_index_list> ',' <month_index> | <month_index>
     * <month_index> ::= index of month in year, 1 - january
     * <low_memory_schedule> ::= 'lowMemory(' <min_free_space> ')'
     * <low_disk_schedule> ::= 'lowDisk(' <path> ',' <min_free_space> ')'
     * <composite_schedule> ::= 'and(' <schedule_list> ')' | 'or(' <schedule_list> ')'
     * <schedule_list> ::= <schedule_list> ',' <schedule> | <schedule>
     * Each schedule has general format: <schedule_name> '(' <schedule_parameters> ')'. Minus sign ('-') after <schedule_name>
     * designates excluding schedule.
     * Examples:
     * time schedule: time-(10:30:45.475..10:46) / 10:30
     * date schedule: date(01.12.2001 10:30:45.475..01.12.2001 10:46) / date(01.12.2001 10:46)
     * day of month schedule: dayOfMonth(5..*) / dayOfMonth-(*-3..*-2) / dayOfMonth(5) / dayOfMonth(*)
     * day of year schedule: dayOfYear(5..*) / dayOfYear(*-3..*-2) / dayOfYear(5) / dayOfYear(*)
     * day of week schedule: dayOfWeek(1,3,5)
     * month schedule: month(1,3,5)
     * day of week in month schedule: dayOfWeekInMonth(5/2..7/*-1) / dayOfWeekInMonth(1/1) / dayOfWeekInMonth(1/*)
     * composite schedule: and(or(time(10:30:45.475..10:46), time(10:30:45.475..10:46))), dayOfWeek(1,2,3), dayOfYear(0..*-1))
     *
     * @param expression schedule expression
     * @param timeZone   time zone or null if default time zone is used
     * @param locale     locale or null if default locale is used
     * @param dateFormat date and time format or null if default date and time format is used
     * @param timeFormat time format or null if default time format is used
     * @return schedule configuration
     */
    public static ScheduleSchemaConfiguration parse(String expression, String timeZone, String locale, String dateFormat, String timeFormat) {
        Assert.notNull(expression);
        ScheduleExpressionParser parser = new ScheduleExpressionParser(timeZone, locale, dateFormat, timeFormat);
        return parser.parse(expression);
    }

    public static ScheduleSchemaConfiguration parse(String expression, String dateFormat, String timeFormat) {
        return parse(expression, null, null, dateFormat, timeFormat);
    }

    public static ScheduleSchemaConfiguration parse(String expression) {
        return parse(expression, null, null, null, null);
    }

    private Schedules() {
    }
}