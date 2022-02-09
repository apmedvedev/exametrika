/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;


/**
 * The {@link SelfExpressionNode} is a "self" (context) expression node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SelfExpressionNode implements IExpressionNode {
    @Override
    public Object evaluate(ExpressionContext context, Object self) {
        return self;
    }

    @Override
    public String toString() {
        return "$self";
    }
}
