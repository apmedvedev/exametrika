/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ResponseMessage} is a response message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ResponseMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String testCaseName;
    private final String nodeName;
    private final boolean resultsOnly;
    private final Throwable exception;

    public ResponseMessage(String testCaseName, String nodeName, boolean resultsOnly, Throwable exception) {
        Assert.notNull(testCaseName);
        Assert.notNull(nodeName);

        this.testCaseName = testCaseName;
        this.nodeName = nodeName;
        this.resultsOnly = resultsOnly;
        this.exception = exception;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean isResultsOnly() {
        return resultsOnly;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(testCaseName, nodeName, resultsOnly, exception).toString();
    }

    private interface IMessages {
        @DefaultMessage("testcase: {0}, node: {1}, results only: {2}, exception: {3}")
        ILocalizedMessage toString(String testCaseName, String nodeName, boolean resultsOnly, Throwable exception);
    }
}

