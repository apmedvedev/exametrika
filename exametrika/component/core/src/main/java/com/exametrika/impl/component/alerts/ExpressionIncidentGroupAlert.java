/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import java.util.Map;

import com.exametrika.api.component.config.model.ExpressionIncidentGroupSchemaConfiguration;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;


/**
 * The {@link ExpressionIncidentGroupAlert} represents a expression incident group alert.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionIncidentGroupAlert extends IncidentGroupAlert {
    private final Map<String, Object> runtimeContext;
    private final IExpression condition;

    public ExpressionIncidentGroupAlert(ExpressionIncidentGroupSchemaConfiguration configuration) {
        super(configuration);

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = MeterExpressions.getRuntimeContext();
        this.condition = Expressions.compile(configuration.getExpression(), compileContext);
    }

    @Override
    protected boolean isMatched(IIncident incident) {
        return condition.execute(incident, runtimeContext);
    }
}
