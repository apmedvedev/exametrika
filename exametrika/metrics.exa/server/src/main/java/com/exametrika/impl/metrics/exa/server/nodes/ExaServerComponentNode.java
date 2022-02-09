/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.nodes;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.metrics.exa.server.nodes.IExaServerComponent;
import com.exametrika.impl.component.nodes.HealthComponentNode;


/**
 * The {@link ExaServerComponentNode} is a exa server component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaServerComponentNode extends HealthComponentNode implements IExaServerComponent {
    public ExaServerComponentNode(INode node) {
        super(node);
    }
}