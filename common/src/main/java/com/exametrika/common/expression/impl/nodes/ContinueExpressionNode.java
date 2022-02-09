/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;


/**
 * The {@link ContinueExpressionNode} is a continue expression node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ContinueExpressionNode implements IExpressionNode {
    @Override
    public Object evaluate(ExpressionContext context, Object self) {
        context.setContinueRequested(true);
        return null;
    }

    @Override
    public String toString() {
        return "continue";
    }
}
