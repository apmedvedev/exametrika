/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;


/**
 * The {@link IExpressionNode} is a single expression node, participating in expression.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IExpressionNode {
    /**
     * Evaluates result of expression.
     *
     * @param context expression context or null if context is not set
     * @param self    self (evaluation) context or null if context is not set
     * @return result of evaluation. Can be null
     */
    Object evaluate(ExpressionContext context, Object self);
}
