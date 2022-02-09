/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.config;

import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link IndexConfigurationLoader} is a loader of {@link IndexDatabaseExtensionConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("IndexDatabaseExtension")) {
            long maxIndexIdlePeriod = element.get("maxIndexIdlePeriod");
            FullTextIndexConfiguration fullTextIndex = loadFullTextIndex((JsonObject) element.get("fullTextIndex"));

            return new IndexDatabaseExtensionConfiguration(maxIndexIdlePeriod, fullTextIndex);
        } else
            throw new InvalidConfigurationException();
    }

    private FullTextIndexConfiguration loadFullTextIndex(JsonObject element) {
        long writerCommitPeriod = element.get("writerCommitPeriod");
        long searcherUpdatePeriod = element.get("searcherUpdatePeriod");
        long indexDeleteDelay = element.get("indexDeleteDelay");
        long bufferSizePerIndex = element.get("bufferSizePerIndex");

        return new FullTextIndexConfiguration(writerCommitPeriod, searcherUpdatePeriod, indexDeleteDelay, bufferSizePerIndex);
    }
}