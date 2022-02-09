/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import com.exametrika.common.utils.ByteArray;


/**
 * The {@link IUser} represents a user.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IUser extends ISubject {
    /**
     * Returns groups.
     *
     * @return groups
     */
    Iterable<IUserGroup> getGroups();

    /**
     * Finds group by name.
     *
     * @param name group name
     * @return group or null if group is not found
     */
    IUserGroup findGroup(String name);

    /**
     * Returns user credentials.
     *
     * @return user credentials
     */
    ByteArray getCredentials();

    /**
     * Sets user password.
     *
     * @param value user password
     */
    void setPassword(String value);

    /**
     * Checks password.
     *
     * @param password password
     * @return true if check is succeeded
     */
    boolean checkPassword(String password);
}
