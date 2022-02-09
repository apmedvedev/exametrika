/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;

import java.util.Set;


/**
 * The {@link ISecurityService} represents a security service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISecurityService {
    String NAME = "system.SecurityService";

    /**
     * Returns audit log.
     *
     * @return audit log
     */
    IAuditLog getAuditLog();

    /**
     * Returns list of users.
     *
     * @return list of users
     */
    Iterable<IUser> getUsers();

    /**
     * Finds user by name.
     *
     * @param name user name
     * @return user or null if user is not found
     */
    IUser findUser(String name);

    /**
     * Adds user.
     *
     * @param name user name
     * @return user
     */
    IUser addUser(String name);

    /**
     * Returns root group.
     *
     * @return root group
     */
    IUserGroup getRootGroup();


    /**
     * Returns user group by group identifier (fully qualified name).
     *
     * @param groupId group identifier
     * @return group or null if group is not found
     */
    IUserGroup findUserGroup(String groupId);

    /**
     * Finds subjects for specified role taking into account given permission to specified object.
     *
     * @param roleName   role name
     * @param permission permission. Can be null if not used
     * @param object     object. Can be null if not used
     * @return list of subjects
     */
    Set<ISubject> findSubjects(String roleName, IPermission permission, Object object);

    /**
     * Returns current session.
     *
     * @return current session or null if session is not started.
     */
    ISession getSession();

    /**
     * Returns current secured transaction.
     *
     * @return current secured transaction or null if secured transaction is not started.
     */
    ISecuredTransaction getTransaction();

    /**
     * Performs login for specified user authenticating it.
     *
     * @param userName user name
     * @param password password
     * @return session
     */
    ISession login(String userName, String password);

    /**
     * Performs login for specified user.
     *
     * @param userName user name
     * @return session
     */
    ISession login(String userName);
}
