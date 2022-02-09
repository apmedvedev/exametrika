/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import java.io.Serializable;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Objects;

/**
 * The {@link AsyncActionParameterDefinitionSchemaConfiguration} represents a parameter definition for base action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AsyncActionParameterDefinitionSchemaConfiguration extends Configuration {
    public final boolean required;
    public final String optionsName;
    public final String propertiesName;
    public final Serializable defaultValue;

    public AsyncActionParameterDefinitionSchemaConfiguration(boolean required, String optionsName, String propertiesName, Serializable defaultValue) {
        this.required = required;
        this.optionsName = optionsName;
        this.propertiesName = propertiesName;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AsyncActionParameterDefinitionSchemaConfiguration))
            return false;

        AsyncActionParameterDefinitionSchemaConfiguration configuration = (AsyncActionParameterDefinitionSchemaConfiguration) o;
        return required == configuration.required && Objects.equals(optionsName, configuration.optionsName) &&
                Objects.equals(propertiesName, configuration.propertiesName) && Objects.equals(defaultValue, configuration.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(required, optionsName, propertiesName, defaultValue);
    }
}