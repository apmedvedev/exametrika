/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.NodeObject;


/**
 * The {@link NodeObjectCache} is a weak cache of node objects.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class NodeObjectCache {
    private final TLongObjectMap<Entry> objects = new TLongObjectHashMap<Entry>();
    private final ReferenceQueue<NodeObject> queue = new ReferenceQueue<NodeObject>();

    public boolean isEmpty() {
        expungeStaleEntries();
        return objects.isEmpty();
    }

    public int size() {
        expungeStaleEntries();
        return objects.size();
    }

    public NodeObject get(long id) {
        expungeStaleEntries();
        Entry entry = objects.get(id);
        if (entry != null)
            return entry.get();
        else
            return null;
    }

    public void put(long id, NodeObject object) {
        expungeStaleEntries();
        Entry prev = objects.put(id, new Entry(id, object, queue));
        delete(prev, true);
    }

    public void remove(long id) {
        Entry entry = objects.remove(id);
        delete(entry, false);
        expungeStaleEntries();
    }

    public void clear() {
        objects.forEachValue(new TObjectProcedure<Entry>() {
            @Override
            public boolean execute(Entry entry) {
                delete(entry, false);
                return true;
            }
        });

        objects.clear();
        expungeStaleEntries();
    }

    private void delete(Entry entry, boolean checkUnloaded) {
        if (entry == null)
            return;

        NodeObject prevObject = entry.get();
        if (prevObject == null)
            return;

        Assert.isTrue(!checkUnloaded || !prevObject.isLoaded());
        prevObject.setStale();
    }

    private void expungeStaleEntries() {
        while (true) {
            Entry entry = (Entry) queue.poll();
            if (entry == null)
                break;

            objects.remove(entry.id);
        }
    }

    private static class Entry extends WeakReference<NodeObject> {
        private final long id;

        public Entry(long id, NodeObject referent, ReferenceQueue<NodeObject> queue) {
            super(referent, queue);

            this.id = id;
        }
    }
}
