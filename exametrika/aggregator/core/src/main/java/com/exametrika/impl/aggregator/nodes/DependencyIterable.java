/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import java.util.Iterator;

import com.exametrika.api.aggregator.nodes.Dependency;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField.IReferenceIterator;
import com.exametrika.common.utils.Assert;

/**
 * The {@link DependencyIterable} is an iterable over stack node dependencies.
 *
 * @param <T> dependency node type
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class DependencyIterable<T extends IAggregationNode> implements Iterable<Dependency<T>> {
    private final IReferenceField<T> field;

    public DependencyIterable(IReferenceField<T> field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public Iterator<Dependency<T>> iterator() {
        return new DependencyIterator<T>(field.iterator());
    }

    private static class DependencyIterator<T extends IAggregationNode> implements Iterator<Dependency<T>> {
        private final IReferenceIterator<T> it;

        public DependencyIterator(IReferenceField.IReferenceIterator<T> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Dependency<T> next() {
            T node = it.next();
            return new Dependency<T>(node, (it.getFlags() & StackNode.TOTAL_REFERENCE_FLAG) != 0);
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }
}