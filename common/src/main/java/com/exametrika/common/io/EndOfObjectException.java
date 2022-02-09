/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import com.exametrika.common.l10n.ILocalizedMessage;


/**
 * The {@link EndOfObjectException} is thrown when end of object has been reached.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class EndOfObjectException extends SerializationException {
    public EndOfObjectException() {
        super();
    }

    public EndOfObjectException(ILocalizedMessage message) {
        super(message);
    }

    public EndOfObjectException(ILocalizedMessage message, Throwable cause) {
        super(message, cause);
    }

    public EndOfObjectException(Throwable cause) {
        super(cause);
    }
}