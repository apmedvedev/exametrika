/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;
import java.util.Map;

import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link ShellContext} is a shell context.
 *
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellContext implements IShellContext {
    private final Shell shell;

    public ShellContext(Shell shell) {
        Assert.notNull(shell);

        this.shell = shell;
    }

    @Override
    public IShell getShell() {
        return shell;
    }

    @Override
    public String getPath() {
        ShellNode node = shell.getContextNode();
        char nameSeparator = shell.getNameSeparator();
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        while (node.getParent() != null) {
            if (first)
                first = false;
            else
                builder.insert(0, nameSeparator);

            builder.insert(0, node.getName());

            node = node.getParent();
        }

        return builder.toString();
    }

    @Override
    public Object executeCommand(String commandName, Map<String, Object> parameters) {
        return shell.executeCommand(commandName, parameters);
    }

    @Override
    public Object executeLine(String commandLine) {
        List<Pair<IShellCommand, Map<String, Object>>> parsedCommands = shell.getParser().parseCommands("", commandLine);
        return shell.execute(this, parsedCommands);
    }
}