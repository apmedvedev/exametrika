/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security;


/**
 * The {@link IUserGroup} represents a user group.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IUserGroup extends ISubject {
    /**
     * Returns unique identifier of group (fully qualified name).
     *
     * @return unique identifier of group (fully qualified name)
     */
    String getGroupId();

    /**
     * Returns parent group.
     *
     * @return parent group or null if this group does not have parent
     */
    IUserGroup getParent();

    /**
     * Returns children groups.
     *
     * @return children groups
     */
    Iterable<IUserGroup> getChildren();

    /**
     * Finds child group by name.
     *
     * @param name group name
     * @return group or null if group is not found
     */
    IUserGroup findChild(String name);

    /**
     * Adds child group.
     *
     * @param name group name
     * @return group
     */
    IUserGroup addChild(String name);

    /**
     * Removes child group.
     *
     * @param name group name
     */
    void removeChild(String name);

    /**
     * Removes all children.
     */
    void removeAllChildren();

    /**
     * Returns users.
     *
     * @return users
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
     * @param user user
     */
    void addUser(IUser user);

    /**
     * Removes user.
     *
     * @param name user name
     */
    void removeUser(String name);

    /**
     * Removes all users.
     */
    void removeAllUsers();
}
