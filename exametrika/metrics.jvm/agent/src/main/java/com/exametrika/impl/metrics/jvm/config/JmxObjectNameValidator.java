/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.config;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.exametrika.common.json.schema.IJsonDiagnostics;
import com.exametrika.common.json.schema.IJsonValidationContext;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonType;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;


/**
 * The {@link JmxObjectNameValidator} is a validator of child monitors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmxObjectNameValidator implements IJsonValidator {
    private static final IMessages messages = Messages.get(IMessages.class);

    @Override
    public boolean supports(Class clazz) {
        return clazz == String.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context) {
        String name = (String) instance;

        try {
            ObjectName.getInstance(name);
        } catch (MalformedObjectNameException e) {
            IJsonDiagnostics diagnostics = context.getDiagnostics();
            diagnostics.addError(messages.invalidName(diagnostics.getPath(), name, e.getMessage()));
        }
    }

    private interface IMessages {
        @DefaultMessage("Validation error of ''{0}''. Invalid JMX object name ''{1}'': {2}.")
        ILocalizedMessage invalidName(String path, String name, String message);
    }
}
