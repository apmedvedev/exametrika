/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web;

import java.security.Principal;

import com.exametrika.api.exadb.security.ISession;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ExaDbPrincipal} is a principal base on exadb security.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaDbPrincipal implements Principal {
    private final String name;
    private final ISession session;

    public ExaDbPrincipal(String name, ISession session) {
        Assert.notNull(name);
        Assert.notNull(session);

        this.name = name;
        this.session = session;
    }

    public ISession getSession() {
        return session;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}