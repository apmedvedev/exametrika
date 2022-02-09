/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import java.util.Map;


/**
 * The {@link IExpression} represents a compiled expression.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IExpression {
    /**
     * Returns text of expression.
     *
     * @return text of expression
     */
    String getText();

    /**
     * Executes expression.
     *
     * @param context   context. Can be null
     * @param variables variables. Can be null
     * @return return value. Can be null
     */
    <T> T execute(Object context, Map<String, ? extends Object> variables);
}
