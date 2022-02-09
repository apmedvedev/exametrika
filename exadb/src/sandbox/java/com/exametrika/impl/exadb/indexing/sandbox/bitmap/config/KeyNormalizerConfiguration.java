/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap.config;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.common.config.Configuration;


/**
 * The {@link KeyNormalizerConfiguration} is a configuration of key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class KeyNormalizerConfiguration extends Configuration {
    public abstract IKeyNormalizer createKeyNormalizer();
}
