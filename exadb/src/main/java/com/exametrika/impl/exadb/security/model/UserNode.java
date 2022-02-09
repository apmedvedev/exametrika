/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.model;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.api.exadb.security.schema.IUserNodeSchema;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.security.SecurityService;


/**
 * The {@link UserNode} is a user.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class UserNode extends SubjectNode implements IUser {
    private static final int GROUPS_FIELD = 5;
    private static final int CREDENTIALS_FIELD = 6;

    public UserNode(INode node) {
        super(node);
    }

    @Override
    public Iterable<IUserGroup> getGroups() {
        IReferenceField<IUserGroup> field = getField(GROUPS_FIELD);
        return field;
    }

    @Override
    public IUserGroup findGroup(String name) {
        IReferenceField<IUserGroup> field = getField(GROUPS_FIELD);
        for (IUserGroup group : field) {
            if (group.getName().equals(name))
                return group;
        }

        return null;
    }

    public void addGroup(IUserGroup group) {
        IReferenceField<IUserGroup> field = getField(GROUPS_FIELD);
        field.add(group);
    }

    public void removeGroup(IUserGroup group) {
        IReferenceField<IUserGroup> field = getField(GROUPS_FIELD);
        field.remove(group);
    }

    @Override
    public ByteArray getCredentials() {
        ISerializableField<ByteArray> field = getField(CREDENTIALS_FIELD);
        return field.get();
    }

    @Override
    public void setPassword(String password) {
        IUserNodeSchema schema = (IUserNodeSchema) getSchema();
        ByteArray value = schema.createPasswordHash(password, null);

        ISerializableField<ByteArray> field = getField(CREDENTIALS_FIELD);
        field.set(value);
    }

    @Override
    public boolean checkPassword(String password) {
        IUserNodeSchema schema = (IUserNodeSchema) getSchema();

        ByteArray creadentials = getCredentials();
        ByteArray value = schema.createPasswordHash(password, creadentials);

        return value.equals(creadentials);
    }

    @Override
    public void delete() {
        closeUserSessions();

        super.delete();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonGroups = false;
        for (IUserGroup group : getGroups()) {
            if (!jsonGroups) {
                json.key("groups");
                json.startArray();
                jsonGroups = true;
            }

            json.value(group.getName());
        }

        if (jsonGroups)
            json.endArray();
    }

    protected void closeUserSessions() {
        SecurityService securityService = getTransaction().findDomainService(ISecurityService.NAME);
        securityService.closeUserSessions(this);
    }
}