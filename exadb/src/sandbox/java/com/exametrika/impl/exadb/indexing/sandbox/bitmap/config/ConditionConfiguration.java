/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.ICondition;


/**
 * The {@link ConditionConfiguration} is a configuration of condition.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ConditionConfiguration extends Configuration {
    public abstract ICondition createCondition();
}
