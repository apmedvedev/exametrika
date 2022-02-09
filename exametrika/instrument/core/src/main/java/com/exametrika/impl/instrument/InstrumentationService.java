/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.IClassTransformer;
import com.exametrika.api.instrument.IInstrumentationMXBean;
import com.exametrika.api.instrument.IInstrumentationService;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.instrument.IReentrancyListener;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.instrument.boot.Interceptors;


/**
 * The {@link InstrumentationService} represents an instrumentation service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstrumentationService implements IInstrumentationService, IService, IServiceProvider, IInstrumentationMXBean {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(InstrumentationService.class);
    private final InterceptorManager interceptorManager = new InterceptorManager(true);
    private final List<IJoinPointFilter> joinPointFilters = new ArrayList<IJoinPointFilter>();
    private Instrumentation instrumentation;
    private volatile ClassTransformer classTransformer;
    private boolean started;
    private IReentrancyListener reentrancyListener;

    @Override
    public IJoinPointProvider getJoinPointProvider() {
        return interceptorManager;
    }

    @Override
    public IClassTransformer getClassTransformer() {
        return classTransformer;
    }

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register(IInstrumentationService.NAME, this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
    }

    @Override
    public synchronized void start(IServiceRegistry registry) {
        Assert.checkState(classTransformer == null);

        instrumentation = registry.findParameter("instrumentation");
        if (instrumentation == null)
            return;

        //Managements.register(MBEAN_NAME, this);

        Map<String, String> agentArgs = registry.findParameter("agentArgs");
        IJoinPointFilter joinPointFilter = new CompositeJoinPointFilter(joinPointFilters);
        interceptorManager.setJoinPointFilter(joinPointFilter);

        classTransformer = new ClassTransformer(instrumentation, interceptorManager, joinPointFilter,
                reentrancyListener, agentArgs);
        Interceptors.setInvokeDispatcher(interceptorManager);

        instrumentation.addTransformer(classTransformer, true);

        started = true;
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());
    }

    @Override
    public synchronized void stop(boolean fromShutdownHook) {
        if (classTransformer == null)
            return;

        //Managements.unregister(MBEAN_NAME);

        instrumentation.removeTransformer(classTransformer);
        Interceptors.setInvokeDispatcher(null);
        classTransformer.close(fromShutdownHook);
        classTransformer = null;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        if (classTransformer == null)
            return;

        InstrumentationConfiguration configuration = context.get(InstrumentationConfiguration.SCHEMA);
        classTransformer.setConfiguration(configuration);

        if (configuration != null)
            interceptorManager.setMaxJoinPointCount(configuration.getMaxJoinPointCount());

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationUpdated());
    }

    @Override
    public void onTimer(long currentTime) {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            classTransformer.onTimer();
    }

    @Override
    public int getTotalTransformedClassesCount() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getTotalTransformedClassesCount();
        else
            return 0;
    }

    @Override
    public int getTotalSkippedClassesCount() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getTotalSkippedClassesCount();
        else
            return 0;
    }

    @Override
    public int getTotalTransformationErrorsCount() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getTotalTransformationErrorsCount();
        else
            return 0;
    }

    @Override
    public long getTotalTransformationTime() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getTotalTransformationTime();
        else
            return 0;
    }

    @Override
    public int getTotalTransformedClassesInitialSize() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getTotalTransformedClassesInitialSize();
        else
            return 0;
    }

    @Override
    public int getTotalTransformedClassesResultingSize() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getTotalTransformedClassesResultingSize();
        else
            return 0;
    }

    @Override
    public int getJoinPointCount() {
        ClassTransformer classTransformer = this.classTransformer;
        if (classTransformer != null)
            return classTransformer.getJoinPointCount();
        else
            return 0;
    }

    @Override
    public synchronized void addJoinPointFilter(IJoinPointFilter filter) {
        Assert.notNull(filter);
        Assert.checkState(!started);

        joinPointFilters.add(filter);
    }

    @Override
    public void setReentrancyListener(IReentrancyListener listener) {
        this.reentrancyListener = listener;
    }

    private interface IMessages {
        @DefaultMessage("Instrumentation service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Instrumentation service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of instrumentation service is updated.")
        ILocalizedMessage configurationUpdated();
    }
}
