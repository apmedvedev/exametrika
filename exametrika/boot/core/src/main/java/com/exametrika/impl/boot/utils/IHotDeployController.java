/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot.utils;


/**
 * The {@link IHotDeployController} is used to control hot deploy process.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHotDeployController {
    /**
     * Called when contents of configuration directory are changed.
     */
    void onConfigurationChanged();

    /**
     * Called when contents of hot deploy directory are changed and deployer needs to update working directory.
     */
    void onBeginHotDeploy();

    /**
     * Called when contents of hot deploy directory are changed and deployer has updated working directory.
     */
    void onEndHotDeploy();
}
