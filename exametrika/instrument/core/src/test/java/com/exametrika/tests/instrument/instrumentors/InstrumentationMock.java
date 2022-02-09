/**
 * Copyright 2011 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

public class InstrumentationMock implements Instrumentation {
    public List<Class> loadedClasses = new ArrayList<Class>();
    public List<Class> retransformedClasses = new ArrayList<Class>();
    public boolean retransformSupported = true;
    public Set<ClassFileTransformer> transformers = new HashSet<ClassFileTransformer>();

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        transformers.add(transformer);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        return transformers.remove(transformer);
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return retransformSupported;
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        for (Class clazz : classes)
            retransformedClasses.add(clazz);
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return false;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
            UnmodifiableClassException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return true;
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return loadedClasses.toArray(new Class[loadedClasses.size()]);
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return null;
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return 0;
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return false;
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        throw new UnsupportedOperationException();
    }

    /**@Override
    public void redefineModule(Module module, Set<Module> extraReads, Map<String, Set<Module>> extraExports, Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses, Map<Class<?>, List<Class<?>>> extraProvides) {

    }

    @Override
    public boolean isModifiableModule(Module module) {
        return false;
    }*/
}
