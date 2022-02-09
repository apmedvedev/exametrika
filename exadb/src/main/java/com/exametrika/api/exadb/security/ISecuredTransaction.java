/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import java.util.concurrent.Callable;

import com.exametrika.spi.exadb.security.IPrincipal;


/**
 * The {@link ISecuredTransaction} represents a secured transaction.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISecuredTransaction {
    /**
     * Is transaction read-only?
     *
     * @return true if transaction read-only
     */
    boolean isReadOnly();

    /**
     * Returns transaction options.
     *
     * @return transaction options
     */
    int getOptions();

    /**
     * Returns active transaction operation.
     *
     * @return active transaction operation
     */
    <T> T getOperation();

    /**
     * Returns session.
     *
     * @return session
     */
    ISession getSession();

    /**
     * Returns principal.
     *
     * @return principal
     */
    IPrincipal getPrincipal();

    /**
     * Finds secured domain service by fully qualified name.
     *
     * @param name fully qualified name of domain service
     * @return domain service or null if domain service is not found
     */
    <T> T findDomainService(String name);

    /**
     * Runs specified operation in privileged mode, where all authorization checks are disabled.
     *
     * @param operation operation
     * @return operation result
     */
    <T> T runPrivileged(Callable<T> operation);

    /**
     * Runs specified operation in privileged mode, where all authorization checks are disabled.
     *
     * @param operation operation
     */
    void runPrivileged(Runnable operation);
}
