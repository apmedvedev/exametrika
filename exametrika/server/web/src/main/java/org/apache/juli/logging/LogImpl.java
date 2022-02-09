/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package org.apache.juli.logging;

import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.Assert;


/**
 * The {@link LogImpl} is an implementation of {@link Log} interface.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogImpl implements Log {
    private final ILogger logger;

    public LogImpl(ILogger logger) {
        Assert.notNull(logger);

        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isLogEnabled(LogLevel.DEBUG);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLogEnabled(LogLevel.ERROR);
    }

    @Override
    public boolean isFatalEnabled() {
        return isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLogEnabled(LogLevel.INFO);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLogEnabled(LogLevel.TRACE);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLogEnabled(LogLevel.WARNING);
    }

    @Override
    public void trace(Object message) {
        logger.log(LogLevel.TRACE, Messages.nonLocalized(message));
    }

    @Override
    public void trace(Object message, Throwable t) {
        logger.log(LogLevel.TRACE, Messages.nonLocalized(message), t);
    }

    @Override
    public void debug(Object message) {
        logger.log(LogLevel.DEBUG, Messages.nonLocalized(message));

    }

    @Override
    public void debug(Object message, Throwable t) {
        logger.log(LogLevel.DEBUG, Messages.nonLocalized(message), t);
    }

    @Override
    public void info(Object message) {
        logger.log(LogLevel.INFO, Messages.nonLocalized(message));
    }

    @Override
    public void info(Object message, Throwable t) {
        logger.log(LogLevel.INFO, Messages.nonLocalized(message), t);
    }

    @Override
    public void warn(Object message) {
        logger.log(LogLevel.WARNING, Messages.nonLocalized(message));
    }

    @Override
    public void warn(Object message, Throwable t) {
        logger.log(LogLevel.WARNING, Messages.nonLocalized(message), t);
    }

    @Override
    public void error(Object message) {
        logger.log(LogLevel.ERROR, Messages.nonLocalized(message));
    }

    @Override
    public void error(Object message, Throwable t) {
        logger.log(LogLevel.ERROR, Messages.nonLocalized(message), t);
    }

    @Override
    public void fatal(Object message) {
        error(message);
    }

    @Override
    public void fatal(Object message, Throwable t) {
        error(message, t);
    }
}
