/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.Iterator;

import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.common.utils.Assert;

/**
 * The {@link IncidentIterable} is an iterable over incidents.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class IncidentIterable implements Iterable<IIncident> {
    private final Iterable<IIncident> field;

    public IncidentIterable(Iterable<IIncident> field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public Iterator<IIncident> iterator() {
        return new IncidentIterator(field.iterator());
    }

    private static class IncidentIterator implements Iterator<IIncident> {
        private final Iterator<IIncident> it;
        private IIncident current;

        public IncidentIterator(Iterator<IIncident> it) {
            Assert.notNull(it);

            this.it = it;
            this.current = nextElement();
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public IIncident next() {
            Assert.checkState(current != null);

            IIncident node = current;
            current = nextElement();
            return node;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }

        private IIncident nextElement() {
            for (; it.hasNext(); ) {
                IIncident next = it.next();
                if (((ComponentNode) next.getComponent()).isAccessAlowed())
                    return next;
            }

            return null;
        }
    }
}