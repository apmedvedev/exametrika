/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeNonUniqueSortedIndex;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.IAuditLog;
import com.exametrika.api.exadb.security.IAuditRecord;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.IRole;
import com.exametrika.api.exadb.security.ISecuredTransaction;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.api.exadb.security.config.SecurityServiceConfiguration;
import com.exametrika.api.exadb.security.config.model.RoleSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.BitArray;
import com.exametrika.common.utils.Strings;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.security.model.SecurityRootNode;
import com.exametrika.impl.exadb.security.schema.SecurityServiceSchema;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;
import com.exametrika.spi.exadb.security.IRoleMappingStrategy;


/**
 * The {@link SecurityService} is a security service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecurityService extends DomainService implements ISecurityService {
    private SecurityServiceConfiguration configuration = new SecurityServiceConfiguration();
    private IObjectSpaceSchema spaceSchema;
    private INodeSchema userSchema;
    private INodeSchema userGroupSchema;
    private IFieldSchema roleNameSchema;
    private List<Permission> permissions = new ArrayList<Permission>();
    private Map<String, BitArray> rolePermissions = new HashMap<String, BitArray>();
    private SessionManager sessionManager = new SessionManager();
    private long lastUpdatePermissionsTime = Times.getCurrentTime();
    private boolean updatePermissions;
    private Deque<IAuditRecord> auditQueue = new ArrayDeque<IAuditRecord>();

    public SecurityService() {
        sessionManager.setSessionTimeoutPeriod(configuration.getSessionTimeoutPeriod());
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setUpdatePermissions() {
        this.updatePermissions = true;
    }

    public void closeUserSessions(IUser user) {
        for (Session session : sessionManager.getSessions().values()) {
            Principal principal = session.getPrincipal();
            if (principal.getUser() == user)
                session.close();
        }
    }

    @Override
    public SecurityServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public SecurityServiceSchema getSchema() {
        return (SecurityServiceSchema) schema;
    }

    @Override
    public void setConfiguration(DomainServiceConfiguration configuration, boolean clearCache) {
        if (configuration == null)
            configuration = new SecurityServiceConfiguration();

        this.configuration = (SecurityServiceConfiguration) configuration;
        sessionManager.setSessionTimeoutPeriod(this.configuration.getSessionTimeoutPeriod());
    }

    @Override
    public void setSchema(IDomainServiceSchema schema) {
        super.setSchema(schema);

        SecurityServiceSchema securityServiceSchema = (SecurityServiceSchema) schema;
        permissions = securityServiceSchema.getPermissions();
        for (Permission permission : permissions)
            permission.init(this);

        rolePermissions.clear();

        for (RoleSchemaConfiguration role : getSchema().getConfiguration().getSecurityModel().getRoles()) {
            if (role.isAdministrator()) {
                rolePermissions.put(role.getName(), null);
                continue;
            }

            List<PermissionPattern> patterns = new ArrayList<PermissionPattern>();
            for (String permissionPattern : role.getPermissionPatterns())
                patterns.add(PermissionPattern.parse(permissionPattern));

            BitArray roleMask = new BitArray(permissions.size());
            for (int i = 0; i < permissions.size(); i++) {
                Permission permission = permissions.get(i);
                boolean match = false;
                for (int k = 0; k < patterns.size(); k++) {
                    PermissionPattern pattern = patterns.get(k);
                    if (pattern.match(permission.getLevels())) {
                        match = true;
                        break;
                    }
                }

                if (match)
                    roleMask.set(i);
            }

            rolePermissions.put(role.getName(), roleMask);
        }

        updatePrincipalPermissions();
        lastUpdatePermissionsTime = Times.getCurrentTime();
    }

    @Override
    public IAuditLog getAuditLog() {
        ensureSpace();

        SecurityRootNode root = ((SecurityRootNode) spaceSchema.getSpace().getRootNode());
        return (IAuditLog) root.getAuditLogField().getRecords();
    }

    public boolean isAuditEnabled() {
        return getSchema().getConfiguration().getSecurityModel().isAuditEnabled();
    }

    public void addAuditRecord(IAuditRecord record) {
        if (!isAuditEnabled())
            return;

        auditQueue.addLast(record);
    }

    @Override
    public Iterable<IUser> getUsers() {
        ensureSpace();

        return spaceSchema.getSpace().getNodes(userSchema);
    }

    @Override
    public IUser findUser(String name) {
        ensureSpace();

        INodeIndex<String, IUser> index = spaceSchema.getSpace().getIndex(userSchema.getPrimaryField());
        return index.find(name);
    }

    @Override
    public IUser addUser(String name) {
        Assert.isTrue(!Strings.isEmpty(name));

        ensureSpace();

        return spaceSchema.getSpace().createNode(name, userSchema);
    }

    @Override
    public IUserGroup getRootGroup() {
        ensureSpace();

        return ((SecurityRootNode) spaceSchema.getSpace().getRootNode()).getRootGroup();
    }

    @Override
    public IUserGroup findUserGroup(String id) {
        ensureSpace();

        INodeIndex<String, IUserGroup> index = spaceSchema.getSpace().getIndex(userGroupSchema.getPrimaryField());
        return index.find(id);
    }

    @Override
    public Set<ISubject> findSubjects(String roleName, IPermission permission, Object object) {
        ensureSpace();

        Assert.notNull(roleName);
        boolean administrator = false;
        if (permission != null) {
            if (!rolePermissions.containsKey(roleName))
                return Collections.emptySet();
            BitArray roleMask = rolePermissions.get(roleName);
            if (roleMask == null)
                administrator = true;
            else if (!roleMask.get(permission.getIndex()))
                return Collections.emptySet();
        }

        Set<ISubject> subjects = new LinkedHashSet<ISubject>();
        SecurityServiceSchema schema = getSchema();
        INodeNonUniqueSortedIndex<String, IRole> index = spaceSchema.getSpace().getIndex(roleNameSchema);
        for (IRole role : index.findValues(roleName)) {
            if (schema.getRoleMappingStrategy() != null && !schema.getRoleMappingStrategy().isSubjectInRole(role))
                continue;

            ISubject subject = role.getSubject();
            if (!administrator && permission != null && object != null && schema.getCheckPermissionStrategy() != null)
                buildSubjects(permission, schema, object, subject, subjects);
            else
                subjects.add(subject);
        }

        return subjects;
    }

    @Override
    public ISession getSession() {
        return sessionManager.getCurrentSession();
    }

    @Override
    public ISecuredTransaction getTransaction() {
        Session session = sessionManager.getCurrentSession();
        if (session == null)
            return null;
        else
            return session.getTransaction();
    }

    @Override
    public ISession login(String userName, String password) {
        return createSession(userName, password);
    }

    @Override
    public ISession login(String userName) {
        return createSession(userName, null);
    }

    @Override
    public void onTimer(long currentTime) {
        sessionManager.onTimer(currentTime);

        if (updatePermissions ||
                (getSchema().getRoleMappingStrategy() != null && currentTime > lastUpdatePermissionsTime + configuration.getRoleMappingUpdatePeriod())) {
            updatePrincipalPermissions();
            lastUpdatePermissionsTime = currentTime;
        }

        writeAuditRecords();
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
        userSchema = null;
        userGroupSchema = null;
        roleNameSchema = null;
        sessionManager.clearCaches();
    }

    @Override
    public void stop() {
        sessionManager.close();
    }

    private ISession createSession(String userName, String password) {
        IUser user = findUser(userName);
        Assert.checkState(user != null);
        if (password != null) {
            boolean result = user.checkPassword(password);
            if (!result && isAuditEnabled())
                addAuditRecord(new AuditRecord(user.getName(), "session:open", null, Times.getCurrentTime(), false));
            Assert.checkState(result);
        }

        Set<String> roles = mapRoles(user);
        BitArray permissionMask = buildPermissionMask(roles);

        Session session = new Session((Database) context.getDatabase(), this, new Principal(user, roles, permissionMask, this));
        sessionManager.addSession(session);

        if (isAuditEnabled())
            addAuditRecord(new AuditRecord(user.getName(), "session:open", null, Times.getCurrentTime(), true));

        return session;
    }

    private Set<String> mapRoles(IUser user) {
        Set<String> roles = new HashSet<String>();
        buildRoles(getSchema().getRoleMappingStrategy(), user, roles);

        return roles;
    }

    private void buildRoles(IRoleMappingStrategy roleMappingStrategy, ISubject subject, Set<String> roles) {
        if (roleMappingStrategy != null)
            roles.addAll(roleMappingStrategy.getRoles(subject));
        else {
            for (IRole role : subject.getRoles())
                roles.add(role.getName());
        }

        if (subject instanceof IUser) {
            for (IUserGroup group : ((IUser) subject).getGroups())
                buildRoles(roleMappingStrategy, group, roles);
        } else {
            IUserGroup parent = ((IUserGroup) subject).getParent();
            if (parent != null)
                buildRoles(roleMappingStrategy, parent, roles);
        }
    }

    private BitArray buildPermissionMask(Set<String> roles) {
        BitArray mask = new BitArray(permissions.size());
        for (String role : roles) {
            if (!rolePermissions.containsKey(role))
                continue;

            BitArray roleMask = rolePermissions.get(role);
            if (roleMask == null)
                return null;

            mask.or(roleMask);
        }

        return mask;
    }

    private void buildSubjects(IPermission permission, SecurityServiceSchema schema, Object object, ISubject subject, Set<ISubject> subjects) {
        if (schema.getCheckPermissionStrategy().check(permission, object, subject))
            subjects.add(subject);
        else if (subject instanceof IUserGroup) {
            IUserGroup group = (IUserGroup) subject;
            for (IUserGroup child : group.getChildren())
                buildSubjects(permission, schema, object, child, subjects);
            for (IUser user : group.getUsers())
                buildSubjects(permission, schema, object, user, subjects);
        }
    }

    private void updatePrincipalPermissions() {
        for (Session session : sessionManager.getSessions().values()) {
            Principal principal = session.getPrincipal();

            Set<String> roles = mapRoles(principal.getUser());
            BitArray permissionMask = buildPermissionMask(roles);
            principal.setPermissionMask(roles, permissionMask);
        }

        updatePermissions = false;
    }

    private void writeAuditRecords() {
        if (auditQueue.isEmpty())
            return;

        ensureSpace();

        while (!auditQueue.isEmpty()) {
            IAuditRecord record = auditQueue.removeFirst();
            SecurityRootNode root = ((SecurityRootNode) spaceSchema.getSpace().getRootNode());
            IStructuredBlobField<IAuditRecord> log = root.getAuditLogField();
            log.add(record);
        }
    }

    private void ensureSpace() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().findSpace("security");
            Assert.notNull(spaceSchema);

            userSchema = spaceSchema.findNode("User");
            Assert.notNull(userSchema);

            userGroupSchema = spaceSchema.findNode("UserGroup");
            Assert.notNull(userGroupSchema);

            INodeSchema roleSchema = spaceSchema.findNode("Role");
            Assert.notNull(roleSchema);

            roleNameSchema = roleSchema.findField("name");
            Assert.notNull(roleNameSchema);
        }
    }
}
