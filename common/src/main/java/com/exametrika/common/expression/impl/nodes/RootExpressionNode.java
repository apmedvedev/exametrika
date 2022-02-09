/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;


/**
 * The {@link RootExpressionNode} is a "root" (context) expression node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RootExpressionNode implements IExpressionNode {
    @Override
    public Object evaluate(ExpressionContext context, Object self) {
        return context.getRoot();
    }

    @Override
    public String toString() {
        return "$root";
    }
}
