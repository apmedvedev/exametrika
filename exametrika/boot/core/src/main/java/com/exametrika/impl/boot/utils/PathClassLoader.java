/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The {@link PathClassLoader} represents a class loader that loads classes from specified class path, which consists of
 * local jar files and local directories. Path class loader also defines filter on classes available from parent class loader.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PathClassLoader extends ClassLoader implements Closeable {
    private static final byte[] MAGIC = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
    private final List<File> files = new ArrayList<File>();
    private final List<File> dirs = new ArrayList<File>();
    private final List<File> libraryDirs = new ArrayList<File>();
    private final List<String> availableSystemPackages;
    private List<Class> classes = new ArrayList<Class>();
    private List<JarFile> jarFiles;

    /**
     * Creates an object.
     *
     * @param classPaths              list of class path entries. Each entry can be local jar file or local directory
     * @param libraryPaths            list of paths to native libraries. Each path can be local native library or local directory
     * @param availableSystemPackages list of system packages available from parent class loader
     * @param parent                  parent class loader
     */
    public PathClassLoader(List<File> classPaths, List<File> libraryPaths, List<String> availableSystemPackages, ClassLoader parent) {
        super(parent);

        if (classPaths == null)
            throw new IllegalArgumentException();
        if (libraryPaths == null)
            throw new IllegalArgumentException();
        if (availableSystemPackages == null)
            throw new IllegalArgumentException();

        for (File file : classPaths) {
            if (file.isFile())
                files.add(file);
            else
                dirs.add(file);
        }

        for (File file : libraryPaths) {
            if (!file.exists())
                continue;

            if (!file.isFile())
                libraryDirs.add(file);
        }

        this.availableSystemPackages = availableSystemPackages;
    }

    public boolean isParentClass(String name) {
        int pos = name.lastIndexOf('.');
        if (pos == -1)
            return false;

        String classPackage = name.substring(0, pos + 1);
        for (String path : availableSystemPackages) {
            if (classPackage.startsWith(path + '.'))
                return true;
        }

        return false;
    }

    @Override
    public synchronized InputStream getResourceAsStream(String name) {
        try {
            byte[] data = loadResource(name);
            if (data != null)
                return new ByteArrayInputStream(data);

            if (isParentResource(name))
                return getParent().getResourceAsStream(name);
            else
                return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public URL getResource(String name) {
        try {
            InputStream stream = getResourceAsStream(name);
            if (stream != null)
                return new URL(null, "classpath:" + name, new StreamHandler(stream));
            else
                return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<URL>();
        List<byte[]> datas = loadResources(name);
        for (byte[] data : datas)
            urls.add(new URL(null, "classpath:" + name, new StreamHandler(new ByteArrayInputStream(data))));

        return Collections.enumeration(urls);
    }

    @Override
    public synchronized void close() throws IOException {
        clearStaticReferences();

        if (jarFiles != null) {
            for (JarFile jarFile : jarFiles)
                jarFile.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null && isParentClass(name)) {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
            }
        }

        if (c == null)
            c = doFindClass(name);

        if (resolve)
            resolveClass(c);

        return c;
    }

    @Override
    protected String findLibrary(String libname) {
        if (libname.startsWith("exa") && Utils.IS_64_BIT && !libname.endsWith("-x64"))
            libname += "-x64";

        if (!isMappedLibraryName(libname))
            libname = System.mapLibraryName(libname);
        for (File dir : libraryDirs) {
            File file = new File(dir, libname);
            if (file.exists())
                return file.toString();
        }

        return null;
    }

    private Class doFindClass(String name) throws ClassNotFoundException {
        try {
            String path = convertToPath(name);
            byte[] data = loadResource(path);
            if (data == null)
                throw new ClassNotFoundException("Class " + name + " not found.");

            definePackage(name);

            Class clazz = defineClass(name, data, 0, data.length);
            if (classes != null)
                classes.add(clazz);
            return clazz;
        } catch (Exception e) {
            throw new ClassNotFoundException("Class " + name + " not found.", e);
        }
    }

    private void definePackage(String className) {
        String packageName = null;
        int pos = className.lastIndexOf('.');
        if (pos != -1)
            packageName = className.substring(0, pos);

        if (packageName != null) {
            Package pkg = getPackage(packageName);
            if (pkg == null) {
                try {
                    definePackage(packageName, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    private byte[] loadResource(String path) throws IOException {
        if (jarFiles == null)
            loadJarFiles();

        for (File dir : dirs) {
            File file = new File(dir, path);
            if (file.exists())
                return loadData(new FileInputStream(file), path);
        }

        for (JarFile jarFile : jarFiles) {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry != null)
                return loadData(jarFile.getInputStream(entry), path);
        }

        return null;
    }

    private List<byte[]> loadResources(String path) throws IOException {
        if (jarFiles == null)
            loadJarFiles();

        List<byte[]> datas = new ArrayList<byte[]>();

        for (File dir : dirs) {
            File file = new File(dir, path);
            if (file.exists())
                datas.add(loadData(new FileInputStream(file), file.getPath()));
        }

        for (JarFile jarFile : jarFiles) {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry != null)
                datas.add(loadData(jarFile.getInputStream(entry), entry.getName()));
        }

        return datas;
    }

    private byte[] loadData(InputStream stream, String path) throws IOException {
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

        byte[] data = out.toByteArray();
        if (path.endsWith(".class")) {
            for (int i = 0; i < MAGIC.length; i++) {
                if (data[i] != MAGIC[i]) {
                    decrypt(data);
                    return data;
                }
            }
        }

        return data;
    }

    private void loadJarFiles() throws IOException {
        jarFiles = new ArrayList<JarFile>();

        for (File file : files)
            jarFiles.add(new JarFile(file, true));
    }

    private boolean isParentResource(String name) {
        name = name.replace('/', '.');
        return isParentClass(name);
    }

    private void clearStaticReferences() {
        List<Class> classes = this.classes;
        this.classes = null;

        for (Class clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                        field.get(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (Class clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true);

                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                        field.set(null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean isMappedLibraryName(String name) {
        if ((name.startsWith("lib") && (name.endsWith(".so") || name.endsWith(".dylib"))) ||
                name.endsWith(".dll"))
            return true;
        else
            return false;
    }

    private String convertToPath(String name) {
        return name.replace('.', '/') + ".class";
    }

    private void decrypt(byte[] value) {
        byte[] pattern = new byte[4];
        pattern[0] = (byte) (value.length >>> 0);
        pattern[1] = (byte) (value.length >>> 8);
        pattern[2] = pattern[1];
        pattern[3] = pattern[0];
        for (int i = 0; i < value.length; i++) {
            byte b = pattern[i % pattern.length];
            value[i] = (byte) (value[i] ^ b);
        }
    }

    private static class StreamUrlConnection extends URLConnection {
        private final InputStream stream;

        public StreamUrlConnection(URL url, InputStream stream) {
            super(url);
            this.stream = stream;
        }

        @Override
        public void connect() throws IOException {
            connected = true;
        }

        @Override
        public InputStream getInputStream() {
            return stream;
        }
    }

    private static class StreamHandler extends URLStreamHandler {
        private final InputStream stream;

        public StreamHandler(InputStream stream) {
            this.stream = stream;
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new StreamUrlConnection(url, stream);
        }
    }
}
