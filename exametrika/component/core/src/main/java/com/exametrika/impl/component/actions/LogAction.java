/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.actions;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.component.nodes.ComponentNode;


/**
 * The {@link LogAction} is a log action.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class LogAction extends Action {
    public LogAction(IComponent component, IActionSchema schema) {
        super(component, schema);
    }

    @Override
    protected void doExecute(Map<String, ?> parameters) {
        ((ComponentNode) getComponent()).log((String) parameters.get("action"), JsonUtils.toJson(parameters));
    }
}
