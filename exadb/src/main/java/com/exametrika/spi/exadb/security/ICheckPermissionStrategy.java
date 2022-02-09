/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.security;

import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.ISubject;


/**
 * The {@link ICheckPermissionStrategy} represents a dynamic check permission strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICheckPermissionStrategy {
    /**
     * Checks access to specified object from specified subject.
     *
     * @param permission permission
     * @param object     object
     * @param subject    subject
     * @return true if access to specified object from specified subject is allowed
     */
    boolean check(IPermission permission, Object object, ISubject subject);
}
