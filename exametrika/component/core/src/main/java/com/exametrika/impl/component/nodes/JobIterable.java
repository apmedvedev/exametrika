/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.Iterator;

import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.common.utils.Assert;

/**
 * The {@link JobIterable} is an iterable over jobs.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JobIterable implements Iterable<IJob> {
    private final Iterable<IJob> field;
    private final ComponentNode component;

    public JobIterable(Iterable<IJob> field, ComponentNode component) {
        Assert.notNull(field);
        Assert.notNull(component);

        this.field = field;
        this.component = component;
    }

    @Override
    public Iterator<IJob> iterator() {
        return new IncidentIterator(field.iterator());
    }

    private class IncidentIterator implements Iterator<IJob> {
        private final Iterator<IJob> it;
        private IJob current;

        public IncidentIterator(Iterator<IJob> it) {
            Assert.notNull(it);

            this.it = it;
            this.current = nextElement();
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public IJob next() {
            Assert.checkState(current != null);

            IJob node = current;
            current = nextElement();
            return node;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }

        private IJob nextElement() {
            for (; it.hasNext(); ) {
                IJob next = it.next();
                return new JobProxy(next, component);
            }

            return null;
        }
    }
}