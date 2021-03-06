package java.lang.instrument;

import java.util.jar.JarFile;

public interface Instrumentation {
    void addTransformer(ClassFileTransformer transformer, boolean canRetransform);

    void addTransformer(ClassFileTransformer transformer);

    boolean removeTransformer(ClassFileTransformer transformer);

    boolean isRetransformClassesSupported();

    void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

    boolean isRedefineClassesSupported();

    void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
        UnmodifiableClassException;

    boolean isModifiableClass(Class<?> theClass);

    Class[] getAllLoadedClasses();

    Class[] getInitiatedClasses(ClassLoader loader);

    long getObjectSize(Object objectToSize);

    void appendToBootstrapClassLoaderSearch(JarFile jarfile);

    void appendToSystemClassLoaderSearch(JarFile jarfile);

    boolean isNativeMethodPrefixSupported();

    void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix);
}
