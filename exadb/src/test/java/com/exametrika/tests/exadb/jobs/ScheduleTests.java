/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.jobs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.jobs.config.model.Schedules;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.CompositeScheduleSchemaConfiguration.Type;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.impl.exadb.jobs.schedule.StandardSchedulePeriod;
import com.exametrika.spi.exadb.jobs.ISchedule;


/**
 * The {@link ScheduleTests} are tests for {@link ISchedule}.
 *
 * @author Medvedev-A
 */
public class ScheduleTests {
    @Before
    public void setUp() {
        Locale.setDefault(Locale.forLanguageTag("ru-RU"));
    }

    @Test
    public void testTimeSchedule() throws Throwable {
        ISchedule schedule = Schedules.time().from(5).to(20).toSchedule().createSchedule();
        assertThat(schedule.evaluate(time("04:59:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(time("20:59:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 21:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.time().from(time("05:10", "HH:mm")).to(time("05:20", "HH:mm")).toSchedule().createSchedule();
        assertThat(schedule.evaluate(time("05:09:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(time("05:10:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:20:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:21:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.time().set(14).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.12.2010 13:59:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("01.12.2010 14:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 14:59:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 15:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.time().set(time("05:10", "HH:mm")).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:09:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:10:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:10:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.12.2010 05:11:00.000").getTimeInMillis()), is(false));
    }

    @Test
    public void testDateSchedule() throws Throwable {
        ISchedule schedule = Schedules.date().from(2013, 04).to(2013, 05).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("31.03.2013 23:59:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("01.04.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("31.05.2013 23:59:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.06.2013 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.date().set(2013, 04, 30, 22).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("30.04.2013 21:59:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("30.04.2013 22:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("30.04.2013 22:59:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("30.04.2013 23:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.date().set(time("04.2013", "MM.yyyy")).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("31.03.2013 23:59:59.999").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("01.04.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("30.04.2013 23:59:59.999").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("01.05.2013 00:00:00.000").getTimeInMillis()), is(false));
    }

    @Test
    public void testDayOfMonthSchedule() throws Throwable {
        ISchedule schedule = Schedules.dayOfMonth().from(2).toLast(1).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.01.2012 00:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("02.02.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("28.02.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("29.02.2012 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.dayOfMonth().set(15).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("15.02.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("14.02.2012 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.dayOfMonth().last().toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("29.02.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("28.02.2012 00:00:00.000").getTimeInMillis()), is(false));
    }

    @Test
    public void testDayOfYearSchedule() throws Throwable {
        ISchedule schedule = Schedules.dayOfYear().from(2).toLast(1).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.01.2012 00:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("02.01.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("30.12.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("31.12.2012 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.dayOfYear().set(15).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("15.01.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("14.01.2012 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.dayOfYear().last().toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("31.12.2012 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("30.12.2012 00:00:00.000").getTimeInMillis()), is(false));
    }

    @Test
    public void testDayOfWeekSchedule() throws Throwable {
        ISchedule schedule = Schedules.dayOfWeek().set(1).set(3).set(5).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.05.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("02.05.2013 00:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("03.05.2013 00:00:00.000").getTimeInMillis()), is(true));
    }

    @Test
    public void testMonthSchedule() throws Throwable {
        ISchedule schedule = Schedules.month().set(2).set(4).set(6).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.02.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("02.05.2013 00:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("03.06.2013 00:00:00.000").getTimeInMillis()), is(true));
    }

    @Test
    public void testDayOfWeekInMonthSchedule() throws Throwable {
        ISchedule schedule = Schedules.dayOfWeekInMonth().from(2, 1).toLast(4, 1).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("01.04.2013 00:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("02.04.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("18.04.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("19.04.2013 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.dayOfWeekInMonth().set(1, 5).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("29.04.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("28.04.2013 00:00:00.000").getTimeInMillis()), is(false));

        schedule = Schedules.dayOfWeekInMonth().last(2).toSchedule().createSchedule();
        assertThat(schedule.evaluate(dateTime("30.04.2013 00:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("29.04.2013 00:00:00.000").getTimeInMillis()), is(false));
    }

    @Test
    public void testCompositeSchedule() throws Throwable {
        ISchedule schedule = Schedules.composite(Type.AND)
                .month().set(2).set(4).set(6).end()
                .dayOfMonth().set(10).end()
                .dayOfWeek().set(3).set(4).end()
                .orGroup()
                .time().from(5).to(8).end()
                .time().from(20).to(23).end()
                .end().toSchedule().createSchedule();

        assertThat(schedule.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis()), is(true));
        assertThat(schedule.evaluate(dateTime("10.04.2013 04:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("10.03.2013 06:00:00.000").getTimeInMillis()), is(false));
        assertThat(schedule.evaluate(dateTime("11.04.2013 06:00:00.000").getTimeInMillis()), is(false));
    }

    @Test
    public void testSchedulePeriod() throws Throwable {
        StandardSchedulePeriod period = (StandardSchedulePeriod) new StandardSchedulePeriodSchemaConfiguration(UnitType.DAY, Kind.RELATIVE, 10).createPeriod();
        assertThat(period.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis(),
                dateTime("20.04.2013 06:00:00.000").getTimeInMillis()), is(true));
        assertThat(period.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis(),
                dateTime("20.04.2013 05:00:00.000").getTimeInMillis()), is(false));

        period = (StandardSchedulePeriod) new StandardSchedulePeriodSchemaConfiguration(UnitType.DAY, Kind.ABSOLUTE, 1).createPeriod();
        assertThat(period.evaluate(dateTime("10.04.2013 11:00:00.000").getTimeInMillis(),
                dateTime("10.04.2013 23:59:59.999").getTimeInMillis()), is(false));
        assertThat(period.evaluate(dateTime("10.04.2013 11:00:00.000").getTimeInMillis(),
                dateTime("11.04.2013 00:00:00.000").getTimeInMillis()), is(true));

        period = (StandardSchedulePeriod) new StandardSchedulePeriodSchemaConfiguration(UnitType.HOUR, Kind.RELATIVE, 1).createPeriod();
        assertThat(period.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis(),
                dateTime("10.04.2013 07:00:00.000").getTimeInMillis()), is(true));
        assertThat(period.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis(),
                dateTime("10.04.2013 06:59:59.999").getTimeInMillis()), is(false));

        period = (StandardSchedulePeriod) new StandardSchedulePeriodSchemaConfiguration(UnitType.WEEK, Kind.ABSOLUTE, 1).createPeriod();
        assertThat(period.evaluate(dateTime("10.04.2013 11:00:00.000").getTimeInMillis(),
                dateTime("14.04.2013 23:59:59.999").getTimeInMillis()), is(false));
        assertThat(period.evaluate(dateTime("10.04.2013 11:00:00.000").getTimeInMillis(),
                dateTime("15.04.2013 00:00:00.000").getTimeInMillis()), is(true));

        period = (StandardSchedulePeriod) new StandardSchedulePeriodSchemaConfiguration(UnitType.MONTH, Kind.RELATIVE, 1).createPeriod();
        assertThat(period.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis(),
                dateTime("10.05.2013 06:00:00.000").getTimeInMillis()), is(true));
        assertThat(period.evaluate(dateTime("10.04.2013 06:00:00.000").getTimeInMillis(),
                dateTime("10.05.2013 05:59:59.999").getTimeInMillis()), is(false));

        period = (StandardSchedulePeriod) new StandardSchedulePeriodSchemaConfiguration(UnitType.MINUTE, Kind.ABSOLUTE, 15).createPeriod();
        assertThat(period.evaluate(dateTime("17.08.2015 16:07:00.000").getTimeInMillis(),
                dateTime("17.08.2015 16:15:00.000").getTimeInMillis()), is(true));
    }

    @Test
    public void testScheduleExpressionParser() throws Throwable {
        assertThat(Schedules.parse("time(5..20)", null, "HH").equals(Schedules.time().from(5).to(20).toSchedule()), is(true));
        assertThat(Schedules.parse("time-(5:30..20:10)", null, "HH:mm").equals(Schedules.time().from(5, 30).to(20, 10).exclude().toSchedule()), is(true));
        assertThat(Schedules.parse("time(5:30)", null, "HH:mm").equals(Schedules.time().set(5, 30).toSchedule()), is(true));

        assertThat(Schedules.parse("date(5.01.2013 5:10..06.08.2013 6:20)", "dd.MM.yyyy HH:mm", null).equals(
                Schedules.date().from(2013, 1, 5, 5, 10).to(2013, 8, 6, 6, 20).toSchedule()), is(true));
        assertThat(Schedules.parse("date-(01.2013..08.2013)", "MM.yyyy", null).equals(
                Schedules.date().from(2013, 1).to(2013, 8).exclude().toSchedule()), is(true));
        assertThat(Schedules.parse("date(5.01.2013)", "dd.MM.yyyy", null).equals(Schedules.date().set(2013, 1, 5).toSchedule()), is(true));

        assertThat(Schedules.parse("dayOfMonth(5..*)", null, null).equals(Schedules.dayOfMonth().from(5).toLast().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfMonth-(*-3..*-1)", null, null).equals(Schedules.dayOfMonth().fromLast(3).toLast(1).exclude().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfMonth(*)", null, null).equals(Schedules.dayOfMonth().last().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfMonth(*-15)", null, null).equals(Schedules.dayOfMonth().last(15).toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfMonth(15)", null, null).equals(Schedules.dayOfMonth().set(15).toSchedule()), is(true));

        assertThat(Schedules.parse("dayOfYear(5..*)", null, null).equals(Schedules.dayOfYear().from(5).toLast().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfYear-(*-3..*-1)", null, null).equals(Schedules.dayOfYear().fromLast(3).toLast(1).exclude().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfYear(*)", null, null).equals(Schedules.dayOfYear().last().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfYear(*-15)", null, null).equals(Schedules.dayOfYear().last(15).toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfYear(15)", null, null).equals(Schedules.dayOfYear().set(15).toSchedule()), is(true));

        assertThat(Schedules.parse("dayOfWeek(1,3,7)", null, null).equals(Schedules.dayOfWeek().set(1).set(3).set(7).toSchedule()), is(true));

        assertThat(Schedules.parse("month(1,10,12)", null, null).equals(Schedules.month().set(1).set(10).set(12).toSchedule()), is(true));

        assertThat(Schedules.parse("dayOfWeekInMonth(1/5..1/*)", null, null).equals(Schedules.dayOfWeekInMonth().from(1, 5).toLast(1).toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfWeekInMonth-(2/*-3..3/*-1)", null, null).equals(Schedules.dayOfWeekInMonth().fromLast(2, 3).toLast(3, 1).exclude().toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfWeekInMonth(4/*)", null, null).equals(Schedules.dayOfWeekInMonth().last(4).toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfWeekInMonth(5/*-4)", null, null).equals(Schedules.dayOfWeekInMonth().last(5, 4).toSchedule()), is(true));
        assertThat(Schedules.parse("dayOfWeekInMonth(6/5)", null, null).equals(Schedules.dayOfWeekInMonth().set(6, 5).toSchedule()), is(true));

        assertThat(Schedules.parse("lowMemory(100000000)", null, null).equals(Schedules.lowMemory().minFreeSpace(100000000).toSchedule()), is(true));
        assertThat(Schedules.parse("lowDisk(/home/tmp/db  test/test2,100000000)", null, null).equals(Schedules.lowDisk()
                .path("/home/tmp/db  test/test2").minFreeSpace(100000000).toSchedule()), is(true));

        assertThat(Schedules.parse("and(month(2,4,6),dayOfMonth(10),dayOfWeek(4,5),or(time(5..8), time(20..23)))", null, "HH").equals(
                Schedules.composite(Type.AND)
                        .month().set(2).set(4).set(6).end()
                        .dayOfMonth().set(10).end()
                        .dayOfWeek().set(4).set(5).end()
                        .orGroup()
                        .time().from(5).to(8).end()
                        .time().from(20).to(23).end()
                        .end().toSchedule()), is(true));
    }

    private Calendar time(String value) throws Throwable {
        DateFormat format = new SimpleDateFormat("HH:mm:ss.SS");
        format.parse(value);
        return format.getCalendar();
    }

    private Calendar time(String value, String pattern) throws Throwable {
        DateFormat format = new SimpleDateFormat(pattern);
        format.getCalendar().clear();
        format.parse(value);
        return format.getCalendar();
    }

    private Calendar dateTime(String value) throws Throwable {
        DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SS");
        format.getCalendar().clear();
        format.parse(value);
        return format.getCalendar();
    }
}
