/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.config;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.schema.IJsonDiagnostics;
import com.exametrika.common.json.schema.IJsonValidationContext;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonType;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;


/**
 * The {@link ServerChannelValidator} is a validator of server channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerChannelValidator implements IJsonValidator {
    private static final IMessages messages = Messages.get(IMessages.class);

    @Override
    public boolean supports(Class clazz) {
        return clazz == JsonObject.class || clazz == JsonObjectBuilder.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context) {
        IJsonDiagnostics diagnostics = context.getDiagnostics();

        JsonObject object = (JsonObject) instance;
        if (Boolean.TRUE.equals(object.get("secured", null))) {
            if (object.get("keyStorePath", null) == null)
                diagnostics.addError(messages.nullInRequiredProperty(diagnostics.getPath() + ".keyStorePath"));
            if (object.get("keyStorePassword", null) == null)
                diagnostics.addError(messages.nullInRequiredProperty(diagnostics.getPath() + ".keyStorePassword"));
        }
    }

    private interface IMessages {
        @DefaultMessage("Validation error of ''{0}''. Value of required property can not be null.")
        ILocalizedMessage nullInRequiredProperty(String path);
    }
}
