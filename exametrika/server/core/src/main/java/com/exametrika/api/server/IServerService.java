/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.server;

import com.exametrika.api.exadb.core.IDatabase;


/**
 * The {@link IServerService} represents a server service.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IServerService {
    final String NAME = "server";

    /**
     * Returns database;
     *
     * @return database
     */
    IDatabase getDatabase();
}
