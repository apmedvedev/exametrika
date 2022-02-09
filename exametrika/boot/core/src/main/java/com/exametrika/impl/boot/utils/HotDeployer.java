/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * The {@link HotDeployer} is used to hot deploy configuration and other files.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HotDeployer implements IHotDeployer {
    private final File configPath;
    private final File modulesPath;
    private final File workPath;
    private final File workModulesPath;
    private final IHotDeployController controller;
    private final long detectionPeriod;
    private final long redeploymentPeriod;
    private final long restartDelayPeriod;
    private final Map<File, FileInfo> configurationChangesMap = new HashMap<File, FileInfo>();
    private final Map<File, FileInfo> hotDeployChangesMap = new HashMap<File, FileInfo>();
    private final Timer timer;
    private long lastDetectionTime;
    private long nextRedeploymentTime;
    private Set<File> newOrChangedFiles;
    private Set<File> deletedFiles;
    private Deployer deployer;
    private boolean enabled = true;
    private boolean active;

    /**
     * Creates an object.
     *
     * @param configPath         hot deploy configuration directory. Can be null if not used
     * @param modulesPath        hot deploy modules directory. Can be null if not used
     * @param workPath           work hot deploy directory
     * @param controller         hot deploy controller
     * @param detectionPeriod    period in milliseconds to detect file changes
     * @param redeploymentPeriod period in milliseconds between last file changes and subsequent redeployments or between two redeployments
     * @param restartDelayPeriod period in milliseconds between stop and start of container
     */
    public HotDeployer(File configPath, File modulesPath, File workPath, IHotDeployController controller, long detectionPeriod,
                       long redeploymentPeriod, long restartDelayPeriod) {
        if (workPath == null || workPath.isFile())
            throw new IllegalArgumentException();

        workPath.mkdirs();

        if (controller == null)
            throw new IllegalArgumentException();

        File lock = new File(workPath, "update.lock");
        reconcileDir(workPath, configPath, "Conf", lock);
        reconcileDir(workPath, modulesPath, "Lib", lock);
        lock.delete();

        this.configPath = configPath;
        this.modulesPath = modulesPath;
        this.workPath = workPath;
        this.workModulesPath = new File(workPath, "modules");
        this.controller = controller;
        this.detectionPeriod = detectionPeriod;
        this.redeploymentPeriod = redeploymentPeriod;
        this.restartDelayPeriod = restartDelayPeriod;

        if (modulesPath != null) {
            Utils.emptyDir(workModulesPath);
            Utils.copy(modulesPath, workModulesPath);
        }

        this.timer = new Timer("Hot deploy thread", true);
    }

    public File getWorkModulesPath() {
        return workModulesPath;
    }

    @Override
    public String getConfigPath() {
        return configPath.getPath();
    }

    @Override
    public void update(String configuration, String relativePath, String configFileName, File modulesArchive) {
        if (modulesArchive != null && (modulesPath == null || !modulesArchive.exists() || !modulesArchive.isFile()))
            throw new IllegalArgumentException();

        setEnabled(false);

        File lock = new File(workPath, "update.lock");

        FileWriter writer = null;
        try {
            lock.delete();

            File newConfigPath = new File(workPath, "newConf" + File.separator + relativePath + File.separator + configFileName);
            File configPath = new File(this.configPath, relativePath + File.separator + configFileName);
            File newModulesPath = new File(workPath, "newLib" + File.separator + relativePath);
            File modulesPath = new File(this.modulesPath, relativePath);

            if (configuration != null) {
                Utils.emptyDir(newConfigPath.getParentFile());
                newConfigPath.getParentFile().mkdirs();
                writer = new FileWriter(newConfigPath);
                writer.write(configuration);
                Utils.close(writer);
                writer = null;
            }
            if (modulesArchive != null) {
                Utils.emptyDir(newModulesPath);
                Utils.unzip(modulesArchive, newModulesPath);
            }

            lock.createNewFile();

            if (configuration != null)
                configPath.delete();

            if (modulesArchive != null) {
                Utils.emptyDir(modulesPath);
                modulesPath.delete();
            }

            if (configuration != null) {
                configPath.getParentFile().mkdirs();
                Utils.move(newConfigPath, configPath);
            }
            if (modulesArchive != null) {
                modulesPath.mkdirs();
                Utils.move(newModulesPath, modulesPath);
            }

            Utils.delete(new File(workPath, "newConf"));
            Utils.delete(new File(workPath, "newLib"));

            lock.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Utils.close(writer);
            setEnabled(true);
        }
    }

    public void onTimer() {
        boolean redeploymentRequired = false;
        boolean fullRedeployment = false;
        Set<File> newOrChangedFiles = null;
        Set<File> deletedFiles = null;

        synchronized (this) {
            if (!enabled)
                return;

            if (System.currentTimeMillis() < lastDetectionTime + detectionPeriod)
                return;

            deletedFiles = new LinkedHashSet<File>();
            newOrChangedFiles = new LinkedHashSet<File>();

            if (modulesPath != null && detectChanges(hotDeployChangesMap, modulesPath, newOrChangedFiles, deletedFiles)) {
                if (this.newOrChangedFiles == null)
                    this.newOrChangedFiles = newOrChangedFiles;
                else
                    this.newOrChangedFiles.addAll(newOrChangedFiles);

                if (this.deletedFiles == null)
                    this.deletedFiles = deletedFiles;
                else
                    this.deletedFiles.addAll(deletedFiles);

                nextRedeploymentTime = System.currentTimeMillis() + redeploymentPeriod;
            } else if (configPath != null && detectChanges(configurationChangesMap, configPath, null, null))
                nextRedeploymentTime = System.currentTimeMillis() + redeploymentPeriod;

            if (nextRedeploymentTime != 0 && System.currentTimeMillis() > nextRedeploymentTime) {
                redeploymentRequired = true;
                if (this.newOrChangedFiles != null || this.deletedFiles != null) {
                    newOrChangedFiles = this.newOrChangedFiles;
                    deletedFiles = this.deletedFiles;
                    fullRedeployment = true;
                }

                nextRedeploymentTime = 0;
                this.newOrChangedFiles = null;
                this.deletedFiles = null;
            }

            lastDetectionTime = System.currentTimeMillis();

            if (redeploymentRequired)
                active = true;
        }

        if (redeploymentRequired) {
            if (fullRedeployment) {
                try {
                    controller.onBeginHotDeploy();

                    updateDir(modulesPath, workModulesPath, newOrChangedFiles, deletedFiles);

                    for (int i = 0; i < 10; i++) {
                        System.gc();
                        Thread.sleep(restartDelayPeriod / 10);
                    }

                    controller.onEndHotDeploy();
                } catch (Exception e) {
                    Utils.emptyDir(workModulesPath);

                    Loggers.logError(getClass().getName(), e);
                }
            } else {
                try {
                    controller.onConfigurationChanged();
                } catch (Exception e) {
                    Loggers.logError(getClass().getName(), e);
                }
            }

            synchronized (this) {
                active = false;
                notifyAll();
            }
        }
    }

    public synchronized void start() {
        if (deployer != null)
            throw new IllegalStateException();

        Loggers.logDebug(getClass().getName(), "Hot deployer is started.");

        if (configPath != null)
            detectChanges(configurationChangesMap, configPath, null, null);

        if (modulesPath != null)
            detectChanges(hotDeployChangesMap, modulesPath, null, null);

        deployer = new Deployer();
        timer.schedule(deployer, 0, detectionPeriod);
    }

    public synchronized void stop() {
        deployer.cancel();
        deployer = null;
        configurationChangesMap.clear();
        hotDeployChangesMap.clear();

        if (workModulesPath != null)
            Utils.emptyDir(workModulesPath);

        lastDetectionTime = 0;
        nextRedeploymentTime = 0;
        newOrChangedFiles = null;
        deletedFiles = null;

        Loggers.logDebug(getClass().getName(), "Hot deployer is stopped.");
    }

    private void setEnabled(boolean enabed) {
        synchronized (this) {
            this.enabled = enabed;
            while (!enabed && active) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }

        if (enabed)
            Loggers.logDebug(getClass().getName(), "Hot deployer is enabled.");
        else
            Loggers.logDebug(getClass().getName(), "Hot deployer is disabled.");
    }

    private boolean detectChanges(Map<File, FileInfo> changesMap, File dir, Set<File> newOrChangedFiles, Set<File> deletedFiles) {
        if (!dir.isDirectory())
            return false;

        boolean modified = false;
        for (File file : dir.listFiles()) {
            if (file.isDirectory())
                modified = detectChanges(changesMap, file, newOrChangedFiles, deletedFiles) || modified;
            else {
                long lastModified = file.lastModified();
                FileInfo info = changesMap.get(file);
                if (info == null) {
                    info = new FileInfo();
                    info.lastModified = lastModified;
                    changesMap.put(file, info);

                    modified = true;

                    if (newOrChangedFiles != null)
                        newOrChangedFiles.add(file);
                } else if (info.lastModified != lastModified) {
                    info.lastModified = lastModified;
                    modified = true;

                    if (newOrChangedFiles != null)
                        newOrChangedFiles.add(file);
                }
            }
        }

        for (Iterator<Entry<File, FileInfo>> it = changesMap.entrySet().iterator(); it.hasNext(); ) {
            File file = it.next().getKey();
            if (!file.exists()) {
                it.remove();
                modified = true;

                if (deletedFiles != null)
                    deletedFiles.add(file);
            }
        }

        return modified;
    }

    private void updateDir(File sourceRootDir, File destinationRootDir, Set<File> newOrChangedFiles, Set<File> deletedFiles) {
        if (newOrChangedFiles != null) {
            for (File source : newOrChangedFiles) {
                File destination = getDestinationFile(sourceRootDir, destinationRootDir, source);
                if (destination == null)
                    continue;

                Utils.copy(source, destination);
            }
        }

        if (deletedFiles != null) {
            for (File source : deletedFiles) {
                File destination = getDestinationFile(sourceRootDir, destinationRootDir, source);
                if (destination == null)
                    continue;

                destination.delete();
            }
        }
    }

    private File getDestinationFile(File sourceRootDir, File destinationRootDir, File sourceFile) {
        if (!sourceFile.getPath().startsWith(sourceRootDir.getPath()))
            return null;

        String relativePath = sourceFile.getPath().substring(sourceRootDir.getPath().length());
        return new File(destinationRootDir, relativePath);
    }

    private void reconcileDir(File workPath, File path, String suffix, File lock) {
        if (path != null) {
            File newPath = new File(workPath, "new" + suffix);
            if (lock.exists()) {
                if (newPath.exists()) {
                    path.getParentFile().mkdirs();
                    Utils.move(newPath, path);
                }
            }

            newPath.delete();

            path.mkdirs();
            if (!path.isDirectory())
                throw new IllegalArgumentException();
        }
    }

    private class Deployer extends TimerTask {
        @Override
        public void run() {
            onTimer();
        }
    }

    private static class FileInfo {
        long lastModified;
    }
}
