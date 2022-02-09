/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.ICacheable;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;


/**
 * The {@link MetricName} represents a metric name.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MetricName implements IMetricName, ICacheable {
    private static final int METRIC_NAME_CACHE_SIZE = Memory.getShallowSize(MetricName.class);
    private static final MetricName root = new MetricName("", Collections.<String>emptyList(), 41, 0);
    private static Map<String, Entry> names = new HashMap<String, Entry>();
    private static ReferenceQueue<MetricName> queue = new ReferenceQueue<MetricName>();

    private final String value;
    private final List<String> segments;
    private final int hashCode;
    private final long id;
    private final int cacheSize;

    public static MetricName root() {
        return root;
    }

    public static MetricName get(String name) {
        Assert.notNull(name);

        List<String> segments = new ArrayList<String>();
        parse(name, segments);

        return get(segments, name);
    }

    public static MetricName get(List<String> segments) {
        return get(segments, toString(segments));
    }

    public static synchronized void reset() {
        names = new HashMap<String, Entry>();
        queue = new ReferenceQueue<MetricName>();
        NameIds.reset();
    }

    public long getId() {
        return id;
    }

    @Override
    public int getCacheSize() {
        return cacheSize;
    }

    @Override
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    @Override
    public List<String> getSegments() {
        return segments;
    }

    @Override
    public String getLastSegment() {
        if (segments.isEmpty())
            return "";
        else
            return segments.get(segments.size() - 1);
    }

    @Override
    public boolean startsWith(IMetricName prefix) {
        if (!(prefix instanceof MetricName))
            return false;

        MetricName name = (MetricName) prefix;

        if (name.segments.size() > segments.size())
            return false;

        for (int i = 0; i < name.segments.size(); i++) {
            if (!segments.get(i).equals(name.segments.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MetricName))
            return false;

        MetricName name = (MetricName) o;
        return id == name.id;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return value;
    }

    private static String toString(List<String> segments) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int k = 0; k < segments.size(); k++) {
            if (first)
                first = false;
            else
                builder.append('.');

            builder.append(Names.escape(segments.get(k)));
        }

        return builder.toString();
    }

    private MetricName(String value, List<String> segments, int hashCode, long id) {
        for (String segment : segments)
            Assert.isTrue(!segment.isEmpty());

        this.value = value;
        this.segments = segments;
        this.hashCode = hashCode;
        this.id = id;
        this.cacheSize = computeCacheSize();
    }

    private int computeCacheSize() {
        int cacheSize = METRIC_NAME_CACHE_SIZE + CacheSizes.getStringCacheSize(value) + CacheSizes.IMMUTABLES_LIST_CACHE_SIZE +
                CacheSizes.getArrayListCacheSize(segments);

        for (String value : segments)
            cacheSize += CacheSizes.getStringCacheSize(value);

        return cacheSize;
    }

    private static synchronized MetricName get(List<String> segments, String name) {
        Assert.notNull(name);
        if (name.isEmpty()) {
            Assert.isTrue(segments == null || segments.isEmpty());
            return root;
        }

        expungeStaleReferences();

        Entry entry = names.get(name);
        if (entry != null) {
            MetricName value = entry.get();
            if (value != null)
                return value;

            entry.removed = true;
        }

        if (segments == null) {
            segments = new ArrayList<String>();
            parse(name, segments);
        }

        MetricName value = new MetricName(name, Immutables.wrap(segments), hash(segments), NameIds.getNextId());
        names.put(name, new Entry(name, value, queue));

        return value;
    }

    private static int hash(List<String> segments) {
        return 41 * segments.hashCode();
    }

    private static void expungeStaleReferences() {
        while (true) {
            Entry entry = (Entry) queue.poll();
            if (entry == null)
                break;

            if (entry.removed)
                continue;

            names.remove(entry.key);
        }
    }

    private static void parse(String name, List<String> segments) {
        if (name.isEmpty())
            return;

        int start = 0;
        int i = 0;
        for (; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') {
                if (i < name.length() - 1 && name.charAt(i + 1) == '.') {
                    i++;
                    continue;
                }

                if (i > start)
                    segments.add(name.substring(start, i));

                start = i + 1;
            }
        }

        if (i > start)
            segments.add(name.substring(start, i));
    }

    private static class Entry extends WeakReference<MetricName> {
        private final String key;
        private boolean removed;

        public Entry(String key, MetricName referent, ReferenceQueue<MetricName> queue) {
            super(referent, queue);

            Assert.notNull(key);

            this.key = key;
        }
    }
}
