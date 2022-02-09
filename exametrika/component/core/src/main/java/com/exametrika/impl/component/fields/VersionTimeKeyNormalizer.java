/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.fields;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link VersionTimeKeyNormalizer} is a configuration of version time key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionTimeKeyNormalizer implements IKeyNormalizer<VersionTime> {
    @Override
    public ByteArray normalize(VersionTime key) {
        byte[] buffer = new byte[16];
        Indexes.normalizeLong(buffer, 0, key.getNodeId());
        Indexes.normalizeLong(buffer, 8, key.getTime());
        return new ByteArray(buffer);
    }
}