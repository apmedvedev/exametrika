/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell;

import java.util.List;

/**
 * The {@link IShellCommandProvider} is a provider of shell commands.
 *
 * @author Medvedev-A
 */
public interface IShellCommandProvider {
    /**
     * Returns list of shell commands.
     *
     * @return list of shell commands
     */
    List<IShellCommand> getCommands();
}