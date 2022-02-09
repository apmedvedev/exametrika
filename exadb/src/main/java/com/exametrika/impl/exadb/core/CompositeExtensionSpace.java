/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IExtensionSpace;


/**
 * The {@link CompositeExtensionSpace} is a composite extension space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeExtensionSpace implements IExtensionSpace {
    private final List<IExtensionSpace> extensionSpaces;
    private final int priority;

    public CompositeExtensionSpace(List<IExtensionSpace> extensionSpaces, int priority) {
        Assert.notNull(extensionSpaces);

        this.extensionSpaces = extensionSpaces;
        this.priority = priority;
    }

    @Override
    public List<String> getFiles() {
        List<String> files = new ArrayList<String>();
        for (IExtensionSpace extensionSpace : extensionSpaces)
            files.addAll(extensionSpace.getFiles());

        return files;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void create() {
        for (IExtensionSpace extensionSpace : extensionSpaces)
            extensionSpace.create();
    }

    @Override
    public void open() {
        for (IExtensionSpace extensionSpace : extensionSpaces)
            extensionSpace.open();
    }
}
