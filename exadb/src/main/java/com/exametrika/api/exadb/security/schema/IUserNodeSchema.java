/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.schema;

import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link IUserNodeSchema} represents a schema for user node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IUserNodeSchema extends IObjectNodeSchema {
    /**
     * Creates a hash of given password.
     *
     * @param password    password
     * @param credentials existing credentials if user password is verified or null if new user password is set
     * @return password hash
     */
    ByteArray createPasswordHash(String password, ByteArray credentials);
}
