/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.security;

import java.util.Set;

import com.exametrika.api.exadb.security.IRole;
import com.exametrika.api.exadb.security.ISubject;


/**
 * The {@link IRoleMappingStrategy} represents a dynamic role mapping strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IRoleMappingStrategy {
    /**
     * Returns subject roles (only roles of subject itself are examined).
     *
     * @param subject subject
     * @return subject role names
     */
    Set<String> getRoles(ISubject subject);

    /**
     * Is subject in given role (only roles of subject itself are examined)?
     *
     * @param role subject role
     * @return true if subject is in given role
     */
    boolean isSubjectInRole(IRole role);
}
