/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import com.exametrika.api.component.config.model.TagIncidentGroupSchemaConfiguration;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.component.nodes.IncidentNode;


/**
 * The {@link TagIncidentGroupAlert} represents a tag incident group alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TagIncidentGroupAlert extends IncidentGroupAlert {
    private final ICondition<String> condition;

    public TagIncidentGroupAlert(TagIncidentGroupSchemaConfiguration configuration) {
        super(configuration);

        this.condition = Strings.createFilterCondition(configuration.getPattern(), true);
    }

    @Override
    protected boolean isMatched(IIncident incident) {
        for (String tag : ((IncidentNode) incident).getAlert().getConfiguration().getTags()) {
            if (condition.evaluate(tag))
                return true;
        }

        return false;
    }
}
