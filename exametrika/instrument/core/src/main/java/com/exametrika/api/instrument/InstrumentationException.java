/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;


/**
 * The {@link InstrumentationException} is a root instrumentation exception.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class InstrumentationException extends SystemException {
    public InstrumentationException() {
        super();
    }

    public InstrumentationException(ILocalizedMessage message) {
        super(message);
    }

    public InstrumentationException(ILocalizedMessage message, Throwable cause) {
        super(message, cause);
    }

    public InstrumentationException(Throwable cause) {
        super(cause);
    }
}
