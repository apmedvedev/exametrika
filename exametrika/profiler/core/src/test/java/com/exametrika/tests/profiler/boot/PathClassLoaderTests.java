/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.boot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;

import org.junit.Test;

import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.ITestable;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.boot.Bootstrap;
import com.exametrika.impl.boot.utils.PathClassLoader;


/**
 * The {@link PathClassLoaderTests} are tests for {@link PathClassLoader}.
 *
 * @author Medvedev-A
 * @see PathClassLoader
 */
public class PathClassLoaderTests {
    @Test
    public void testClassLoader() throws Throwable {
        File tempDir = new File(new File(new File(System.getProperty("java.io.tmpdir")), "classload"), "dir");
        Files.emptyDir(tempDir);
        tempDir.mkdirs();

        File testFile = new File(tempDir, "test.jar");
        OutputStream out = new BufferedOutputStream(new FileOutputStream(testFile));

        String path = getClass().getName().replace('.', '/');
        int pos = path.lastIndexOf('/');
        path = path.substring(0, pos) + "/test.jar";
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);

        IOs.copy(in, out);

        IOs.close(out);

        File testJarFile = new File(tempDir, "test.jar");
        File tempFile1 = File.createTempFile("test", ".tmp", tempDir);

        final PathClassLoader loader = new PathClassLoader(Arrays.asList(testJarFile, tempDir.getParentFile()),
                Arrays.<File>asList(), Arrays.asList("java", "javax", "com.sun"), getClass().getClassLoader());

        Class clazz1 = loader.loadClass(String.class.getName());
        assertThat(clazz1.getClassLoader(), nullValue());
        Class clazz2 = loader.loadClass(Test.class.getName());
        assertThat(clazz2.getClassLoader() == loader, is(true));
        new Expected(ClassNotFoundException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                loader.loadClass(Bootstrap.class.getName());
            }
        });

        InputStream stream = loader.getResourceAsStream("META-INF/MANIFEST.MF");
        assertThat(stream != null, is(true));
        stream = loader.getResourceAsStream("LICENSE1.txt");
        assertThat(stream, nullValue());

        stream = loader.getResourceAsStream(tempDir.getName() + File.separatorChar + tempFile1.getName());
        assertThat(stream.read(), is(-1));

        stream = loader.getResourceAsStream(tempDir.getName() + File.separatorChar + tempFile1.getName() + new Random().nextInt());
        assertThat(stream, nullValue());

        URL url = loader.getResource("META-INF/MANIFEST.MF");
        assertThat(url.openStream() != null, is(true));

        Enumeration<URL> urls = loader.getResources("META-INF/MANIFEST.MF");
        assertThat(urls.nextElement().openStream() != null, is(true));
        assertThat(urls.hasMoreElements(), is(false));

        loader.close();

        assertThat(testJarFile.delete(), is(true));
    }
}
