/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link InstallMessage} is an install message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstallMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String testCaseName;
    private final String roleName;
    private final String nodeName;
    private final String executorName;
    private final Map<String, Object> executorParameters;

    public InstallMessage(String testCaseName, String roleName, String nodeName, String executorName, Map<String, Object> executorParameters) {
        Assert.notNull(testCaseName);
        Assert.notNull(nodeName);
        Assert.notNull(executorName);
        Assert.notNull(executorParameters);

        this.testCaseName = testCaseName;
        this.roleName = roleName;
        this.nodeName = nodeName;
        this.executorName = executorName;
        this.executorParameters = Immutables.wrap(executorParameters);
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getExecutorName() {
        return executorName;
    }

    public Map<String, Object> getExecutorParameters() {
        return executorParameters;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(testCaseName, roleName, nodeName, executorName, executorParameters).toString();
    }

    private interface IMessages {
        @DefaultMessage("testcase: {0}, role: {1}, install node: {2}, executor: {3}, executor parameters: {4}")
        ILocalizedMessage toString(String testCaseName, String roleName, String nodeName, String executorName, Map<String, Object> executorParameters);
    }
}

