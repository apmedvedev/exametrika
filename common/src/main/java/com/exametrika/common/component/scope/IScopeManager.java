/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

/**
 * The {@link IScopeManager} is a manager of scopes of particular type.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IScopeManager {
    /**
     * Returns a scope attached to current thread.
     *
     * @return current thread's scope
     * @throws MissingScopeException if scope is not attached to current thread
     */
    IScope getScope();

    /**
     * Attaches a scope to current thread.
     *
     * @param scope scope to attach
     * @throws ScopeAlreadyAttachedException if another scope already attached to current thread
     */
    void attach(IScope scope);

    /**
     * Detaches a scope from current thread (if any).
     *
     * @return detached scope or null if there are no attached scope.
     */
    IScope detach();
}
