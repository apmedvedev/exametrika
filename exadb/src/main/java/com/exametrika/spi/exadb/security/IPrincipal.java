/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.security;

import com.exametrika.api.exadb.security.IUser;


/**
 * The {@link IPrincipal} represents a principal - authenticated user.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPrincipal {
    /**
     * Returns user.
     *
     * @return user
     */
    IUser getUser();

    /**
     * Is principal administrator?
     *
     * @return true if principal is administrator
     */
    boolean isAdministrator();

    /**
     * Does principal have specified role?
     *
     * @param roleName role name
     * @return true if principal have specified role
     */
    boolean hasRole(String roleName);
}
