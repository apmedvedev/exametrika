/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.Set;

import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.BitArray;
import com.exametrika.spi.exadb.security.IPrincipal;


/**
 * The {@link Principal} is a principal - authenticated user.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Principal implements IPrincipal {
    private final String userName;
    private final ISecurityService securityService;
    private IUser user;
    private Set<String> roles;
    private BitArray permissionMask;

    public Principal(IUser user, Set<String> roles, BitArray permissionMask, ISecurityService securityService) {
        Assert.notNull(user);
        Assert.notNull(roles);
        Assert.notNull(securityService);

        this.userName = user.getName();
        this.user = user;
        this.roles = roles;
        this.permissionMask = permissionMask;
        this.securityService = securityService;
    }

    @Override
    public IUser getUser() {
        if (user != null)
            return user;
        else
            return refreshUser();
    }

    @Override
    public boolean isAdministrator() {
        return permissionMask == null;
    }

    @Override
    public boolean hasRole(String roleName) {
        return roles.contains(roleName);
    }

    public boolean hasPermission(int index) {
        if (permissionMask == null)
            return true;
        else
            return permissionMask.get(index);
    }

    public void setPermissionMask(Set<String> roles, BitArray permissionMask) {
        Assert.notNull(roles);

        this.roles = roles;
        this.permissionMask = permissionMask;
    }

    @Override
    public String toString() {
        return userName;
    }

    public void clearCache() {
        user = null;
    }

    private IUser refreshUser() {
        user = securityService.findUser(userName);
        Assert.notNull(user);
        return user;
    }
}
