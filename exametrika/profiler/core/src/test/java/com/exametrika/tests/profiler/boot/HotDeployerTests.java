/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.boot;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.exametrika.common.utils.Files;
import com.exametrika.impl.boot.utils.HotDeployer;
import com.exametrika.impl.boot.utils.IHotDeployController;
import com.exametrika.impl.boot.utils.Utils;


/**
 * The {@link HotDeployerTests} are tests for {@link HotDeployer}.
 *
 * @author Medvedev-A
 * @see HotDeployer
 */
public class HotDeployerTests {
    @Test
    public void testConfigurationChanges() throws Exception {
        HotDeployControllerMock controller = new HotDeployControllerMock();
        File tempDir = new File(new File(System.getProperty("java.io.tmpdir")), "hotdeploy");
        Files.emptyDir(tempDir);
        tempDir.mkdirs();
        File tempFile1 = File.createTempFile("test", ".tmp", tempDir);
        File tempFile2 = File.createTempFile("test", ".tmp", tempDir);

        HotDeployer deployer = new HotDeployer(tempDir, null, tempDir, controller, 50, 100, 0);
        deployer.start();

        Thread.sleep(300);
        assertThat(controller.started, is(false));
        assertThat(controller.stopped, is(false));
        assertThat(controller.configUpdated, is(false));

        File tempFile3 = File.createTempFile("test", ".tmp", tempDir);

        Thread.sleep(300);
        assertThat(controller.started, is(false));
        assertThat(controller.stopped, is(false));
        assertThat(controller.configUpdated, is(true));
        controller.configUpdated = false;
        Thread.sleep(300);
        assertThat(controller.configUpdated, is(false));

        tempFile2.delete();

        Thread.sleep(300);
        assertThat(controller.started, is(false));
        assertThat(controller.stopped, is(false));
        assertThat(controller.configUpdated, is(true));
        controller.configUpdated = false;

        tempFile1.setLastModified(System.currentTimeMillis());

        Thread.sleep(300);
        assertThat(controller.started, is(false));
        assertThat(controller.stopped, is(false));
        assertThat(controller.configUpdated, is(true));
        controller.configUpdated = false;

        deployer.stop();

        tempFile3.delete();
    }

    @Test
    public void testHotDeployChanges() throws Exception {
        HotDeployControllerMock controller = new HotDeployControllerMock();
        File hotDeployDir = new File(new File(System.getProperty("java.io.tmpdir")), "hotdeploy");
        File workDir = new File(new File(System.getProperty("java.io.tmpdir")), "work");
        Files.emptyDir(hotDeployDir);
        Files.emptyDir(workDir);
        hotDeployDir.mkdirs();
        workDir.mkdirs();
        File tempFile1 = File.createTempFile("test", ".tmp", hotDeployDir);
        File tempFile2 = File.createTempFile("test", ".tmp", hotDeployDir);
        File subDir = new File(hotDeployDir, "dir");
        subDir.mkdirs();
        File tempFile3 = File.createTempFile("test", ".tmp", subDir);
        File tempFile4 = File.createTempFile("test", ".tmp", subDir);

        HotDeployer deployer = new HotDeployer(null, hotDeployDir, workDir, controller, 50, 200, 0);
        workDir = deployer.getWorkModulesPath();
        deployer.start();

        assertThat(workFileExists(hotDeployDir, workDir, tempFile1), is(true));
        assertThat(workFileExists(hotDeployDir, workDir, tempFile2), is(true));
        assertThat(workFileExists(hotDeployDir, workDir, subDir), is(true));
        assertThat(workFileExists(hotDeployDir, workDir, tempFile3), is(true));
        assertThat(workFileExists(hotDeployDir, workDir, tempFile4), is(true));

        Thread.sleep(300);
        assertThat(controller.started, is(false));
        assertThat(controller.stopped, is(false));
        assertThat(controller.configUpdated, is(false));

        File tempFile5 = File.createTempFile("test", ".tmp", subDir);

        Thread.sleep(1000);
        assertThat(controller.started, is(true));
        assertThat(controller.stopped, is(true));
        assertThat(controller.configUpdated, is(false));
        controller.started = false;
        controller.stopped = false;
        assertThat(workFileExists(hotDeployDir, workDir, tempFile5), is(true));

        tempFile2.delete();
        tempFile5.delete();

        Thread.sleep(1000);
        assertThat(controller.started, is(true));
        assertThat(controller.stopped, is(true));
        assertThat(controller.configUpdated, is(false));
        controller.started = false;
        controller.stopped = false;
        assertThat(workFileExists(hotDeployDir, workDir, tempFile2), is(false));
        assertThat(workFileExists(hotDeployDir, workDir, tempFile5), is(false));

        tempFile1.setLastModified(System.currentTimeMillis());

        Thread.sleep(1000);
        assertThat(controller.started, is(true));
        assertThat(controller.stopped, is(true));
        assertThat(controller.configUpdated, is(false));
        controller.started = false;
        controller.stopped = false;
        assertThat(workFileExists(hotDeployDir, workDir, tempFile1), is(true));

        deployer.stop();

        assertThat(workDir.listFiles().length, is(0));
    }

