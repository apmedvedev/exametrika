/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;

/**
 * The {@link IdCache} is a cache of identifiers.
 *
 * @param <T> identifier type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IdCache<T> {
    private static final int MAX_ELEMENTS = 10000;
    private Map<T, Id> idsMap = new HashMap<T, Id>();
    private final SimpleList<Id> ids = new SimpleList<Id>();

    public synchronized UUID get(T name) {
        Assert.notNull(name);

        Id id = idsMap.get(name);
        if (id != null) {
            id.element.remove();
            id.element.reset();
        } else {
            id = new Id<T>(name, UUID.randomUUID());
            idsMap.put(name, id);
            if (idsMap.size() > MAX_ELEMENTS) {
                Id removed = ids.getFirst().getValue();
                removed.element.remove();
                idsMap.remove(removed.name);
            }
        }

        ids.addLast(id.element);
        return id.id;
    }

    private static class Id<T> {
        private final T name;
        private final UUID id;
        private final Element<Id> element = new Element<Id>(this);

        public Id(T name, UUID id) {
            Assert.notNull(name);
            Assert.notNull(id);

            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name.toString();
        }
    }
}