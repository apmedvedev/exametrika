/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.nodes.IExitPointNode;
import com.exametrika.api.exadb.objectdb.INode;


/**
 * The {@link ExitPointNode} is a exit point node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExitPointNode extends StackNode implements IExitPointNode {
    public ExitPointNode(INode node) {
        super(node);
    }
}