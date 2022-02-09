/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.ObservableWeakHashMap;
import com.exametrika.common.utils.ObservableWeakHashMap.IStaleEntryObserver;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.model.ISerializeNameDictionary;


/**
 * The {@link SerializeNameDictionary} is a dictionary of names used for serialization optimization.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SerializeNameDictionary implements ISerializeNameDictionary {
    public static final UUID EXTENTION_ID = UUID.fromString("37742164-8c14-4769-8784-68c2861ce0cc");
    public static final byte SCOPE_NAME = 0x1;
    public static final byte METRIC_NAME = 0x2;
    public static final byte CALLPATH_NAME = 0x3;

    private final INameDictionary nameDictionary;
    private final ObservableWeakHashMap<IScopeName, NameInfo> scopes = new ObservableWeakHashMap<IScopeName, NameInfo>(new NameObserver(SCOPE_NAME));
    private final ObservableWeakHashMap<IMetricName, NameInfo> metrics = new ObservableWeakHashMap<IMetricName, NameInfo>(new NameObserver(METRIC_NAME));
    private final ObservableWeakHashMap<ICallPath, NameInfo> callPaths = new ObservableWeakHashMap<ICallPath, NameInfo>(new NameObserver(CALLPATH_NAME));
    private List<SerializeNameId> removedNames = new LinkedList<SerializeNameId>();

    private long nextScopeId = 0;
    private long nextMetricId = 0;
    private long nextCallPathId = 0;

    public static class SerializeNameId {
        public final byte type;
        public final long id;

        public SerializeNameId(byte type, long id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof SerializeNameId))
                return false;

            SerializeNameId nameId = (SerializeNameId) o;
            return type == nameId.type && id == nameId.id;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, id);
        }

        @Override
        public String toString() {
            String typeName;
            switch (type) {
                case SCOPE_NAME:
                    typeName = "scope";
                    break;
                case METRIC_NAME:
                    typeName = "metric";
                    break;
                case CALLPATH_NAME:
                    typeName = "callpath";
                    break;
                default:
                    return Assert.error();
            }

            return typeName + ":" + id;
        }
    }

    public SerializeNameDictionary() {
        nameDictionary = null;
    }

    public SerializeNameDictionary(INameDictionary nameDictionary) {
        Assert.notNull(nameDictionary);

        this.nameDictionary = nameDictionary;
    }

    public final List<SerializeNameId> takeRemovedNames() {
        if (removedNames.isEmpty())
            return Collections.emptyList();

        List<SerializeNameId> removedNames = this.removedNames;
        this.removedNames = new LinkedList<SerializeNameId>();
        return removedNames;
    }

    public final void reset() {
        scopes.clear();
        metrics.clear();
        callPaths.clear();
        nextScopeId = 0;
        nextMetricId = 0;
        nextCallPathId = 0;
    }

    @Override
    public boolean convertIdsToNames() {
        return nameDictionary != null;
    }

    @Override
    public IName getName(long persistentNameId) {
        return nameDictionary.getName(persistentNameId);
    }

    @Override
    public final long getScopeId(IScopeName name) {
        NameInfo info = scopes.get(name);
        if (info != null)
            return info.id;
        else
            return -1;
    }

    @Override
    public final long putScope(IScopeName name) {
        NameInfo info = scopes.get(name);
        if (info == null) {
            info = new NameInfo();
            info.id = nextScopeId++;

            scopes.put(name, info);
        }

        return info.id;
    }

    @Override
    public final long getMetricId(IMetricName name) {
        NameInfo info = metrics.get(name);
        if (info != null)
            return info.id;
        else
            return -1;
    }

    @Override
    public final long putMetric(IMetricName name) {
        NameInfo info = metrics.get(name);
        if (info == null) {
            info = new NameInfo();
            info.id = nextMetricId++;

            metrics.put(name, info);
        }

        return info.id;
    }

    @Override
    public final long getCallPathId(ICallPath name) {
        NameInfo info = callPaths.get(name);
        if (info != null)
            return info.id;
        else
            return -1;
    }

    @Override
    public final long putCallPath(ICallPath name) {
        NameInfo info = callPaths.get(name);
        if (info == null) {
            info = new NameInfo();
            info.id = nextCallPathId++;

            callPaths.put(name, info);
        }

        return info.id;
    }

    private class NameObserver implements IStaleEntryObserver<NameInfo> {
        private final byte type;

        public NameObserver(byte type) {
            Assert.notNull(type);

            this.type = type;
        }

        @Override
        public void onExpunge(NameInfo value) {
            removedNames.add(new SerializeNameId(type, value.id));
        }
    }

    private static class NameInfo {
        public long id;
    }
}
