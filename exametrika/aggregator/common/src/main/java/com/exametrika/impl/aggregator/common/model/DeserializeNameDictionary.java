/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;

import java.util.List;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary.SerializeNameId;
import com.exametrika.spi.aggregator.common.model.IDeserializeNameDictionary;
import com.exametrika.spi.aggregator.common.model.INameDictionary;


/**
 * The {@link DeserializeNameDictionary} is a dictionary of names used for deserialization optimization.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DeserializeNameDictionary implements IDeserializeNameDictionary {
    public static final UUID EXTENTION_ID = UUID.fromString("39cc8860-c0ac-40da-bd0e-7714e25c411f");
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(DeserializeNameDictionary.class);

    private final INameDictionary dictionary;
    private final IMarker marker;
    private final TLongLongMap scopeIds = new TLongLongHashMap(10, 0.5f, Long.MAX_VALUE, Long.MAX_VALUE);
    private final TLongLongMap metricIds = new TLongLongHashMap(10, 0.5f, Long.MAX_VALUE, Long.MAX_VALUE);
    private final TLongLongMap callPathIds = new TLongLongHashMap(10, 0.5f, Long.MAX_VALUE, Long.MAX_VALUE);
    private boolean modified;

    public DeserializeNameDictionary(INameDictionary dictionary, IMarker marker) {
        Assert.notNull(dictionary);

        this.dictionary = dictionary;
        this.marker = Loggers.getMarker(Integer.toString(System.identityHashCode(this)), marker);
    }

    public boolean isModified() {
        return modified;
    }

    public void clearModified() {
        modified = false;
    }

    public void reset() {
        scopeIds.clear();
        metricIds.clear();
        callPathIds.clear();
        modified = false;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.reset());
    }

    @Override
    public long putScope(long scopeSessionId, IScopeName name) {
        Assert.notNull(name);

        long id = scopeIds.get(scopeSessionId);
        if (id == scopeIds.getNoEntryValue()) {
            id = dictionary.getName(name);
            scopeIds.put(scopeSessionId, id);
            modified = true;

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.putScope(name, scopeSessionId));
        }

        return id;
    }

    @Override
    public long getScopeId(long scopeSessionId) {
        long id = scopeIds.get(scopeSessionId);
        if (id == scopeIds.getNoEntryValue()) {
            modified = true;
            Assert.error("{1}Scope with session id ''{0}'' is not found.", scopeSessionId, marker);
        }
        return id;
    }

    @Override
    public void removeScope(long scopeSessionId) {
        scopeIds.remove(scopeSessionId);

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.removeScope(scopeSessionId));
    }

    @Override
    public long putMetric(long metricSessionId, IMetricName name) {
        Assert.notNull(name);

        long id = metricIds.get(metricSessionId);
        if (id == metricIds.getNoEntryValue()) {
            id = dictionary.getName(name);
            metricIds.put(metricSessionId, id);
            modified = true;

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.putMetric(name, metricSessionId));
        }

        return id;
    }

    @Override
    public long getMetricId(long metricSessionId) {
        long id = metricIds.get(metricSessionId);
        if (id == metricIds.getNoEntryValue()) {
            modified = true;
            Assert.error("{1}Metric with session id ''{0}'' is not found.", metricSessionId, marker);
        }
        return id;
    }

    @Override
    public void removeMetric(long metricSessionId) {
        metricIds.remove(metricSessionId);

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.removeMetric(metricSessionId));
    }

    @Override
    public long putCallPath(long callPathSessionId, long parentCallPathId, long metricId) {
        long id = callPathIds.get(callPathSessionId);
        if (id == callPathIds.getNoEntryValue()) {
            id = dictionary.getCallPath(parentCallPathId, metricId);
            callPathIds.put(callPathSessionId, id);
            modified = true;

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.putCallPath(callPathSessionId, parentCallPathId, metricId));
        }

        return id;
    }

    @Override
    public long getCallPathId(long callPathSessionId) {
        long id = callPathIds.get(callPathSessionId);
        if (id == callPathIds.getNoEntryValue()) {
            modified = true;
            Assert.error("{1}CallPath with session id ''{0}'' is not found.", callPathSessionId, marker);
        }
        return id;
    }

    @Override
    public void removeCallPath(long callPathSessionId) {
        callPathIds.remove(callPathSessionId);

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.removeCallPath(callPathSessionId));
    }

    public void removeNames(List<SerializeNameId> names) {
        for (SerializeNameId name : names) {
            switch (name.type) {
                case SerializeNameDictionary.SCOPE_NAME:
                    removeScope(name.id);
                    break;
                case SerializeNameDictionary.METRIC_NAME:
                    removeMetric(name.id);
                    break;
                case SerializeNameDictionary.CALLPATH_NAME:
                    removeCallPath(name.id);
                    break;
                default:
                    Assert.error();
            }
        }
    }

    private interface IMessages {
        @DefaultMessage("Dictionary has been reset.")
        ILocalizedMessage reset();

        @DefaultMessage("Scope ''{0}'' with session id ''{1}'' has been put.")
        ILocalizedMessage putScope(IScopeName name, long scopeSessionId);

        @DefaultMessage("Scope with session id ''{0}'' has been removed.")
        ILocalizedMessage removeScope(long scopeSessionId);

        @DefaultMessage("Metric ''{0}'' with session id ''{1}'' has been put.")
        ILocalizedMessage putMetric(IMetricName name, long metricSessionId);

        @DefaultMessage("Metric with session id ''{0}'' has been removed.")
        ILocalizedMessage removeMetric(long metricSessionId);

        @DefaultMessage("CallPath ''{1}:{2}'' with session id ''{0}'' has been put.")
        ILocalizedMessage putCallPath(long callPathSessionId, long parentCallPathId, long metricId);

        @DefaultMessage("CallPath with session id ''{0}'' has been removed.")
        ILocalizedMessage removeCallPath(long callPathSessionId);
    }
}
