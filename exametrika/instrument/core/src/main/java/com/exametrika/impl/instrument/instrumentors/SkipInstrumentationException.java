/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.SystemException;


/**
 * The {@link SkipInstrumentationException} is thrown when class transformation must be skipped.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class SkipInstrumentationException extends SystemException {
    public SkipInstrumentationException() {
        super();
    }

    public SkipInstrumentationException(ILocalizedMessage message) {
        super(message);
    }

    public SkipInstrumentationException(ILocalizedMessage message, Throwable cause) {
        super(message, cause);
    }

    public SkipInstrumentationException(Throwable cause) {
        super(cause);
    }
}
