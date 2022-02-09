/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.intercept;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.boot.IInvocation;


/**
 * The {@link AbstractDynamicInterceptor} is an abstract interceptor based on JMX.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractDynamicInterceptor implements IDynamicInterceptor, IValueMXBean {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractDynamicInterceptor.class);
    private volatile ObjectName name;
    protected final AtomicLong value = new AtomicLong();
    protected volatile IJoinPoint joinPoint;

    @Override
    public final boolean intercept(IInvocation invocation) {
        if (name != null) {
            updateValue(invocation);
            return true;
        } else
            return false;
    }

    @Override
    public final synchronized void start(IJoinPoint joinPoint) {
        Assert.notNull(joinPoint);
        Assert.checkState(name == null);

        this.joinPoint = joinPoint;

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try {
            name = new ObjectName("com.exametrika.instrument:type=Counter,name=" + joinPoint.getClassName() + ",id=" + joinPoint.getMethodName() + "@" +
                    joinPoint.getSourceLineNumber());
            if (!register(server, name)) {
                name = new ObjectName("com.exametrika.instrument:type=Counter,name=" + joinPoint.getClassName() + ",id=" + joinPoint.getMethodName() + "@" +
                        joinPoint.getSourceLineNumber() + "@" + joinPoint.getId());
                if (!register(server, name)) {
                    name = new ObjectName("com.exametrika.instrument:type=Counter,name=" + joinPoint.getClassName() + ",id=" + joinPoint.getMethodName() + "@" +
                            joinPoint.getSourceLineNumber() + "@" + joinPoint.getId() + "@" + joinPoint.getClassLoaderId());
                    unregister(server, name);
                    register(server, name);
                }
            }
        } catch (Exception e) {
            throw new InstrumentationException(e);
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started(name));
    }

    @Override
    public final synchronized void stop(boolean close) {
        if (name == null)
            return;

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        unregister(server, name);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped(name));

        name = null;
    }

    @Override
    public final long getValue() {
        return value.get();
    }

    protected abstract void updateValue(IInvocation invocation);

    private boolean register(MBeanServer server, ObjectName name) {
        try {
            server.registerMBean(this, name);
            return true;
        } catch (InstanceAlreadyExistsException e) {
            return false;
        } catch (Exception e) {
            throw new InstrumentationException(e);
        }
    }

    private void unregister(MBeanServer server, ObjectName name) {
        try {
            if (server.isRegistered(name))
                server.unregisterMBean(name);
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }

    private interface IMessages {
        @DefaultMessage("Interceptor ''{0}'' is started.")
        ILocalizedMessage started(ObjectName name);

        @DefaultMessage("Interceptor ''{0}'' is stopped.")
        ILocalizedMessage stopped(ObjectName name);
    }
}
