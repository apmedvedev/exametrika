/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.alerts.TagIncidentGroupAlert;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link TagIncidentGroupSchemaConfiguration} is an tag incident group schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TagIncidentGroupSchemaConfiguration extends AlertSchemaConfiguration {
    private final String pattern;

    public TagIncidentGroupSchemaConfiguration(String name, String description, List<? extends AlertChannelSchemaConfiguration> channels,
                                               List<String> tags, boolean enabled, String pattern) {
        super(name, description, channels, tags, enabled);

        Assert.notNull(pattern);

        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public IAlert createAlert(IDatabaseContext context) {
        return new TagIncidentGroupAlert(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TagIncidentGroupSchemaConfiguration))
            return false;

        TagIncidentGroupSchemaConfiguration configuration = (TagIncidentGroupSchemaConfiguration) o;
        return super.equals(configuration) && pattern.equals(configuration.pattern);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pattern);
    }
}
