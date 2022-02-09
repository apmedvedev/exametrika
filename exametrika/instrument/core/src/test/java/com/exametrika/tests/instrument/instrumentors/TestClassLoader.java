/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.instrument.StaticClassTransformer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * The {@link TestClassLoader} represents a test class loader.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TestClassLoader extends ClassLoader {
    private final List<String> basePackages;
    private final StaticClassTransformer classTransformer;
    private final ClassLoader base;

    public TestClassLoader(ClassLoader parent, ClassLoader base, String basePackage, StaticClassTransformer classTransformer) {
        this(parent, base, Collections.singletonList(basePackage), classTransformer);
    }

    public TestClassLoader(String basePackage, StaticClassTransformer classTransformer) {
        this(ClassLoader.getSystemClassLoader(), ClassLoader.getSystemClassLoader(), Collections.singletonList(basePackage), classTransformer);
    }

    public TestClassLoader(List<String> basePackages, StaticClassTransformer classTransformer, boolean dummy) {
        this(ClassLoader.getSystemClassLoader(), ClassLoader.getSystemClassLoader(), basePackages, classTransformer);
    }

    public TestClassLoader(ClassLoader parent, ClassLoader base, List<String> basePackages, StaticClassTransformer classTransformer) {
        super(parent);

        this.base = base;
        this.basePackages = basePackages;
        this.classTransformer = classTransformer;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null && canTransformed(name))
            c = doFindClass(name);
        if (c == null)
            return super.loadClass(name, resolve);

        if (resolve)
            resolveClass(c);

        return c;
    }

    private boolean canTransformed(String className) {
        for (String basePackage : basePackages) {
            if (className.startsWith(basePackage))
                return true;
        }

        return false;
    }

    private Class doFindClass(String name) throws ClassNotFoundException {
        try {
            ByteArray buffer = loadClassData(name);
            byte[] classFileBuffer = new byte[buffer.getLength()];
            System.arraycopy(buffer.getBuffer(), buffer.getOffset(), classFileBuffer, 0, buffer.getLength());

            byte[] transformedClassFileBuffer = null;
            if (classTransformer != null)
                transformedClassFileBuffer = classTransformer.transform(classFileBuffer);
            if (transformedClassFileBuffer == null)
                transformedClassFileBuffer = classFileBuffer;

            return defineClass(name, transformedClassFileBuffer, 0, transformedClassFileBuffer.length);
        } catch (Exception e) {
            throw new ClassNotFoundException("Class " + name + " not found.", e);
        }
    }

    private ByteArray loadClassData(String name) throws IOException {
        String path = convertToPath(name);
        ByteOutputStream stream = new ByteOutputStream();
        IOs.copy(base.getResourceAsStream(path), stream);
        return new ByteArray(stream.getBuffer(), 0, stream.getLength());
    }

    private String convertToPath(String name) {
        return name.replace('.', '/') + ".class";
    }
}
