/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.boot;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import com.exametrika.api.boot.config.BootConfiguration;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.boot.config.BootConfigurationLoader;


/**
 * The {@link BootConfigurationLoaderTests} are tests for {@link BootConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see BootConfigurationLoader
 */
public class BootConfigurationLoaderTests {
    @Test
    public void testLoader() throws Throwable {
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("com.exametrika.home", tmpDir);
        System.setProperty("com.exametrika.workPath", tmpDir + "/work");
        BootConfigurationLoader loader = new BootConfigurationLoader();
        copy(tmpDir, "agent.conf");
        copy(tmpDir, "agent1.conf");
        copy(tmpDir, "agent2.conf");
        BootConfiguration configuration = loader.load(new File(tmpDir, "agent.conf").toString());

        assertThat(configuration.getRunModes(), is(Collections.asSet("mode1", "mode2")));
        assertThat(configuration.getBootClassPaths(), is(Arrays.asList(new File(tmpDir, "boot1"), new File(tmpDir, "boot2"),
                new File(tmpDir, "boot3"), new File(tmpDir, "boot4"), new File(tmpDir, "boot5"), new File(tmpDir, "boot6"))));
        assertThat(configuration.getSystemClassPaths(), is(Arrays.asList(new File(tmpDir, "system1"), new File(tmpDir, "system2"),
                new File(tmpDir, "system3"), new File(tmpDir, "system4"), new File(tmpDir, "system5"), new File(tmpDir, "system6"))));
        assertThat(configuration.getClassPaths(), is(Arrays.asList(new File(tmpDir, "path1"), new File(tmpDir, "path2"),
                new File(tmpDir, "path3"), new File(tmpDir, "path4"), new File(tmpDir, "path5"), new File(tmpDir, "path6"))));
        assertThat(configuration.getLibraryPaths(), is(Arrays.asList(new File(tmpDir, "lib1"), new File(tmpDir, "lib2"),
                new File(tmpDir, "lib3"), new File(tmpDir, "lib4"), new File(tmpDir, "lib5"), new File(tmpDir, "lib6"))));
        assertThat(configuration.getSystemPackages(), is(Arrays.asList("java", "javax", "org.w3c", "org.xml", "sun", "sunw", "com.sun",
                "org.ietf.jgss", "org.omg", "org.jcp.xml", "jdk", "com.exametrika.impl.boot", "my.package1", "my.package2",
                "my.package3", "my.package4", "my.package5", "my.package6")));
        assertThat(configuration.getWorkPath(), is(new File(tmpDir, "work")));
        assertThat(configuration.getHotDeployConfigPath(), is(new File(tmpDir, "conf")));
        assertThat(configuration.getHotDeployModulesPath(), is(new File(tmpDir, "lib")));
        assertThat(configuration.getHotDeployDetectionPeriod(), is(1000L));
        assertThat(configuration.getHotDeployRedeploymentPeriod(), is(2000L));
        assertThat(configuration.getHotDeployRestartDelayPeriod(), is(2000L));
    }

    public static void copy(String tmpDir, String name) throws IOException, FileNotFoundException {
        File file = new File(tmpDir, name);
        file.deleteOnExit();
        IOs.copy(BootConfigurationLoaderTests.class.getClassLoader().getResourceAsStream(
                BootConfigurationLoaderTests.class.getPackage().getName().replace('.', '/') + "/" + name), new FileOutputStream(file));
    }
}
