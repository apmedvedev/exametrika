/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.common.utils.Assert;


/**
 * The {@link LoginOperation} is an implementation of login operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class LoginOperation extends Operation {
    private final String userName;
    private final String password;

    public LoginOperation(String userName, String password) {
        Assert.notNull(userName);
        Assert.notNull(password);

        this.userName = userName;
        this.password = password;
    }

    public LoginOperation(String userName) {
        Assert.notNull(userName);

        this.userName = userName;
        this.password = null;
    }

    @Override
    public void run(ITransaction transaction) {
        ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);

        if (password != null)
            onLogin(securityService.login(userName, password));
        else
            onLogin(securityService.login(userName));
    }

    protected abstract void onLogin(ISession session);
}
