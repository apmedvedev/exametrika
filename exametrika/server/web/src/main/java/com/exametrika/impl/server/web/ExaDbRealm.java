/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web;

import java.security.Principal;

import org.apache.catalina.Wrapper;
import org.apache.catalina.realm.RealmBase;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.api.server.IServerService;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ExaDbRealm} is a realm base on exadb security.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaDbRealm extends RealmBase {
    private IServerService serverService;

    public ExaDbRealm(IServerService serverService) {
        Assert.notNull(serverService);

        this.serverService = serverService;
    }

    @Override
    protected String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Principal authenticate(final String username, final String credentials) {
        final ISession[] session = new ISession[1];
        try {
            serverService.getDatabase().transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    try {
                        ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                        session[0] = securityService.login(username, credentials);
                    } catch (Exception e) {
                    }
                }
            });

            if (session[0] != null)
                return new ExaDbPrincipal(username, session[0]);
            else
                return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean hasRole(Wrapper wrapper, Principal principal, String role) {
        return true;
    }

    @Override
    protected Principal getPrincipal(final String username) {
        final ISession[] session = new ISession[1];
        try {
            serverService.getDatabase().transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                    session[0] = securityService.login(username);
                }
            });

            return new ExaDbPrincipal(username, session[0]);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected String getPassword(String username) {
        return null;
    }
}