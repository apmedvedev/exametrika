/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.model;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link UserGroupNode} is a user group.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class UserGroupNode extends SubjectNode implements IUserGroup {
    protected static final int ID_FIELD = 5;
    private static final int PARENT_FIELD = 6;
    private static final int CHILDREN_FIELD = 7;
    private static final int USERS_FIELD = 8;

    public UserGroupNode(INode node) {
        super(node);
    }

    @Override
    public String getGroupId() {
        return (String) getKey();
    }

    @Override
    public IUserGroup getParent() {
        ISingleReferenceField<IUserGroup> field = getField(PARENT_FIELD);
        return field.get();
    }

    @Override
    public Iterable<IUserGroup> getChildren() {
        IReferenceField<IUserGroup> field = getField(CHILDREN_FIELD);
        return field;
    }

    @Override
    public IUserGroup findChild(String name) {
        IReferenceField<IUserGroup> field = getField(CHILDREN_FIELD);
        for (IUserGroup group : field) {
            if (group.getName().equals(name))
                return group;
        }

        return null;
    }

    @Override
    public IUserGroup addChild(String name) {
        Assert.isTrue(!Strings.isEmpty(name));

        IUserGroup child = findChild(name);
        if (child != null)
            return child;

        String groupId = getGroupId();
        String key;
        if (!groupId.isEmpty())
            key = groupId + "." + name;
        else
            key = name;

        child = getSpace().createNode(key, getSpace().getSchema().findNode("UserGroup"), name);

        IReferenceField<IUserGroup> field = getField(CHILDREN_FIELD);
        field.add(child);

        ISingleReferenceField<IUserGroup> parentField = ((UserGroupNode) child).getField(PARENT_FIELD);
        parentField.set(this);

        updatePermissions();

        return child;
    }

    @Override
    public void removeChild(String name) {
        IUserGroup group = findChild(name);
        if (group == null)
            return;

        IReferenceField<IUserGroup> field = getField(CHILDREN_FIELD);
        field.remove(group);
        group.delete();

        updatePermissions();
    }

    @Override
    public void removeAllChildren() {
        IReferenceField<IUserGroup> field = getField(CHILDREN_FIELD);
        for (IUserGroup group : field)
            group.delete();

        field.clear();

        updatePermissions();
    }

    @Override
    public Iterable<IUser> getUsers() {
        IReferenceField<IUser> field = getField(USERS_FIELD);
        return field;
    }

    @Override
    public IUser findUser(String name) {
        IReferenceField<IUser> field = getField(USERS_FIELD);
        for (IUser user : field) {
            if (user.getName().equals(name))
                return user;
        }

        return null;
    }

    @Override
    public void addUser(IUser user) {
        IUser foundUser = findUser(user.getName());
        if (foundUser != null)
            return;

        IReferenceField<IUser> field = getField(USERS_FIELD);
        field.add(user);

        ((UserNode) user).addGroup(this);

        updatePermissions();
    }

    @Override
    public void removeUser(String name) {
        IUser user = findUser(name);
        if (user == null)
            return;

        IReferenceField<IUser> field = getField(USERS_FIELD);
        field.remove(user);
        ((UserNode) user).removeGroup(this);

        updatePermissions();
    }

    @Override
    public void removeAllUsers() {
        IReferenceField<UserNode> field = getField(USERS_FIELD);
        for (UserNode user : field)
            user.removeGroup(this);

        field.clear();

        updatePermissions();
    }

    @Override
    public void delete() {
        for (IUserGroup group : getChildren())
            group.delete();

        super.delete();

        updatePermissions();
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
        IStringField field = getField(NAME_FIELD);
        field.set((String) args[0]);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonChildren = false;
        for (IUserGroup group : getChildren()) {
            if (!jsonChildren) {
                json.key("children");
                json.startObject();
                jsonChildren = true;
            }

            json.key(group.getName());
            json.startObject();
            ((INodeObject) group).dump(json, context);
            json.endObject();
        }

        if (jsonChildren)
            json.endObject();

        boolean jsonUsers = false;
        for (IUser user : getUsers()) {
            if (!jsonUsers) {
                json.key("users");
                json.startArray();
                jsonUsers = true;
            }

            json.value(user.getName());
        }

        if (jsonUsers)
            json.endArray();
    }
}