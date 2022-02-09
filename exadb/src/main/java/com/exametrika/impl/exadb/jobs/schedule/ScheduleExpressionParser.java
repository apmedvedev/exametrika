/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.schedule;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.exametrika.api.exadb.jobs.config.model.CompositeScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.CompositeScheduleSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.DateScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.DayOfMonthScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.DayOfWeekInMonthScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.DayOfWeekScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.DayOfYearScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.LowDiskScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.LowMemoryScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.MonthScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.TimeScheduleSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.CompositeScheduleSchemaConfiguration.Type;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Locales;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaBuilder;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link ScheduleExpressionParser} represents a schedule expression parser.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ScheduleExpressionParser {
    private static final IMessages messages = Messages.get(IMessages.class);
    private State state;
    private ScheduleSchemaBuilder scheduleBuilder;
    private final String timeZone;
    private final String locale;
    private final String dateFormat;
    private final String timeFormat;

    public ScheduleExpressionParser() {
        this(null, null, null, null);
    }

    public ScheduleExpressionParser(String dateFormat, String timeFormat) {
        this(null, null, dateFormat, timeFormat);
    }

    public ScheduleExpressionParser(String timeZone, String locale, String dateFormat, String timeFormat) {
        this.timeZone = timeZone;
        this.locale = locale;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
    }

    public ScheduleSchemaConfiguration parse(String expression) {
        Assert.notNull(expression);

        state = new State(expression);
        parseSchedule(true);

        if (scheduleBuilder != null)
            return scheduleBuilder.toSchedule();
        else
            return null;
    }

    private void parseSchedule(boolean topLevel) {
        while (true) {
            Token token = readToken();
            if (token == Token.EOL) {
                if (topLevel)
                    break;
                else
                    syntaxError();
            } else if (token == Token.RightBrace | token == Token.Comma) {
                if (!topLevel) {
                    unread();
                    break;
                } else
                    syntaxError();
            } else if (token == Token.ID) {
                boolean exclude = isExcluding();

                if (state.id.equals("and"))
                    parseCompositeSchedule(CompositeScheduleSchemaConfiguration.Type.AND, exclude);
                else if (state.id.equals("or"))
                    parseCompositeSchedule(CompositeScheduleSchemaConfiguration.Type.OR, exclude);
                else if (state.id.equals("time"))
                    parseTimeSchedule(exclude);
                else if (state.id.equals("date"))
                    parseDateSchedule(exclude);
                else if (state.id.equals("dayOfMonth"))
                    parseDayOfMonthSchedule(exclude);
                else if (state.id.equals("dayOfYear"))
                    parseDayOfYearSchedule(exclude);
                else if (state.id.equals("dayOfWeek"))
                    parseDayOfWeekSchedule(exclude);
                else if (state.id.equals("month"))
                    parseMonthSchedule(exclude);
                else if (state.id.equals("dayOfWeekInMonth"))
                    parseDayOfWeekInMonthSchedule(exclude);
                else if (state.id.equals("lowMemory"))
                    parseLowMemorySchedule(exclude);
                else if (state.id.equals("lowDisk"))
                    parseLowDiskSchedule(exclude);
                else
                    syntaxError();
            } else
                syntaxError();
        }
    }

    private boolean isExcluding() {
        Token token = readToken();
        if (token == Token.Minus)
            return true;
        else {
            unread();
            return false;
        }
    }

    private void parseDayOfWeekInMonthSchedule(boolean exclude) {
        DayOfWeekInMonthScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new DayOfWeekInMonthScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).dayOfWeekInMonth();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        int startPos = state.pos;
        token = findToken(Token.Interval, Token.RightBrace);
        if (token == Token.Interval) {
            DayOfWeekInMonth startDay = parseDayOfWeekInMonth(state.expression.substring(startPos, state.pos - 2));
            startPos = state.pos;
            findToken(Token.RightBrace);
            DayOfWeekInMonth endDay = parseDayOfWeekInMonth(state.expression.substring(startPos, state.pos - 1));
            if (startDay.index != -1)
                scheduleBuilder.from(startDay.dayOfWeek, startDay.index);
            else
                scheduleBuilder.fromLast(startDay.dayOfWeek, startDay.offset);

            if (endDay.index != -1)
                scheduleBuilder.to(endDay.dayOfWeek, endDay.index);
            else
                scheduleBuilder.toLast(endDay.dayOfWeek, endDay.offset);
        } else {
            DayOfWeekInMonth day = parseDayOfWeekInMonth(state.expression.substring(startPos, state.pos - 1));
            if (day.index != -1)
                scheduleBuilder.set(day.dayOfWeek, day.index);
            else
                scheduleBuilder.last(day.dayOfWeek, day.offset);
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseMonthSchedule(boolean exclude) {
        MonthScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new MonthScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).month();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        while (true) {
            token = readToken();
            if (token == Token.Comma)
                continue;
            if (token == Token.RightBrace)
                break;
            if (token == Token.Number)
                scheduleBuilder.set(state.number);
            else
                syntaxError();
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseDayOfWeekSchedule(boolean exclude) {
        DayOfWeekScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new DayOfWeekScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).dayOfWeek();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        while (true) {
            token = readToken();
            if (token == Token.Comma)
                continue;
            if (token == Token.RightBrace)
                break;
            if (token == Token.Number)
                scheduleBuilder.set(state.number);
            else
                syntaxError();
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseDayOfYearSchedule(boolean exclude) {
        DayOfYearScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new DayOfYearScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).dayOfYear();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        int startPos = state.pos;
        token = findToken(Token.Interval, Token.RightBrace);
        if (token == Token.Interval) {
            DayOf startDay = parseDayOf(state.expression.substring(startPos, state.pos - 2));
            startPos = state.pos;
            findToken(Token.RightBrace);
            DayOf endDay = parseDayOf(state.expression.substring(startPos, state.pos - 1));

            if (startDay.index != -1)
                scheduleBuilder.from(startDay.index);
            else
                scheduleBuilder.fromLast(startDay.offset);

            if (endDay.index != -1)
                scheduleBuilder.to(endDay.index);
            else
                scheduleBuilder.toLast(endDay.offset);
        } else {
            DayOf day = parseDayOf(state.expression.substring(startPos, state.pos - 1));
            if (day.index != -1)
                scheduleBuilder.set(day.index);
            else
                scheduleBuilder.last(day.offset);
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseDayOfMonthSchedule(boolean exclude) {
        DayOfMonthScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new DayOfMonthScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).dayOfMonth();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        int startPos = state.pos;
        token = findToken(Token.Interval, Token.RightBrace);
        if (token == Token.Interval) {
            DayOf startDay = parseDayOf(state.expression.substring(startPos, state.pos - 2));
            startPos = state.pos;
            findToken(Token.RightBrace);
            DayOf endDay = parseDayOf(state.expression.substring(startPos, state.pos - 1));
            if (startDay.index != -1)
                scheduleBuilder.from(startDay.index);
            else
                scheduleBuilder.fromLast(startDay.offset);

            if (endDay.index != -1)
                scheduleBuilder.to(endDay.index);
            else
                scheduleBuilder.toLast(endDay.offset);
        } else {
            DayOf day = parseDayOf(state.expression.substring(startPos, state.pos - 1));
            if (day.index != -1)
                scheduleBuilder.set(day.index);
            else
                scheduleBuilder.last(day.offset);
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseDateSchedule(boolean exclude) {
        DateScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new DateScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).date();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        int startPos = state.pos;
        token = findToken(Token.Interval, Token.RightBrace);
        if (token == Token.Interval) {
            Calendar startDate = parseDate(state.expression.substring(startPos, state.pos - 2));
            startPos = state.pos;
            findToken(Token.RightBrace);
            Calendar endDate = parseDate(state.expression.substring(startPos, state.pos - 1));
            scheduleBuilder.from(startDate).to(endDate);
        } else {
            Calendar date = parseDate(state.expression.substring(startPos, state.pos - 1));
            scheduleBuilder.set(date);
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseTimeSchedule(boolean exclude) {
        TimeScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new TimeScheduleSchemaBuilder();
            scheduleBuilder.timeZone(timeZone);
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).time();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        int startPos = state.pos;
        token = findToken(Token.Interval, Token.RightBrace);
        if (token == Token.Interval) {
            Calendar startTime = parseTime(state.expression.substring(startPos, state.pos - 2));
            startPos = state.pos;
            findToken(Token.RightBrace);
            Calendar endTime = parseTime(state.expression.substring(startPos, state.pos - 1));
            scheduleBuilder.from(startTime).to(endTime);
        } else {
            Calendar time = parseTime(state.expression.substring(startPos, state.pos - 1));
            scheduleBuilder.set(time);
        }

        if (exclude)
            scheduleBuilder.exclude();
    }

    private DayOf parseDayOf(String expression) {
        return parseDayOf(new State(expression));
    }

    private DayOf parseDayOf(State state) {
        Token token = readToken(state);
        DayOf day = new DayOf();
        if (token == Token.Number)
            day.index = state.number;
        else if (token == Token.Asterisk) {
            day.index = -1;

            token = readToken(state);
            if (token == Token.Minus) {
                token = readToken(state);
                if (token == Token.Number)
                    day.offset = state.number;
                else
                    syntaxError();
            } else if (token != Token.EOL)
                syntaxError();
        } else
            syntaxError();

        token = readToken(state);
        if (token != Token.EOL)
            syntaxError();

        return day;
    }

    private DayOfWeekInMonth parseDayOfWeekInMonth(String expression) {
        State state = new State(expression);
        Token token = readToken(state);
        DayOfWeekInMonth day = new DayOfWeekInMonth();
        if (token != Token.Number)
            syntaxError();

        day.dayOfWeek = state.number;
        token = readToken(state);
        if (token != Token.Slash)
            syntaxError();

        DayOf dayOf = parseDayOf(state);
        day.index = dayOf.index;
        day.offset = dayOf.offset;

        return day;
    }

    private Calendar parseTime(String expression) {
        Locale locale = this.locale != null ? Locales.getLocale(this.locale) : Locale.getDefault();

        DateFormat format;
        if (timeFormat == null)
            format = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        else
            format = new SimpleDateFormat(timeFormat, locale);

        format.getCalendar().clear();

        try {
            format.parse(expression);
        } catch (ParseException e) {
            syntaxError(e);
        }

        return format.getCalendar();
    }

    private Calendar parseDate(String expression) {
        Locale locale = this.locale != null ? Locales.getLocale(this.locale) : Locale.getDefault();

        DateFormat format;
        if (dateFormat == null)
            format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        else
            format = new SimpleDateFormat(dateFormat, locale);

        format.getCalendar().clear();

        try {
            format.parse(expression);
        } catch (ParseException e) {
            syntaxError(e);
        }

        return format.getCalendar();
    }

    private void parseLowMemorySchedule(boolean exclude) {
        LowMemoryScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new LowMemoryScheduleSchemaBuilder();
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).lowMemory();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        token = readToken();
        if (token != Token.Number)
            syntaxError();

        scheduleBuilder.minFreeSpace(state.number);

        token = readToken();
        if (token != Token.RightBrace)
            syntaxError();

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseLowDiskSchedule(boolean exclude) {
        LowDiskScheduleSchemaBuilder scheduleBuilder;
        if (this.scheduleBuilder == null) {
            scheduleBuilder = new LowDiskScheduleSchemaBuilder();
            this.scheduleBuilder = scheduleBuilder;
        } else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) this.scheduleBuilder).lowDisk();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        int startPos = state.pos;
        token = findToken(Token.Comma);

        scheduleBuilder.path(state.expression.substring(startPos, state.pos - 1));

        token = readToken();
        if (token != Token.Number)
            syntaxError();

        scheduleBuilder.minFreeSpace(state.number);

        token = readToken();
        if (token != Token.RightBrace)
            syntaxError();

        if (exclude)
            scheduleBuilder.exclude();
    }

    private void parseCompositeSchedule(Type type, boolean exclude) {
        boolean first = false;
        if (scheduleBuilder == null) {
            scheduleBuilder = new CompositeScheduleSchemaBuilder(type);
            ((CompositeScheduleSchemaBuilder) scheduleBuilder).timeZone(timeZone);
            first = true;
        } else if (type == Type.AND)
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) scheduleBuilder).andGroup();
        else
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) scheduleBuilder).orGroup();

        Token token = readToken();
        if (token != Token.LeftBrace)
            syntaxError();

        while (true) {
            parseSchedule(false);

            token = readToken();
            if (token == Token.RightBrace)
                break;
            else if (token != Token.Comma)
                syntaxError();
        }

        if (exclude)
            ((CompositeScheduleSchemaBuilder) scheduleBuilder).exclude();

        if (!first)
            scheduleBuilder = ((CompositeScheduleSchemaBuilder) scheduleBuilder).end();
    }

    private void syntaxError() {
        throw new InvalidArgumentException(messages.syntaxError(state.pos));
    }

    private void syntaxError(Throwable e) {
        throw new InvalidArgumentException(messages.syntaxError(state.pos), e);
    }

    private Token findToken(Token... tokens) {
        while (true) {
            Token token = readToken();
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i] == token)
                    return token;
            }

            if (token == Token.EOL)
                syntaxError();
        }
    }

    private Token readToken() {
        return readToken(state);
    }

    private Token readToken(State state) {
        while (true) {
            char ch = read(state);
            if (ch == (char) -1)
                return Token.EOL;
            if (Character.isWhitespace(ch))
                continue;
            if (ch == '(')
                return Token.LeftBrace;
            if (ch == ')')
                return Token.RightBrace;
            if (ch == '*')
                return Token.Asterisk;
            if (ch == ',')
                return Token.Comma;
            if (ch == '-')
                return Token.Minus;
            if (ch == '/')
                return Token.Slash;
            if (ch == '.') {
                ch = read(state);
                if (ch == '.')
                    return Token.Interval;
                else {
                    unread(state);
                    continue;
                }
            }
            if (Character.isLetter(ch)) {
                StringBuilder builder = new StringBuilder();
                builder.append(ch);
                while (true) {
                    ch = read(state);
                    if (Character.isLetter(ch))
                        builder.append(ch);
                    else {
                        unread(state);
                        state.id = builder.toString();
                        return Token.ID;
                    }
                }
            }
            if (Character.isDigit(ch)) {
                int startPos = state.pos - 1;
                while (true) {
                    ch = read(state);
                    if (Character.isDigit(ch))
                        continue;
                    else {
                        unread(state);
                        state.number = Integer.parseInt(state.expression.substring(startPos, state.pos));
                        return Token.Number;
                    }
                }
            }

            return Token.Unknown;
        }
    }

    private char read(State state) {
        if (state.pos >= state.expression.length()) {
            state.pos++;
            return (char) -1;
        }

        return state.expression.charAt(state.pos++);
    }

    private void unread() {
        unread(state);
    }

    private void unread(State state) {
        state.pos--;
    }

    private enum Token {
        EOL,
        LeftBrace,
        RightBrace,
        Comma,
        Asterisk,
        Minus,
        Slash,
        Interval,
        ID,
        Number,
        Unknown
    }

    private class DayOf {
        private int index;
        private int offset;
    }

    private class DayOfWeekInMonth {
        private int dayOfWeek;
        private int index;
        private int offset;
    }

    private static class State {
        private final String expression;
        private int pos;
        private String id;
        private int number;

        private State(String expression) {
            this.expression = expression;
        }
    }

    private interface IMessages {
        @DefaultMessage("Syntax error at ''{0}''.")
        ILocalizedMessage syntaxError(int pos);
    }
}