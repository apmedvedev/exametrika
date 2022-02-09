/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot.utils;

import java.io.File;


/**
 * The {@link IHotDeployer} is used to control hot deployer.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHotDeployer {
    /**
     * Returns configuration path.
     *
     * @return configuration path
     */
    String getConfigPath();

    /**
     * Updates contents of configuration and modules directories with contents from specified zip archives.
     *
     * @param configuration  inline configuration
     * @param relativePath   path relative to base directory
     * @param configFileName configuration file name (without path)
     * @param modulesArchive modules archive
     */
    void update(String configuration, String relativePath, String configFileName, File modulesArchive);
}
