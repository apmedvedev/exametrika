/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;

import com.exametrika.api.exadb.objectdb.fields.IJsonRecord;


/**
 * The {@link IActionLog} represents an action log node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IActionLog {
    /**
     * Returns log field.
     *
     * @return log field
     */
    Iterable<IJsonRecord> getLog();
}
