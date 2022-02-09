/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.boot.config;

import java.io.File;
import java.util.List;
import java.util.Set;


/**
 * The {@link BootConfiguration} represents a boot properties.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BootConfiguration {
    public static final String SCHEMA = "com.exametrika.boot-1.0";
    private final boolean productionMode;
    private final Set<String> runModes;
    private final List<File> bootClassPaths;
    private final List<File> systemClassPaths;
    private final List<File> classPaths;
    private final List<File> libraryPaths;
    private final List<String> systemPackages;
    private final File workPath;
    private final File hotDeployConfigPath;
    private final File hotDeployModulesPath;
    private final long hotDeployDetectionPeriod;
    private final long hotDeployRedeploymentPeriod;
    private final long hotDeployRestartDelayPeriod;

    public BootConfiguration(boolean productionMode, Set<String> runModes, List<File> bootClassPaths, List<File> systemClassPaths,
                             List<File> classPaths, List<File> libraryPaths, List<String> systemPackages, File workPath,
                             File hotDeployConfigPath, File hotDeployModulesPath, long hotDeployDetectionPeriod,
                             long hotDeployRedeploymentPeriod, long hotDeployRestartDelayPeriod) {
        if (runModes == null)
            throw new IllegalArgumentException();
        if (bootClassPaths == null)
            throw new IllegalArgumentException();
        if (systemClassPaths == null)
            throw new IllegalArgumentException();
        if (classPaths == null)
            throw new IllegalArgumentException();
        if (libraryPaths == null)
            throw new IllegalArgumentException();
        if (systemPackages == null)
            throw new IllegalArgumentException();

        this.runModes = runModes;
        this.productionMode = productionMode;
        this.bootClassPaths = bootClassPaths;
        this.systemClassPaths = systemClassPaths;
        this.classPaths = classPaths;
        this.libraryPaths = libraryPaths;
        this.systemPackages = systemPackages;
        this.workPath = workPath;
        this.hotDeployConfigPath = hotDeployConfigPath;
        this.hotDeployModulesPath = hotDeployModulesPath;
        this.hotDeployDetectionPeriod = hotDeployDetectionPeriod;
        this.hotDeployRedeploymentPeriod = hotDeployRedeploymentPeriod;
        this.hotDeployRestartDelayPeriod = hotDeployRestartDelayPeriod;
    }

    public boolean isProductionMode() {
        return productionMode;
    }

    public Set<String> getRunModes() {
        return runModes;
    }

    public List<File> getBootClassPaths() {
        return bootClassPaths;
    }

    public List<File> getSystemClassPaths() {
        return systemClassPaths;
    }

    public List<File> getClassPaths() {
        return classPaths;
    }

    public List<File> getLibraryPaths() {
        return libraryPaths;
    }

    public List<String> getSystemPackages() {
        return systemPackages;
    }

    public File getWorkPath() {
        return workPath;
    }

    public File getHotDeployConfigPath() {
        return hotDeployConfigPath;
    }

    public File getHotDeployModulesPath() {
        return hotDeployModulesPath;
    }

    public long getHotDeployDetectionPeriod() {
        return hotDeployDetectionPeriod;
    }

    public long getHotDeployRedeploymentPeriod() {
        return hotDeployRedeploymentPeriod;
    }

    public long getHotDeployRestartDelayPeriod() {
        return hotDeployRestartDelayPeriod;
    }
}
