/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.IRole;
import com.exametrika.api.exadb.security.ISecuredTransaction;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.api.exadb.security.SecuredOperation;
import com.exametrika.api.exadb.security.config.SecurityServiceConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.impl.exadb.security.LoginOperation;
import com.exametrika.impl.exadb.security.SecuredTransaction;
import com.exametrika.impl.exadb.security.SecurityService;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link SecurityTests} are tests for component model.
 *
 * @author Medvedev-A
 */
public class SecurityTests {
    private IDatabaseFactory.Parameters parameters;
    private DatabaseConfiguration configuration;
    private Database database;
    private File tempDir;

    @Before
    public void setUp() {
        Times.setTest(0);
        tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(10);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(100000000)).toConfiguration());
        builder.addDomainService(new SecurityServiceConfiguration(600000, 1000));
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
        Numbers.clearTest();
        Times.clearTest();
    }

    @Test
    public void testUsers() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JsonObject metadata = Json.object().put("key1", "value1").toObject();
                List<String> labels = Arrays.asList("label1", "label2");

                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser admin = securityService.findUser("Admin");
                IUser user1 = securityService.addUser("user1");
                user1.setDescription("User1");
                user1.setMetadata(metadata);
                user1.setLabels(labels);
                IUser user2 = securityService.addUser("user2");

                IUserGroup group1 = securityService.getRootGroup().addChild("group1");
                group1.setDescription("Group1");
                group1.setMetadata(metadata);
                group1.setLabels(labels);

                IUserGroup group2 = securityService.getRootGroup().addChild("group2");

                IUserGroup group11 = group1.addChild("group11");
                IUserGroup group12 = group1.addChild("group12");
                group1.addUser(user1);
                group1.addUser(user2);

                group11.addUser(user1);
                group11.addUser(user2);

                group12.addUser(user1);
                group12.addUser(user2);

                IUserGroup group21 = group2.addChild("group21");
                IUserGroup group22 = group2.addChild("group22");
                group2.addUser(user1);
                group2.addUser(user2);

                group21.addUser(user1);
                group21.addUser(user2);

                group22.addUser(user1);
                group22.addUser(user2);

                IRole groupRole11 = group1.addRole("role1");

                groupRole11.setMetadata(metadata);
                assertThat(groupRole11.getName(), is("role1"));
                assertThat(groupRole11.getMetadata(), is(metadata));
                assertThat(groupRole11.getSubject() == group1, is(true));

                IRole groupRole12 = group1.addRole("role2");
                assertThat(groupRole12.getName(), is("role2"));
                assertThat(groupRole12.getMetadata(), nullValue());

                IRole groupRole21 = group2.addRole("role1");
                IRole groupRole22 = group2.addRole("role2");

                IRole userRole11 = user1.addRole("role1");
                IRole userRole12 = user1.addRole("role2");

                IRole userRole21 = user2.addRole("role1");
                IRole userRole22 = user2.addRole("role2");

                assertThat(userRole11.getSubject() == user1, is(true));

                assertThat(user1.getName(), is("user1"));
                assertThat(user1.getDescription(), is("User1"));
                assertThat(user1.getMetadata(), is(metadata));
                assertThat(user1.getLabels(), is(labels));
                assertThat(Collections.toList(user1.getGroups().iterator()), is((List) Arrays.asList(group1, group11, group12, group2, group21, group22)));
                assertThat(user1.findGroup("group1") == group1, is(true));
                assertThat(Collections.toList(user1.getRoles().iterator()), is((List) Arrays.asList(userRole11, userRole12)));
                assertThat(user1.findRole("role1") == userRole11, is(true));
                user1.setPassword("Hellow world! 123");
                assertThat(user1.checkPassword("Hellow world! 123"), is(true));
                assertThat(user1.checkPassword("test"), is(false));

                assertThat(Collections.toList(user2.getRoles().iterator()), is((List) Arrays.asList(userRole21, userRole22)));

                assertThat(group1.getGroupId(), is("group1"));
                assertThat(group1.getName(), is("group1"));
                assertThat(group1.getDescription(), is("Group1"));
                assertThat(group1.getMetadata(), is(metadata));
                assertThat(group1.getLabels(), is(labels));
                assertThat(Collections.toList(group1.getChildren().iterator()), is((List) Arrays.asList(group11, group12)));
                assertThat(group1.findChild("group11") == group11, is(true));
                assertThat(Collections.toList(group1.getRoles().iterator()), is((List) Arrays.asList(groupRole11, groupRole12)));
                assertThat(group1.findRole("role1") == groupRole11, is(true));
                assertThat(group12.getParent() == group1, is(true));
                assertThat(group1.getParent(), is(securityService.getRootGroup()));
                assertThat(Collections.toList(group2.getRoles().iterator()), is((List) Arrays.asList(groupRole21, groupRole22)));
                assertThat(group11.getGroupId(), is("group1.group11"));

                assertThat(Collections.toList(securityService.getUsers().iterator()), is((List) Arrays.asList(user2, user1, admin)));
                assertThat(Collections.toList(securityService.getRootGroup().getChildren().iterator()), is((List) Arrays.asList(group1, group2)));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser admin = securityService.findUser("Admin");
                IUser user1 = securityService.findUser("user1");
                IUser user2 = securityService.findUser("user2");

                IUserGroup group1 = securityService.getRootGroup().findChild("group1");
                IUserGroup group2 = securityService.getRootGroup().findChild("group2");

                IUserGroup group11 = group1.findChild("group11");
                IUserGroup group12 = group1.findChild("group12");
                IUserGroup group21 = group2.findChild("group21");
                IUserGroup group22 = group2.findChild("group22");

                IRole groupRole11 = group1.findRole("role1");
                IRole groupRole12 = group1.findRole("role2");
                IRole groupRole21 = group2.findRole("role1");
                IRole groupRole22 = group2.findRole("role2");

                IRole userRole11 = user1.findRole("role1");
                IRole userRole12 = user1.findRole("role2");

                IRole userRole21 = user2.findRole("role1");
                IRole userRole22 = user2.findRole("role2");

                assertThat(securityService.findUserGroup("group1") == group1, is(true));
                assertThat(securityService.findUserGroup("group1.group11") == group11, is(true));
                assertThat(securityService.findUserGroup("group1.group12") == group12, is(true));

                group2.delete();
                group1.removeChild("group12");
                group1.removeRole("role1");
                groupRole12.delete();

                user2.delete();
                user1.removeRole("role1");

                assertThat(securityService.findUserGroup("group1.group12"), nullValue());

                assertThat(Collections.toList(securityService.getUsers().iterator()), is((List) Arrays.asList(user1, admin)));
                assertThat(Collections.toList(securityService.getRootGroup().getChildren().iterator()), is((List) Arrays.asList(group1)));
                assertThat(((INodeObject) user2).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) group2).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) group12).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) group21).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) group22).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) groupRole11).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) groupRole12).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) groupRole21).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) groupRole22).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) userRole11).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) userRole21).getNode().isDeleted(), is(true));
                assertThat(((INodeObject) userRole22).getNode().isDeleted(), is(true));

                assertThat(Collections.toList(user1.getGroups().iterator()), is((List) Arrays.asList(group1, group11)));
                assertThat(Collections.toList(group1.getChildren().iterator()), is((List) Arrays.asList(group11)));
                assertThat(Collections.toList(group1.getUsers().iterator()), is((List) Arrays.asList(user1)));
                assertThat(Collections.toList(group11.getUsers().iterator()), is((List) Arrays.asList(user1)));
                assertThat(Collections.toList(group1.getRoles().iterator()), is((List) Arrays.asList()));
                assertThat(Collections.toList(user1.getRoles().iterator()), is((List) Arrays.asList(userRole12)));
            }
        });

        checkDump("users", "users", Arrays.asList("system-security-13.json"));
    }

    @Test
    public void testUserPerformance() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        int COUNT = 10000;
        long t = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            final int k = i;
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                    IUser user1 = securityService.addUser("user" + k);
                    user1.setPassword("password" + k);
                }
            });
        }

        System.out.println("Add users:" + (System.currentTimeMillis() - t));

        for (int i = 0; i < COUNT; i++) {
            final int k = i;
            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                    securityService.login("user" + k, "password" + k);
                }
            });
        }

        System.out.println("Logins:" + (System.currentTimeMillis() - t));
    }

    @Test
    public void testSessionManagement() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.setPassword("user1");
                IUser user2 = securityService.addUser("user2");
                user2.setPassword("user2");
                IUser admin = securityService.addUser("admin");
                admin.addRole("admin");
                admin.setPassword("admin");
            }
        });

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new LoginOperation("user2", "test") {
                    @Override
                    protected void onLogin(ISession session) {
                        out[0] = session;
                    }
                });
            }
        });

        database.transactionSync(new LoginOperation("admin", "admin") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session2 = out[0];

        assertThat(session1 != null, is(true));
        assertThat(session1.isOpened(), is(true));
        assertThat(session2 != null, is(true));
        assertThat(session2.isOpened(), is(true));

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                session1.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                    }
                });
            }
        });

        session2.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                SecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                assertThat(Collections.toList(securityService.getSessionManager().getSessions().values().iterator()),
                        is((List) Arrays.asList(session1, session2)));
            }
        });

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction securedTransaction) {
                assertThat(securedTransaction.getSession() == session1, is(true));
                assertThat(securedTransaction.findDomainService(ISecurityService.NAME), nullValue());
                assertThat(securedTransaction.findDomainService("test.testSecuredService") instanceof TestSecuredDomainService, is(true));

                ITransaction transaction = ((SecuredTransaction) securedTransaction).getTransaction();
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                assertThat(securityService.getTransaction() == securedTransaction, is(true));
                assertThat(securityService.getSession() == session1, is(true));
            }
        });

        session1.close();
        Threads.sleep(100);

        session2.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                SecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                assertThat(Collections.toList(securityService.getSessionManager().getSessions().values().iterator()), is((List) Arrays.asList(session2)));
                assertThat(securityService.getSession() == session2, is(true));
                assertThat(securityService.getTransaction(), nullValue());
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                assertThat(securityService.getTransaction(), nullValue());
                assertThat(securityService.getSession(), nullValue());
            }
        });

        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session3 = out[0];
        assertThat(session3.isOpened(), is(true));

        session2.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                SecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                securityService.findUser("user1").delete();
            }
        });

        assertThat(session3.isOpened(), is(false));
        session2.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                SecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                assertThat(Collections.toList(securityService.getSessionManager().getSessions().values().iterator()), is((List) Arrays.asList(session2)));
            }
        });

        database.transactionSync(new LoginOperation("admin", "admin") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        database.flush();
        final ISession session4 = out[0];
        assertThat(session4.isOpened(), is(true));
        Times.setTest(1000000);
        Threads.sleep(300);
        assertThat(session4.isOpened(), is(false));

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                SecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                assertThat(Collections.toList(securityService.getSessionManager().getSessions().values().iterator()), is((List) Arrays.asList()));
            }
        });

        checkDump("sessions1", "sessions1", Arrays.asList("system-security-13.json"));
    }

    @Test
    public void testRoleMapping() throws Throwable {
        Times.setTest(0);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JsonObject metadata = Json.object().putObject("schedule").put("dateFormat", "dd.MM.yyyy").put("timeFormat", "HH:mm:ss")
                        .put("expression", "time(03:00:00..03:00:01)").end().toObject();

                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.setPassword("user1");
                IUser admin = securityService.addUser("admin");
                admin.addRole("admin");
                admin.setPassword("admin");

                IUserGroup group1 = securityService.getRootGroup().addChild("group1");
                IUserGroup group2 = securityService.getRootGroup().addChild("group2");
                IUserGroup group11 = group1.addChild("group11");

                group1.addUser(user1);
                group11.addUser(user1);
                group2.addUser(user1);

                IRole role = user1.addRole("role1");
                role.setMetadata(metadata);
                group1.addRole("role2");
                group2.addRole("role3");
                group11.addRole("role4");
            }
        });

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];

        database.transactionSync(new LoginOperation("admin", "admin") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session2 = out[0];

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                assertThat(transaction.getPrincipal().getUser().getName(), is("user1"));
                assertThat(transaction.getPrincipal().hasRole("admin"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role1"), is(true));
                assertThat(transaction.getPrincipal().hasRole("role2"), is(true));
                assertThat(transaction.getPrincipal().hasRole("role3"), is(true));
                assertThat(transaction.getPrincipal().hasRole("role4"), is(true));
            }
        });

        session2.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                assertThat(transaction.getPrincipal().getUser().getName(), is("admin"));
                assertThat(transaction.getPrincipal().hasRole("admin"), is(true));
                assertThat(transaction.getPrincipal().hasRole("role1"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role2"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role3"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role4"), is(false));
            }
        });

        Times.setTest(2000);
        database.onTimer(2000);
        Threads.sleep(300);

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                assertThat(transaction.getPrincipal().getUser().getName(), is("user1"));
                assertThat(transaction.getPrincipal().hasRole("admin"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role1"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role2"), is(true));
                assertThat(transaction.getPrincipal().hasRole("role3"), is(true));
                assertThat(transaction.getPrincipal().hasRole("role4"), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.findUser("user1");

                IUserGroup group1 = securityService.getRootGroup().findChild("group1");
                IUserGroup group2 = securityService.getRootGroup().findChild("group2");
                IUserGroup group11 = group1.findChild("group11");

                group1.removeUser("user1");
                group11.removeUser("user1");
                group2.removeUser("user1");

                user1.removeRole("role1");
            }
        });

        Times.setTest(4000);
        Threads.sleep(300);

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                assertThat(transaction.getPrincipal().getUser().getName(), is("user1"));
                assertThat(transaction.getPrincipal().hasRole("admin"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role1"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role2"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role3"), is(false));
                assertThat(transaction.getPrincipal().hasRole("role4"), is(false));
            }
        });

        checkDump("sessions2", "sessions2", Arrays.asList("system-security-13.json"));
    }

    @Test
    public void testReverseRoleMapping() throws Throwable {
        Times.setTest(0);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                JsonObject metadata = Json.object().putObject("schedule").put("dateFormat", "dd.MM.yyyy").put("timeFormat", "HH:mm:ss")
                        .put("expression", "time(03:00:00..03:00:01)").end().toObject();

                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.setPassword("user1");
                user1.setLabels(java.util.Collections.singletonList("nodes"));
                IUser admin = securityService.addUser("admin");
                admin.addRole("admin");
                admin.setPassword("admin");

                IUserGroup group1 = securityService.getRootGroup().addChild("group1");
                group1.setLabels(java.util.Collections.singletonList("nodes"));
                IUserGroup group2 = securityService.getRootGroup().addChild("group2");
                group2.setLabels(java.util.Collections.singletonList("nodes"));
                IUserGroup adminGroup = securityService.getRootGroup().addChild("adminGroup");
                IUserGroup group11 = group1.addChild("group11");
                group11.setLabels(java.util.Collections.singletonList("nodes"));

                group1.addUser(user1);
                group11.addUser(user1);
                group2.addUser(user1);

                IRole role = user1.addRole("role1");
                role.setMetadata(metadata);
                user1.addRole("role4");
                group1.addRole("role2");
                group2.addRole("role3");
                group11.addRole("role4");
                adminGroup.addRole("admin");

                IObjectSpaceSchema spaceSchema = transaction.getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");
                TestNode node = spaceSchema.getSpace().createNode("nodes.node1", schema);

                assertThat(securityService.findSubjects("admin", schema.permission1, node), is(Collections.asSet(adminGroup, admin)));
                assertThat(securityService.findSubjects("role2", schema.permission1, node), is((Set) Collections.asSet()));

                assertThat(securityService.findSubjects("role4", schema.permission4, node), is((Set) Collections.asSet(user1, group11)));
                assertThat(securityService.findSubjects("role2", schema.permission2, node), is((Set) Collections.asSet(group1)));
                group1.setLabels(java.util.Collections.<String>emptyList());
                assertThat(securityService.findSubjects("role2", schema.permission2, node), is((Set) Collections.asSet(user1, group11)));
                user1.setLabels(java.util.Collections.<String>emptyList());
                group11.setLabels(java.util.Collections.<String>emptyList());
                assertThat(securityService.findSubjects("role2", schema.permission2, node), is((Set) Collections.asSet(user1)));

                user1.setLabels(java.util.Collections.singletonList("nodes"));
                assertThat(securityService.findSubjects("role1", schema.permission1, node), is((Set) Collections.asSet(user1)));
                Times.setTest(2000);
                assertThat(securityService.findSubjects("role1", schema.permission1, node), is((Set) Collections.asSet()));
            }
        });
    }

    @Test
    public void testPermissionMapping() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.addRole("role5");
                user1.setPassword("user1");
                user1.setLabels(Arrays.asList("nodes."));

                IUser admin = securityService.addUser("admin");
                admin.addRole("admin");
                admin.setPassword("admin");
            }
        });

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                assertThat(schema.permission1.isAccessAllowed(null), is(false));
                assertThat(schema.permission7.isAccessAllowed(null), is(false));
                assertThat(schema.permission5.isAccessAllowed(null), is(true));
                assertThat(schema.permission6.isAccessAllowed(null), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.findUser("user1");
                user1.removeAllRoles();
                user1.addRole("role6");
            }
        });

        Times.setTest(10000);
        Threads.sleep(300);

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                assertThat(schema.permission1.isAccessAllowed(null), is(true));
                assertThat(schema.permission2.isAccessAllowed(null), is(true));
                assertThat(schema.permission7.isAccessAllowed(null), is(false));
                assertThat(schema.permission5.isAccessAllowed(null), is(true));
                assertThat(schema.permission6.isAccessAllowed(null), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.findUser("user1");
                user1.removeAllRoles();
                user1.addRole("role7");
            }
        });

        Times.setTest(20000);
        Threads.sleep(300);

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                assertThat(schema.permission1.isAccessAllowed(null), is(true));
                assertThat(schema.permission2.isAccessAllowed(null), is(true));
                assertThat(schema.permission7.isAccessAllowed(null), is(false));
                assertThat(schema.permission5.isAccessAllowed(null), is(true));
                assertThat(schema.permission6.isAccessAllowed(null), is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.findUser("user1");
                user1.removeAllRoles();
                user1.addRole("role8");
            }
        });

        Times.setTest(30000);
        Threads.sleep(300);

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                assertThat(schema.permission1.isAccessAllowed(null), is(true));
                assertThat(schema.permission2.isAccessAllowed(null), is(true));
                assertThat(schema.permission7.isAccessAllowed(null), is(true));
                assertThat(schema.permission5.isAccessAllowed(null), is(true));
                assertThat(schema.permission6.isAccessAllowed(null), is(true));
            }
        });

        createDatabase("config2.conf");

        Times.setTest(40000);
        Threads.sleep(300);

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                assertThat(schema.permission1.isAccessAllowed(null), is(false));
                assertThat(schema.permission2.isAccessAllowed(null), is(false));
                assertThat(schema.permission7.isAccessAllowed(null), is(true));
                assertThat(schema.permission5.isAccessAllowed(null), is(false));
                assertThat(schema.permission6.isAccessAllowed(null), is(false));
            }
        });
    }

    @Test
    public void testPatternCheckPermissionStrategy() throws Throwable {
        Times.setTest(1000);
        createDatabase("config3.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.addRole("role1");
                user1.setPassword("user1");
                user1.setLabels(Arrays.asList("domain*:action1:node1"));

                IUser user2 = securityService.addUser("user2");
                user2.addRole("role1");
                user2.setPassword("user2");
                user2.setLabels(Arrays.asList("domain*:action2:node2"));

                IUserGroup group = securityService.getRootGroup().addChild("group");
                group.addUser(user1);
            }
        });

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];
        database.transactionSync(new LoginOperation("user2", "user2") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session2 = out[0];

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");
                TestNode node1 = spaceSchema.getSpace().createNode("node1", schema);
                TestNode node2 = spaceSchema.getSpace().createNode("node2", schema);

                assertThat(schema.permission1.isAccessAllowed(node1), is(true));
                assertThat(schema.permission2.isAccessAllowed(node1), is(false));

                assertThat(schema.permission1.isAccessAllowed(node2), is(false));
                assertThat(schema.permission2.isAccessAllowed(node2), is(false));

                ISecurityService securityService = service.getTransaction().findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.findUser("user1");
                user1.setLabels(java.util.Collections.<String>emptyList());

                assertThat(schema.permission1.isAccessAllowed(node1), is(false));

                IUserGroup group = securityService.findUserGroup("group");
                group.setLabels(Arrays.asList("domain*:action1:node1"));

                assertThat(schema.permission1.isAccessAllowed(node1), is(true));
                assertThat(schema.permission2.isAccessAllowed(node1), is(false));

                assertThat(schema.permission1.isAccessAllowed(node2), is(false));
                assertThat(schema.permission2.isAccessAllowed(node2), is(false));
            }
        });

        session2.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");
                TestNode node1 = spaceSchema.getSpace().findNode("node1", schema);
                TestNode node2 = spaceSchema.getSpace().findNode("node2", schema);

                assertThat(schema.permission1.isAccessAllowed(node1), is(false));
                assertThat(schema.permission2.isAccessAllowed(node1), is(false));

                assertThat(schema.permission1.isAccessAllowed(node2), is(false));
                assertThat(schema.permission2.isAccessAllowed(node2), is(true));
            }
        });
    }

    @Test
    public void testPermissionChecks() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.addRole("role1");
                user1.setPassword("user1");
                user1.setLabels(Arrays.asList("nodes."));

                IUser admin = securityService.addUser("admin");
                admin.addRole("admin");
                admin.setPassword("admin");
            }
        });

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];

        database.transactionSync(new LoginOperation("admin", "admin") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session2 = out[0];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema spaceSchema = transaction.getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                TestNode node = spaceSchema.getSpace().createNode("node1", schema);
                schema.permission1.check(null);
                schema.permission2.check(node);
                schema.permission3.check(null);
                schema.permission4.check(node);
            }
        });

        session2.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IObjectSpaceSchema spaceSchema = transaction.getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                TestNode node = spaceSchema.getSpace().createNode("node2", schema);
                schema.permission1.check(null);
                schema.permission2.check(node);
                schema.permission3.check(null);
                schema.permission4.check(node);
            }
        });

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                final TestNodeSchema schema = spaceSchema.findNode("node1");

                schema.permission1.check(null);
                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            schema.permission2.check(null);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                final TestNodeSchema schema = spaceSchema.findNode("node1");

                transaction.runPrivileged(new Runnable() {
                    @Override
                    public void run() {
                        schema.permission2.check(null);
                    }
                });
                assertThat(transaction.runPrivileged(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        schema.permission2.check(null);
                        return true;
                    }
                }), is(true));

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            schema.permission2.check(null);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                final TestNodeSchema schema = spaceSchema.findNode("node1");

                ((SecuredTransaction) transaction).beginEntry();
                schema.permission2.beginCheck(null);
                schema.permission2.endCheck();
                ((SecuredTransaction) transaction).endEntry();

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            schema.permission2.check(null);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                final TestNodeSchema schema = spaceSchema.findNode("node1");
                final TestNode node = spaceSchema.getSpace().findNode("node1", schema);
                final TestNode node2 = spaceSchema.getSpace().createNode("nodes.node1", schema);

                schema.permission1.check(null);
                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            schema.permission1.check(node);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
                schema.permission1.check(node2);
            }
        });

        session2.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                final TestNodeSchema schema = spaceSchema.findNode("node1");
                final TestNode node = spaceSchema.getSpace().findNode("node1", schema);
                final TestNode node2 = spaceSchema.getSpace().findNode("nodes.node1", schema);

                schema.permission1.check(null);
                schema.permission1.check(node);
                schema.permission1.check(node2);
                schema.permission2.check(null);
                schema.permission2.check(node2);
            }
        });
    }

    @Test
    public void testAudit() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.setPassword("user1");
                IUser admin = securityService.addUser("admin");
                admin.addRole("admin");
                admin.setPassword("admin");
            }
        });

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];

        database.transactionSync(new LoginOperation("admin", "admin") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session2 = out[0];

        new Expected(RawDatabaseException.class, new Runnable() {
            @Override
            public void run() {
                database.transactionSync(new LoginOperation("admin", "test") {
                    @Override
                    protected void onLogin(ISession session) {
                    }
                });
            }
        });

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                final TestNodeSchema schema = spaceSchema.findNode("node1");
                final TestNode node = spaceSchema.getSpace().createNode("test", schema);

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            schema.permission1.check(null);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            schema.permission1.check(node);
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        session1.close();

        session2.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                TestSecuredDomainService service = transaction.findDomainService("test.testSecuredService");
                IObjectSpaceSchema spaceSchema = service.getTransaction().getCurrentSchema().findSchemaById("space:test.space1");
                TestNodeSchema schema = spaceSchema.findNode("node1");

                TestNode node = spaceSchema.getSpace().findNode("test", schema);
                schema.permission1.check(null);
                schema.permission2.check(node);
                schema.permission3.check(null);
                schema.permission4.check(node);
            }
        });

        Times.setTest(3000);
        Threads.sleep(500);
        IOs.close(database);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
        checkDump("audit", "audit", Arrays.asList("system-security-13.json"));
    }

    private void createDatabase(String schemaConfigName) {
        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final String schemaResourcePath = resourcePath + schemaConfigName;

        NodeSchemaConfiguration nodeConfiguration1 = new TestNodeSchemaConfiguration("node1", Arrays.asList(
                new IndexedStringFieldSchemaConfiguration("field", true, 256)));
        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", "",
                new HashSet(Arrays.asList(nodeConfiguration1)), null, 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)),
                Collections.asSet(new TestSecuredDomainServiceSchemaConfiguration("testSecuredService")));

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModules(schemaResourcePath, null);
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });
    }

    private void checkDump(String dumpDirectoryPath, String ethalonDirectoryPath, List<String> resultFileNames) {
        database.getOperations().dump(new File(tempDir, dumpDirectoryPath).getPath(), new DumpContext(IDumpContext.DUMP_ORPHANED, null), null);

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        boolean failed = false;
        for (String resultFileName : resultFileNames)
            failed = !compare(tempDir, dumpDirectoryPath, ethalonDirectoryPath, resourcePath, resultFileName, resultFileName) || failed;

        if (failed)
            assertTrue(false);
    }

    private boolean compare(File baseDir, String dumpDirectoryPath, String ethalonDirectoryPath, final String resourcePath,
                            String resultFileName, String ethalonFileName) {
        JsonObject result = JsonSerializers.load(new File(baseDir, dumpDirectoryPath + File.separator + resultFileName).getPath(), false);
        JsonObject ethalon = JsonSerializers.load(resourcePath + ethalonDirectoryPath + "/" + ethalonFileName, false);
        if (!result.equals(ethalon)) {
            System.out.println("result: " + resultFileName);
            System.out.println(new JsonDiff(true).diff(result, ethalon));
            return false;
        }

        return true;
    }

    public static class TestNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
        public TestNodeSchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields) {
            super(name, fields);
        }

        public TestNodeSchemaConfiguration(String name, String alias, String desciption, List<? extends FieldSchemaConfiguration> fields) {
            super(name, alias, desciption, fields, null);
        }

        @Override
        public INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            return new TestNodeSchema(this, index, fields, documentSchema);
        }

        @Override
        public INodeObject createNode(INode node) {
            return new TestNode(node);
        }
    }

    public static class TestNodeSchema extends ObjectNodeSchema {
        private IPermission permission1;
        private IPermission permission2;
        private IPermission permission3;
        private IPermission permission4;
        private IPermission permission5;
        private IPermission permission6;
        private IPermission permission7;

        public TestNodeSchema(ObjectNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields, IDocumentSchema documentSchema) {
            super(configuration, index, fields, documentSchema);
        }

        @Override
        public void resolveDependencies() {
            super.resolveDependencies();

            permission1 = Permissions.permission(this, "domain1:action1", true);
            permission2 = Permissions.permission(this, "domain1:action2", true);
            permission3 = Permissions.permission(this, "domain2:action1", false);
            permission4 = Permissions.permission(this, "domain2:action2", false);
            permission5 = Permissions.permission(this, "domains.domain1:actions.action1:instance1", false);
            permission6 = Permissions.permission(this, "domains.domain1:action2:instance1", false);
            permission7 = Permissions.permission(this, "test", false);
        }
    }

    public static class TestNode extends ObjectNodeObject {
        public TestNode(INode node) {
            super(node);
        }
    }

    public static class TestSecuredDomainServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
        public TestSecuredDomainServiceSchemaConfiguration(String name) {
            super(name);
        }

        @Override
        public boolean isSecured() {
            return true;
        }

        @Override
        public IDomainService createService() {
            return new TestSecuredDomainService();
        }
    }

    private static class TestSecuredDomainService extends DomainService {
        public ITransaction getTransaction() {
            return context.getTransactionProvider().getTransaction();
        }
    }
}
