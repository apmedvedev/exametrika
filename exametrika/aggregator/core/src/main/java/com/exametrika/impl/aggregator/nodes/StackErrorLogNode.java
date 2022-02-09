/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.nodes.IStackErrorLogNode;
import com.exametrika.api.exadb.objectdb.INode;


/**
 * The {@link StackErrorLogNode} is an stack error log node.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StackErrorLogNode extends StackLogNode implements IStackErrorLogNode {
    public StackErrorLogNode(INode node) {
        super(node);
    }
}