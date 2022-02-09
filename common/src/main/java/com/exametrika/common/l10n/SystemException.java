/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.l10n;

import java.util.Locale;


/**
 * The {@link SystemException} is an system exception whose message can be localized.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class SystemException extends RuntimeException implements ILocalizedException {
    private transient final ILocalizedMessage message;
    private transient final String messageStr;

    public SystemException() {
        message = null;
        messageStr = null;
    }

    public SystemException(ILocalizedMessage message) {
        this.message = message;

        if (message != null)
            messageStr = message.getString();
        else
            messageStr = null;
    }

    public SystemException(ILocalizedMessage message, Throwable cause) {
        super(cause);

        this.message = message;

        if (message != null)
            messageStr = message.getString();
        else
            messageStr = super.getMessage();
    }

    public SystemException(Throwable cause) {
        super(cause);

        if (cause instanceof ILocalizedException)
            message = ((ILocalizedException) cause).getLocalized();
        else
            message = null;

        if (message != null)
            messageStr = message.getString();
        else
            messageStr = super.getMessage();
    }

    @Override
    public final String getMessage() {
        return messageStr;
    }

    @Override
    public final String getMessage(Locale locale) {
        return message != null ? message.getString(locale) : super.getMessage();
    }

    @Override
    public final ILocalizedMessage getLocalized() {
        return message;
    }
}