    @Test
    public void testUpdate() throws Throwable {
        String tmpDir = System.getProperty("java.io.tmpdir");
        BootConfigurationLoaderTests.copy(tmpDir, "modules.zip");
        BootConfigurationLoaderTests.copy(tmpDir, "config.zip");

        File modulesArchive = new File(tmpDir, "modules.zip");

        HotDeployControllerMock controller = new HotDeployControllerMock();
        File configDir = new File(new File(System.getProperty("java.io.tmpdir")), "config");
        File hotDeployDir = new File(new File(System.getProperty("java.io.tmpdir")), "hotdeploy");
        File workDir = new File(new File(System.getProperty("java.io.tmpdir")), "work");
        Files.emptyDir(configDir);
        Files.emptyDir(hotDeployDir);
        Files.emptyDir(workDir);
        hotDeployDir.mkdirs();
        workDir.mkdirs();

        HotDeployer deployer = new HotDeployer(configDir, hotDeployDir, workDir, controller, 50, 200, 0);
        workDir = deployer.getWorkModulesPath();
        deployer.start();

        deployer.update("test configuration", "", "config.conf", modulesArchive);

        Thread.sleep(1000);

        assertThat(controller.started, is(true));
        assertThat(controller.stopped, is(true));
        assertThat(new File(workDir, "hello1").exists(), is(true));
        assertThat(new File(workDir, "hello1/qq.pdf").exists(), is(true));

        assertThat(new File(tmpDir, "config/config.conf").exists(), is(true));

        assertThat(new File(tmpDir, "hotdeploy/hello1").exists(), is(true));
        assertThat(new File(tmpDir, "hotdeploy/hello1/qq.pdf").exists(), is(true));

        deployer.stop();
    }

    @Test
    public void testReconcile() throws Throwable {
        String tmpDir = System.getProperty("java.io.tmpdir");
        BootConfigurationLoaderTests.copy(tmpDir, "modules.zip");
        BootConfigurationLoaderTests.copy(tmpDir, "config.zip");

        HotDeployControllerMock controller = new HotDeployControllerMock();
        File configDir = new File(new File(System.getProperty("java.io.tmpdir")), "config");
        File hotDeployDir = new File(new File(System.getProperty("java.io.tmpdir")), "hotdeploy");
        File workDir = new File(new File(System.getProperty("java.io.tmpdir")), "work");
        Files.emptyDir(configDir);
        Files.emptyDir(hotDeployDir);
        Files.emptyDir(workDir);
        hotDeployDir.mkdirs();
        workDir.mkdirs();

        Utils.unzip(new File(tmpDir, "modules.zip"), new File(workDir, "newLib"));
        Utils.unzip(new File(tmpDir, "config.zip"), new File(workDir, "newConf"));
        new File(workDir, "update.lock").createNewFile();
        HotDeployer deployer = new HotDeployer(configDir, hotDeployDir, workDir, controller, 50, 200, 0);
        workDir = deployer.getWorkModulesPath();

        assertThat(new File(tmpDir, "config/hello1").exists(), is(true));
        assertThat(new File(tmpDir, "config/hello1/qq.pdf").exists(), is(true));

        assertThat(new File(tmpDir, "hotdeploy/hello1").exists(), is(true));
        assertThat(new File(tmpDir, "hotdeploy/hello1/qq.pdf").exists(), is(true));

        Thread.sleep(100);

        assertThat(workDir.listFiles().length, is(1));
    }

    private boolean workFileExists(File sourceRootDir, File destinationRootDir, File sourceFile) {
        if (!sourceFile.getPath().startsWith(sourceRootDir.getPath()))
            return false;

        String relativePath = sourceFile.getPath().substring(sourceRootDir.getPath().length());
        return new File(destinationRootDir, relativePath).exists();
    }

    private static class HotDeployControllerMock implements IHotDeployController {
        public volatile boolean started;
        public volatile boolean stopped;
        public volatile boolean configUpdated;

        @Override
        public void onEndHotDeploy() {
            started = true;
        }

        @Override
        public void onBeginHotDeploy() {
            stopped = true;
        }

        @Override
        public void onConfigurationChanged() {
            configUpdated = true;
        }
    }
}
