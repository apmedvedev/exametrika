/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.config;

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
 * The {@link ChildMonitorValidator} is a validator of child monitors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ChildMonitorValidator implements IJsonValidator {
    private static final IMessages messages = Messages.get(IMessages.class);

    @Override
    public boolean supports(Class clazz) {
        return clazz == JsonObject.class || clazz == JsonObjectBuilder.class;
    }

    @Override
    public void validate(JsonType type, Object instance, IJsonValidationContext context) {
        JsonObject monitor = (JsonObject) instance;

        IJsonDiagnostics diagnostics = context.getDiagnostics();

        if (monitor.get("measurementStrategy", null) != null)
            diagnostics.addError(messages.strategyNotAllowed(diagnostics.getPath()));
    }

    private interface IMessages {
        @DefaultMessage("Validation error of ''{0}''. Measurement strategy is not allowed in child monitor.")
        ILocalizedMessage strategyNotAllowed(String path);
    }
}
