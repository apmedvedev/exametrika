/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.Iterator;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField.IReferenceIterator;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.schema.ComponentRootNodeSchema;

/**
 * The {@link ComponentIterable} is an iterable over components.
 *
 * @param <T> version node type
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ComponentIterable<T extends IComponent> implements Iterable<T> {
    private final IReferenceField<T> field;

    public ComponentIterable(IReferenceField<T> field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public Iterator<T> iterator() {
        ComponentRootNodeSchema rootSchema = (ComponentRootNodeSchema) ((IObjectSpace) field.getNode().getSpace()).getSchema().getRootNode();
        return new ComponentVersionIterator<T>(field.iterator(), rootSchema.isDeletedComponentFiltered());
    }

    private static class ComponentVersionIterator<T extends IComponent> implements Iterator<T> {
        private final IReferenceIterator<T> it;
        private T current;
        private final boolean deletedComponentFiltered;

        public ComponentVersionIterator(IReferenceField.IReferenceIterator<T> it, boolean deletedComponentFiltered) {
            Assert.notNull(it);

            this.it = it;
            this.deletedComponentFiltered = deletedComponentFiltered;
            this.current = nextElement();
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            Assert.checkState(current != null);

            T node = current;
            current = nextElement();
            return node;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }

        private T nextElement() {
            for (; it.hasNext(); ) {
                T next = it.next();
                int refDeletionCount = it.getFlags();
                ComponentVersionNode version = (ComponentVersionNode) next.get();
                if (((ComponentNode) next).isAccessAlowed() && version != null &&
                        (!deletedComponentFiltered || (!version.isDeleted() && refDeletionCount == version.getDeletionCount())))
                    return next;
            }

            return null;
        }
    }
}