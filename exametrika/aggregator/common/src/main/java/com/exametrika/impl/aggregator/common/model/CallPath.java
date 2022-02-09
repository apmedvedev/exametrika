/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;


/**
 * The {@link CallPath} represents a call path.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CallPath implements ICallPath, ICacheable {
    private static final int CALLPATH_CACHE_SIZE = Memory.getShallowSize(CallPath.class);
    private static final CallPath root = new CallPath(null, MetricName.root(), 43, 0);
    private static Map<Key, Entry> callPaths = new HashMap<Key, Entry>();
    private static ReferenceQueue<CallPath> queue = new ReferenceQueue<CallPath>();

    private final CallPath parent;
    private final IMetricName segment;
    private final int hashCode;
    private final long id;

    public static CallPath root() {
        return root;
    }

    public static CallPath get(String callPath) {
        return parse(callPath);
    }

    public static CallPath get(List<? extends IMetricName> segments) {
        Assert.notNull(segments);

        CallPath parent = root;
        for (IMetricName segment : segments)
            parent = get(parent, (MetricName) segment);

        return parent;
    }

    public static synchronized CallPath get(CallPath parent, MetricName segment) {
        if (parent == null)
            parent = root;

        Assert.notNull(segment);

        checkSegment(segment);

        expungeStaleReferences();

        Key key = new Key(parent.getId(), segment.getId());

        Entry entry = callPaths.get(key);
        if (entry != null) {
            CallPath value = entry.get();
            if (value != null)
                return value;

            entry.removed = true;
        }

        CallPath value = new CallPath(parent, segment, 31 * parent.hashCode + segment.hashCode(), NameIds.getNextId());
        callPaths.put(key, new Entry(key, value, queue));

        return value;
    }

    public static synchronized void reset() {
        callPaths = new HashMap<Key, Entry>();
        queue = new ReferenceQueue<CallPath>();
        NameIds.reset();
    }

    public long getId() {
        return id;
    }

    @Override
    public int getCacheSize() {
        return CALLPATH_CACHE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this == root;
    }

    @Override
    public List<IMetricName> getSegments() {
        LinkedList<IMetricName> list = new LinkedList<IMetricName>();

        CallPath current = this;
        while (current.parent != null) {
            list.addFirst(current.segment);
            current = current.parent;
        }

        return Immutables.wrap(list);
    }

    @Override
    public IMetricName getLastSegment() {
        return segment;
    }

    @Override
    public CallPath getParent() {
        return parent;
    }

    @Override
    public CallPath getChild(IMetricName segment) {
        return get(this, (MetricName) segment);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CallPath))
            return false;

        CallPath callPath = (CallPath) o;
        return id == callPath.id;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (IMetricName segment : getSegments()) {
            if (first)
                first = false;
            else
                builder.append(SEPARATOR);

            builder.append(segment);
        }

        return builder.toString();
    }

    private CallPath(CallPath parent, IMetricName segment, int hashCode, long id) {
        this.parent = parent;
        this.segment = segment;
        this.hashCode = hashCode;
        this.id = id;
    }

    private static void expungeStaleReferences() {
        while (true) {
            Entry entry = (Entry) queue.poll();
            if (entry == null)
                break;

            if (entry.removed)
                continue;

            callPaths.remove(entry.key);
        }
    }

    private static CallPath parse(String callPath) {
        Assert.notNull(callPath);

        if (callPath.isEmpty())
            return root;

        CallPath parent = root;
        int start = 0;
        int i = 0;
        for (; i < callPath.length(); i++) {
            char ch = callPath.charAt(i);
            if (ch == SEPARATOR) {
                if (i > start)
                    parent = get(parent, MetricName.get(callPath.substring(start, i)));

                start = i + 1;
            }
        }

        if (i > start)
            return get(parent, MetricName.get(callPath.substring(start, i)));
        else
            return parent;
    }

    private static void checkSegment(IMetricName segment) {
        Assert.notNull(segment);
        Assert.isTrue(!segment.isEmpty());
        Assert.isTrue(segment.toString().indexOf(SEPARATOR) == -1);
    }

    private static class Key {
        private final long parentId;
        private final long metricId;

        public Key(long parentId, long metricId) {
            this.parentId = parentId;
            this.metricId = metricId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Key))
                return false;

            Key key = (Key) o;
            return parentId == key.parentId && metricId == key.metricId;
        }

        @Override
        public int hashCode() {
            return 31 * (int) (parentId ^ (parentId >>> 32)) + (int) (metricId ^ (metricId >>> 32));
        }
    }

    private static class Entry extends WeakReference<CallPath> {
        private final Key key;
        private boolean removed;

        public Entry(Key key, CallPath referent, ReferenceQueue<CallPath> queue) {
            super(referent, queue);

            Assert.notNull(key);

            this.key = key;
        }
    }
}
