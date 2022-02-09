/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import com.exametrika.common.l10n.ILocalizedMessage;

/**
 * The {@link RawPageNotFoundException} is thrown when page is not found in read-only transaction.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class RawPageNotFoundException extends RawDatabaseException {
    public RawPageNotFoundException() {
    }

    public RawPageNotFoundException(ILocalizedMessage message) {
        super(message);
    }

    public RawPageNotFoundException(ILocalizedMessage message, Throwable cause) {
        super(message, cause);
    }

    public RawPageNotFoundException(Throwable cause) {
        super(cause);
    }
}
