/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * The {@link ConnectionClassLoader} represents a class loader for connections and connection factories.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ConnectionClassLoader extends URLClassLoader {
    private final Set<String> classes;

    public ConnectionClassLoader(URL[] urls, Set<String> classes, ClassLoader parent) {
        super(urls, parent);

        if (classes == null)
            throw new IllegalArgumentException();

        this.classes = classes;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null && classes.contains(name))
            c = doFindClass(name);
        if (c == null)
            return super.loadClass(name, resolve);

        if (resolve)
            resolveClass(c);

        return c;
    }

    private Class doFindClass(String name) throws ClassNotFoundException {
        try {
            String path = convertToPath(name);
            InputStream stream = getParent().getResourceAsStream(path);
            if (stream == null)
                throw new ClassNotFoundException("Class " + name + " not found.");

            byte[] buffer = loadData(stream);

            return defineClass(name, buffer, 0, buffer.length);
        } catch (Exception e) {
            throw new ClassNotFoundException("Class " + name + " not found.", e);
        }
    }

    private byte[] loadData(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new BufferedInputStream(stream);

        try {
            byte[] buffer = new byte[8192];

            while (true) {
                int length = in.read(buffer);
                if (length == -1)
                    break;

                out.write(buffer, 0, length);
            }
        } finally {
            in.close();
        }

        return out.toByteArray();
    }

    private String convertToPath(String name) {
        return name.replace('.', '/') + ".class";
    }
}
