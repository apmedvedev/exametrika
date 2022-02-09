/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.ObservableWeakHashMap;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.SimpleIntDeque;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.IInvokeDispatcher;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link InterceptorManager} represents an implementation of {@link IInterceptorManager} and {@link IInvokeDispatcher} for loadtime and runtime
 * instrumentation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InterceptorManager implements IInterceptorManager, IInvokeDispatcher, IJoinPointProvider,
        ObservableWeakHashMap.IStaleEntryObserver<Map<String, List>> {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(InterceptorManager.class);
    private Map<ClassLoader, Map<String, List>> entriesMap = new ObservableWeakHashMap<ClassLoader, Map<String, List>>(this);
    private volatile Entries entries = new Entries();
    private SimpleIntDeque freeSlots = new SimpleIntDeque();
    private final Map<Pair<String, String>, List<JoinPointEntry>> joinPointsMap = new HashMap<Pair<String, String>, List<JoinPointEntry>>();
    private final Map<Pointcut, JoinPointEntry> singletonJoinPoints = new HashMap<Pointcut, JoinPointEntry>();
    private final boolean lazyInterceptorStart;
    private int joinPointCount;
    private volatile IJoinPointFilter joinPointFilter;
    private volatile int maxJoinPointCount = Integer.MAX_VALUE;

    public InterceptorManager() {
        lazyInterceptorStart = false;
    }

    public InterceptorManager(boolean lazyInterceptorStart) {
        this.lazyInterceptorStart = lazyInterceptorStart;
    }

    public void setJoinPointFilter(IJoinPointFilter joinPointFilter) {
        this.joinPointFilter = joinPointFilter;
    }

    public synchronized void setMaxJoinPointCount(int value) {
        this.maxJoinPointCount = value;
        if (entries.count < value)
            entries.exceeded = false;
    }

    @Override
    public synchronized JoinPointInfo allocate(ClassLoader classLoader, IJoinPoint joinPoint) {
        Assert.notNull(joinPoint);

        List<Entry> entryList = null;
        if (!joinPoint.getPointcut().isSingleton()) {
            Map<String, List> classesMap = entriesMap.get(classLoader);
            if (classesMap == null) {
                classesMap = new LinkedHashMap<String, List>();
                entriesMap.put(classLoader, classesMap);
            }
            entryList = classesMap.get(joinPoint.getClassName());
            if (entryList == null) {
                entryList = new ArrayList<Entry>();
                classesMap.put(joinPoint.getClassName(), entryList);
            }
        } else {
            JoinPointEntry entry = singletonJoinPoints.get(joinPoint.getPointcut());
            if (entry != null)
                return new JoinPointInfo(entry.index, entry.version);

            joinPoint = new JoinPoint(joinPoint.getKind(), 0, 0, joinPoint.getPointcut().getName(), "", "", 0, joinPoint.getPointcut(),
                    null, null, null, null, null, 0);
        }

        int index;
        if (!freeSlots.isEmpty())
            index = freeSlots.removeFirst();
        else if (entries.count < maxJoinPointCount)
            index = entries.count++;
        else {
            if (!entries.exceeded) {
                entries.exceeded = true;

                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.maxJoinPointCountExceeded(maxJoinPointCount));
            }

            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.maxJoinPointCountExceeded(joinPoint));

            return null;
        }

        if (index >= entries.elements.length)
            growArray();

        Entry entry = entries.elements[index];
        if (entryList != null)
            entryList.add(entry);

        try {
            if (joinPoint.getPointcut().getInterceptor() instanceof DynamicInterceptorConfiguration) {
                DynamicInterceptorConfiguration configuration = (DynamicInterceptorConfiguration) joinPoint.getPointcut().getInterceptor();
                entry.interceptor = configuration.createInterceptor();

                if (!lazyInterceptorStart) {
                    entry.interceptor.start(joinPoint);
                    entry.started = true;
                } else
                    entry.started = false;
            } else
                entry.started = false;

            entry.joinPoint = joinPoint;
            entry.version = entry.version + 1;
            addJoinPoint(index, entry.version, joinPoint);
        } catch (Exception e) {
            throw new InstrumentationException(e);
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.allocated(index, joinPoint));

        joinPointCount++;

        if (joinPoint.getPointcut().isSingleton())
            singletonJoinPoints.put(joinPoint.getPointcut(), new JoinPointEntry(index, entry.version, joinPoint));

        return new JoinPointInfo(index, entry.version);
    }

    @Override
    public synchronized void free(ClassLoader classLoader, String className) {
        Assert.notNull(className);

        Map<String, List> classesMap = entriesMap.get(classLoader);
        if (classesMap == null)
            return;
        List<Entry> entries = classesMap.remove(className);
        if (classesMap.isEmpty())
            entriesMap.remove(classLoader);

        if (entries != null) {
            for (Entry entry : entries) {
                entry.version = entry.version + 1;

                if (entry.started) {
                    entry.interceptor.stop(false);
                    entry.started = false;
                }
                entry.interceptor = null;

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.freed(entry.index, entry.joinPoint));

                removeJoinPoint(entry.joinPoint);
                entry.joinPoint = null;
                freeSlots.addLast(entry.index);
                joinPointCount--;
            }
        }
    }

    @Override
    public synchronized void freeAll() {
        entries = new Entries();

        for (Map.Entry<ClassLoader, Map<String, List>> classLoaderEntry : entriesMap.entrySet()) {
            for (Map.Entry<String, List> classEntry : classLoaderEntry.getValue().entrySet()) {
                for (Entry entry : (List<Entry>) classEntry.getValue()) {
                    if (entry.started) {
                        entry.interceptor.stop(true);
                        entry.started = false;
                    }

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.freed(entry.index, entry.joinPoint));
                }
            }
        }

        entriesMap.clear();
        freeSlots.clear();
        joinPointsMap.clear();
        joinPointCount = 0;
        singletonJoinPoints.clear();
    }

    @Override
    public synchronized void freeUnloaded() {
        entriesMap.size();
    }

    @Override
    public boolean invoke(int interceptorIndex, int version, IInvocation invocation) {
        Entries entries = this.entries;

        if (interceptorIndex < entries.count) {
            Entry entry = entries.elements[interceptorIndex];

            int entryVersion = entry.version;
            IDynamicInterceptor interceptor = entry.interceptor;
            if (version == entryVersion && interceptor != null) {
                try {
                    if (!entry.started)
                        startInterceptor(entry);

                    return interceptor.intercept(invocation);
                } catch (Throwable e) {
                    logger.log(LogLevel.ERROR, e);
                }
            }
        }

        return false;
    }

    @Override
    public synchronized int getJoinPointCount() {
        return joinPointCount;
    }

    @Override
    public IJoinPoint findJoinPoint(int index, int version) {
        Entries entries = this.entries;

        if (index < entries.count) {
            Entry entry = entries.elements[index];

            int entryVersion = entry.version;
            IJoinPoint joinPoint = entry.joinPoint;
            if (version == -1 || version == entryVersion)
                return joinPoint;
        }

        return null;
    }

    @Override
    public List<JoinPointEntry> findJoinPoints(String className, String methodName, Class interceptorClass) {
        List<JoinPointEntry> joinPoints = joinPointsMap.get(new Pair(className, methodName));
        if (joinPoints == null)
            return Collections.emptyList();

        List<JoinPointEntry> list = new ArrayList<JoinPointEntry>();
        for (JoinPointEntry entry : joinPoints) {
            if (joinPointFilter != null && !joinPointFilter.match(entry.joinPoint))
                continue;

            if (((StaticInterceptorConfiguration) entry.joinPoint.getPointcut().getInterceptor()).getInterceptorClass() == interceptorClass)
                list.add(entry);
        }
        return Immutables.wrap(list);
    }

    private synchronized void startInterceptor(Entry entry) {
        if (entry.started || entry.interceptor == null)
            return;

        entry.started = true;
        entry.interceptor.start(entry.joinPoint);
    }

    @Override
    public synchronized void onExpunge(Map<String, List> value) {
        for (Map.Entry<String, List> classEntry : value.entrySet()) {
            for (Entry entry : (List<Entry>) classEntry.getValue()) {
                entry.version = entry.version + 1;
                if (entry.started) {
                    entry.interceptor.stop(false);
                    entry.started = false;
                }
                entry.interceptor = null;

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.expunged(entry.joinPoint));

                removeJoinPoint(entry.joinPoint);
                entry.joinPoint = null;
                freeSlots.addLast(entry.index);
                joinPointCount--;
            }
        }
    }

    private void addJoinPoint(int index, int version, IJoinPoint joinPoint) {
        Pair<String, String> pair = new Pair(joinPoint.getClassName(), joinPoint.getMethodName());
        List<JoinPointEntry> list = joinPointsMap.get(pair);
        if (list == null) {
            list = new ArrayList<JoinPointEntry>();
            joinPointsMap.put(pair, list);
        }

        list.add(new JoinPointEntry(index, version, joinPoint));
    }

    private void removeJoinPoint(IJoinPoint joinPoint) {
        Pair<String, String> pair = new Pair(joinPoint.getClassName(), joinPoint.getMethodName());
        List<JoinPointEntry> list = joinPointsMap.get(pair);
        if (list != null) {
            for (Iterator<JoinPointEntry> it = list.iterator(); it.hasNext(); ) {
                JoinPointEntry entry = it.next();
                if (entry.joinPoint == joinPoint) {
                    it.remove();
                    break;
                }
            }
            if (list.isEmpty())
                joinPointsMap.remove(pair);
        }
    }

    private void growArray() {
        int oldCapacity = entries.elements.length;
        int newCapacity = (oldCapacity * 3) / 2 + 1;

        Entry[] elements = Arrays.copyOf(this.entries.elements, newCapacity);
        for (int i = oldCapacity; i < newCapacity; i++)
            elements[i] = new Entry(i);

        Entries entries = new Entries();
        entries.elements = elements;
        entries.count = this.entries.count;
        entries.exceeded = this.entries.exceeded;

        this.entries = entries;
    }

    private static class Entries {
        private Entry[] elements = new Entry[0];
        private volatile int count;
        private volatile boolean exceeded;
    }

    private static class Entry {
        private final int index;
        private volatile int version;
        private volatile boolean started;
        private volatile IDynamicInterceptor interceptor;
        private IJoinPoint joinPoint;

        public Entry(int index) {
            this.index = index;
        }
    }

    private interface IMessages {
        @DefaultMessage("Join point ''{0}:{1}'' is allocated.")
        ILocalizedMessage allocated(int index, IJoinPoint joinPoint);

        @DefaultMessage("Could not allocate new join point ''{0}''. Maximum number of join points has been reached.")
        ILocalizedMessage maxJoinPointCountExceeded(IJoinPoint joinPoint);

        @DefaultMessage("Can not allocate new join points. Maximum number of join points has been reached: {0}.")
        ILocalizedMessage maxJoinPointCountExceeded(int count);

        @DefaultMessage("Join point ''{0}:{1}'' is freed.")
        ILocalizedMessage freed(int index, IJoinPoint joinPoint);

        @DefaultMessage("Join point ''{0}'' is expunged.")
        ILocalizedMessage expunged(IJoinPoint joinPoint);
    }
}
