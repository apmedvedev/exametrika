/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;


/**
 * The {@link TimeoutException} is thrown when wait timeout occurs.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class TimeoutException extends SystemException {
    public TimeoutException() {
        super();
    }

    public TimeoutException(ILocalizedMessage message) {
        super(message);
    }

    public TimeoutException(ILocalizedMessage message, Throwable cause) {
        super(message, cause);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }
}
