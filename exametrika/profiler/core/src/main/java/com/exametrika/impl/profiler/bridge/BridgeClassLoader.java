/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.bridge;

import java.io.IOException;
import java.io.InputStream;

import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.boot.utils.PathClassLoader;
import com.exametrika.spi.instrument.boot.INoTransform;


/**
 * The {@link BridgeClassLoader} represents a bridge class loader.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BridgeClassLoader extends ClassLoader implements INoTransform {
    private final PathClassLoader base;

    public BridgeClassLoader(PathClassLoader base, ClassLoader parent) {
        super(parent);

        Assert.notNull(base);

        this.base = base;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null && !base.isParentClass(name))
            c = doFindClass(name);
        if (c == null)
            return super.loadClass(name, resolve);

        if (resolve)
            resolveClass(c);

        return c;
    }

    private Class doFindClass(String name) throws ClassNotFoundException {
        try {
            ByteArray buffer = loadClassData(name);
            if (buffer == null)
                return null;

            return defineClass(name, buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        } catch (Exception e) {
            throw new ClassNotFoundException("Class " + name + " not found.", e);
        }
    }

    private ByteArray loadClassData(String name) throws IOException {
        String path = convertToPath(name);
        InputStream in = base.getResourceAsStream(path);
        if (in == null)
            return null;

        ByteOutputStream stream = new ByteOutputStream();
        IOs.copy(in, stream);
        return new ByteArray(stream.getBuffer(), 0, stream.getLength());
    }

    private String convertToPath(String name) {
        return name.replace('.', '/') + ".class";
    }
}
