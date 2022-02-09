/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.component;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.component.IAction;
import com.exametrika.api.component.IAsyncAction;
import com.exametrika.api.component.config.model.ExpressionComplexRuleSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComponentJobOperationSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionHealthCheckSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionSimpleRuleSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.INodeComponentVersion;
import com.exametrika.api.exadb.core.BatchOperation;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobService;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.Tasks;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Serializers;
import com.exametrika.common.utils.TestMode;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.AggregationService;
import com.exametrika.impl.aggregator.AggregationService.EntryPointHierarchy;
import com.exametrika.impl.aggregator.ClosePeriodBatchOperation;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.RuleContext;
import com.exametrika.impl.aggregator.RuleContext.RuleExecutorInfo;
import com.exametrika.impl.aggregator.nodes.SecondaryEntryPointNode;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.HostComponentNode;
import com.exametrika.impl.component.nodes.HostComponentVersionNode;
import com.exametrika.impl.component.nodes.JobProxy;
import com.exametrika.impl.component.nodes.NodeComponentNode;
import com.exametrika.impl.component.operations.TimeSnapshotOperation;
import com.exametrika.impl.component.schema.ComponentNodeSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.core.tx.DbBatchOperation;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.ScopeHierarchy;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.spi.component.IAgentFailureListener;
import com.exametrika.spi.component.IRule;
import com.exametrika.spi.component.config.model.AsyncActionParameterDefinitionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AsyncActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * The {@link ComponentTests} are tests for component model.
 *
 * @author Medvedev-A
 */
public class ComponentTests {
    private IDatabaseFactory.Parameters parameters;
    private DatabaseConfiguration configuration;
    private Database database;
    private File tempDir;
    private TestAgentActionExecutor actionExecutor = new TestAgentActionExecutor();
    private TestAgentFailureDetector failureDetector = new TestAgentFailureDetector();

    private static class ActionTaskHandler implements ITaskHandler<ActionTask>, ITaskListener<ActionTask> {
        @Override
        public void onTaskStarted(ActionTask task) {
        }

        @Override
        public void onTaskCompleted(ActionTask task) {
            task.completionHandler.onSucceeded(task.result);
        }

        @Override
        public void onTaskFailed(ActionTask task, Throwable error) {
            task.completionHandler.onFailed(error);
        }

        @Override
        public void handle(ActionTask task) {
            try {
                task.result = task.callable.call();
            } catch (Exception e) {
                Exceptions.wrapAndThrow(e);
            }
        }
    }

    public static class TestAgentActionExecutor implements IAgentActionExecutor {
        private TaskQueue<ActionTask> queue = new TaskQueue<ActionTask>();
        private TaskExecutor executor;

        public TestAgentActionExecutor() {
            ActionTaskHandler handler = new ActionTaskHandler();
            executor = new TaskExecutor(5, queue, handler, "test action executor");
            executor.addTaskListener(handler);
            executor.start();
        }

        @Override
        public <T> void execute(String agentId, Object action, ICompletionHandler<T> completionHandler) {
            queue.offer(new ActionTask(agentId, (Callable) action, completionHandler));
        }
    }

    private static class TestAction implements Callable {
        private final Map<String, Object> parameters;

        public TestAction(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        @Override
        public Object call() throws Exception {
            if (!parameters.containsKey("error"))
                return parameters.toString();
            else
                throw new RuntimeException((String) parameters.get("error"));
        }
    }

    @Before
    public void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(10);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(100000000)).toConfiguration());
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put(IAgentActionExecutor.NAME, actionExecutor);
        parameters.parameters.put(IAgentFailureDetector.NAME, failureDetector);
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
    public void testVersions() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.createNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getScope() == Names.getScope("host1"), is(true));
                assertThat(host1.getCurrentVersion() != null, is(true));
                assertThat(host1.getCurrentVersion() == host1.get(), is(true));
                HostComponentVersionNode version1 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version1.isReadOnly(), is(false));
                assertThat(version1.isDeleted(), is(false));
                assertThat(version1.getTime(), is(1000l));
                assertThat(version1.getPreviousVersion(), nullValue());
                assertThat(host1.addVersion() == version1, is(true));

                IHostComponent host2 = space.createNode(nameManager.addName(Names.getScope("host2")).getId(), schema.findNode("Host"));

                INodeComponent node1 = space.createNode(nameManager.addName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                INodeComponent node2 = space.createNode(nameManager.addName(Names.getScope("node2")).getId(), schema.findNode("Node"));

                ComponentRootNode root = space.getRootNode();
                IGroupComponent group1 = space.createNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("Group"));
                root.getRootGroup().addChild(group1);
                IGroupComponent group2 = space.createNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                group1.addChild(group2);

                IGroupComponent aggregationGroup1 = space.createNode(nameManager.addName(Names.getScope("aggregationGroup1")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponent aggregationGroup2 = space.createNode(nameManager.addName(Names.getScope("aggregationGroup2")).getId(),
                        schema.findNode("AggregationGroup"));
                group2.addChild(aggregationGroup1);
                group2.addChild(aggregationGroup2);

                aggregationGroup1.addComponent(host1);
                aggregationGroup2.addComponent(host1);
                group1.addComponent(host1);
                aggregationGroup1.addComponent(host2);

                aggregationGroup2.addComponent(node1);
                aggregationGroup2.addComponent(node2);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                Times.setTest(2000);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getScope() == Names.getScope("host1"), is(true));
                assertThat(host1.getCurrentVersion() != null, is(true));
                assertThat(host1.getCurrentVersion() == host1.get(), is(true));
                final HostComponentVersionNode version1 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version1.isReadOnly(), is(true));
                assertThat(version1.isDeleted(), is(false));
                assertThat(version1.getTime(), is(1000l));
                assertThat(version1.getPreviousVersion(), nullValue());
                assertThat(version1.getComponent() == host1, is(true));

                host1.setOptions(Json.object().put("key1", "value1").toObject());
                host1.setProperties(Json.object().put("key2", "value2").toObject());

                HostComponentVersionNode version2 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version2 != version1, is(true));
                assertThat(version2.isReadOnly(), is(false));
                assertThat(version2.isDeleted(), is(false));
                assertThat(version2.getTime(), is(2000l));
                assertThat(version2.getPreviousVersion(), is((IComponentVersion) version1));
                assertThat(version2.getComponent() == host1, is(true));
                assertThat(host1.addVersion() == version2, is(true));

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            version1.setOptions(Json.object().put("key1", "value1").toObject());
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                Times.setTest(3000);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getScope() == Names.getScope("host1"), is(true));
                assertThat(host1.getCurrentVersion() != null, is(true));
                assertThat(host1.getCurrentVersion() == host1.get(), is(true));
                HostComponentVersionNode version2 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version2.isReadOnly(), is(true));
                assertThat(version2.isDeleted(), is(false));
                assertThat(version2.getTime(), is(2000l));
                assertThat(version2.getPreviousVersion().getTime(), is(1000l));
                assertThat(version2.getComponent() == host1, is(true));
                host1.addVersion();

                HostComponentVersionNode version3 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version3.isReadOnly(), is(false));
                assertThat(version3.isDeleted(), is(false));
                assertThat(version3.getTime(), is(3000l));
                assertThat(version3.getPreviousVersion().getTime(), is(2000l));
                assertThat(version3.getOptions(), is(Json.object().put("key1", "value1").toObject()));
                assertThat(version3.getProperties(), is(Json.object().put("key2", "value2").toObject()));
                assertThat(version3.getComponent() == host1, is(true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                Times.setTest(4000);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                host1.delete();
                host1.delete();

                HostComponentVersionNode version4 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version4.getComponent() == host1, is(true));
                assertThat(version4.isReadOnly(), is(false));
                assertThat(version4.isDeleted(), is(true));
                assertThat(version4.getTime(), is(4000l));
                assertThat(version4.getPreviousVersion().getTime(), is(3000l));
                assertThat(version4.getOptions(), nullValue());
                assertThat(version4.getProperties(), nullValue());
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                Times.setTest(5000);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                host1.setOptions(Json.object().put("key1", "value1").toObject());
                host1.setProperties(Json.object().put("key2", "value2").toObject());

                HostComponentVersionNode version5 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version5.isReadOnly(), is(false));
                assertThat(version5.isDeleted(), is(false));
                assertThat(version5.getTime(), is(5000l));
                assertThat(version5.getPreviousVersion().getTime(), is(4000l));
                assertThat(version5.getOptions(), is(Json.object().put("key1", "value1").toObject()));
                assertThat(version5.getProperties(), is(Json.object().put("key2", "value2").toObject()));
                assertThat(version5.getComponent() == host1, is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(0) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get(), nullValue());
            }
        });

        database.transactionSync(new TimeSnapshotOperation(2000) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(2000l));
                assertThat(host1.get() == host1.get(), is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(3999) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(3000l));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(4000) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(4000l));
                assertThat(host1.get().isDeleted(), is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(4999) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(4000l));
                assertThat(host1.get().isDeleted(), is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(10000) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(5000l));
                assertThat(host1.get().isDeleted(), is(false));
            }
        });

        IOs.close(database);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IHostComponent host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));

                HostComponentVersionNode version5 = (HostComponentVersionNode) host1.getCurrentVersion();
                assertThat(version5.isReadOnly(), is(true));
                assertThat(version5.isDeleted(), is(false));
                assertThat(version5.getTime(), is(5000l));
                assertThat(version5.getPreviousVersion().getTime(), is(4000l));
                assertThat(version5.getOptions(), is(Json.object().put("key1", "value1").toObject()));
                assertThat(version5.getProperties(), is(Json.object().put("key2", "value2").toObject()));
                assertThat(version5.getComponent() == host1, is(true));

                HostComponentVersionNode version4 = (HostComponentVersionNode) version5.getPreviousVersion();
                assertThat(version4.getComponent() == host1, is(true));
                assertThat(version4.isReadOnly(), is(true));
                assertThat(version4.isDeleted(), is(true));
                assertThat(version4.getTime(), is(4000l));
                assertThat(version4.getPreviousVersion().getTime(), is(3000l));
                assertThat(version4.getOptions(), nullValue());
                assertThat(version4.getProperties(), nullValue());

                HostComponentVersionNode version3 = (HostComponentVersionNode) version4.getPreviousVersion();
                assertThat(version3.isReadOnly(), is(true));
                assertThat(version3.isDeleted(), is(false));
                assertThat(version3.getTime(), is(3000l));
                assertThat(version3.getPreviousVersion().getTime(), is(2000l));
                assertThat(version3.getOptions(), is(Json.object().put("key1", "value1").toObject()));
                assertThat(version3.getProperties(), is(Json.object().put("key2", "value2").toObject()));
                assertThat(version3.getComponent() == host1, is(true));

                HostComponentVersionNode version2 = (HostComponentVersionNode) version3.getPreviousVersion();
                assertThat(version2.isReadOnly(), is(true));
                assertThat(version2.isDeleted(), is(false));
                assertThat(version2.getTime(), is(2000l));
                assertThat(version2.getComponent() == host1, is(true));

                HostComponentVersionNode version1 = (HostComponentVersionNode) version2.getPreviousVersion();
                assertThat(version1.isReadOnly(), is(true));
                assertThat(version1.isDeleted(), is(false));
                assertThat(version1.getTime(), is(1000l));
                assertThat(version1.getPreviousVersion(), nullValue());
                assertThat(version1.getComponent() == host1, is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(0) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get(), nullValue());
            }
        });

        database.transactionSync(new TimeSnapshotOperation(2000) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(2000l));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(3999) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(3000l));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(4000) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(4000l));
                assertThat(host1.get().isDeleted(), is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(4999) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(4000l));
                assertThat(host1.get().isDeleted(), is(true));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(10000) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                assertThat(host1.getCurrentVersion().getTime(), is(5000l));
                assertThat(host1.get().getTime(), is(5000l));
                assertThat(host1.get().isDeleted(), is(false));
            }
        });

        checkDump("versions", "versions", Arrays.asList("component-component-28.json"));
    }

    @Test
    public void testGroups() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.createNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                HostComponentNode host2 = space.createNode(nameManager.addName(Names.getScope("host2")).getId(), schema.findNode("Host"));

                NodeComponentNode node1 = space.createNode(nameManager.addName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                NodeComponentNode node2 = space.createNode(nameManager.addName(Names.getScope("node2")).getId(), schema.findNode("Node"));

                ComponentRootNode root = space.getRootNode();
                IGroupComponent group1 = space.createNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("Group"));
                root.getRootGroup().addChild(group1);
                IGroupComponent group2 = space.createNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                group1.addChild(group2);
                IGroupComponent group3 = space.createNode(nameManager.addName(Names.getScope("group3")).getId(), schema.findNode("Group"));
                group1.addChild(group3);

                IGroupComponent aggregationGroup1 = space.createNode(nameManager.addName(Names.getScope("aggregationGroup1")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponent aggregationGroup2 = space.createNode(nameManager.addName(Names.getScope("aggregationGroup2")).getId(),
                        schema.findNode("AggregationGroup"));
                group2.addChild(aggregationGroup1);
                group2.addChild(aggregationGroup2);

                aggregationGroup1.addComponent(host1);
                aggregationGroup1.addComponent(host2);
                aggregationGroup2.addComponent(host1);
                aggregationGroup2.addComponent(host2);

                group3.addComponent(host1);
                group3.addComponent(host2);

                aggregationGroup2.addComponent(node1);
                aggregationGroup2.addComponent(node2);

                group3.addComponent(node1);
                group3.addComponent(node2);

                host1.addNode(node1);
                host1.addNode(node2);
            }
        });

        database.transactionSync(new Operation(false) {
            @Override
            public void run(ITransaction transaction) {
                Times.setTest(2000);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion1 = (HostComponentVersionNode) host1.getCurrentVersion();

                HostComponentNode host2 = space.findNode(nameManager.addName(Names.getScope("host2")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion2 = (HostComponentVersionNode) host2.getCurrentVersion();

                NodeComponentNode node1 = space.findNode(nameManager.addName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                INodeComponentVersion nodeVersion1 = (INodeComponentVersion) node1.getCurrentVersion();
                NodeComponentNode node2 = space.findNode(nameManager.addName(Names.getScope("node2")).getId(), schema.findNode("Node"));
                INodeComponentVersion nodeVersion2 = (INodeComponentVersion) node1.getCurrentVersion();

                IGroupComponent group2 = space.findNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                IGroupComponentVersion groupVersion2 = (IGroupComponentVersion) group2.getCurrentVersion();
                IGroupComponent group3 = space.findNode(nameManager.addName(Names.getScope("group3")).getId(), schema.findNode("Group"));

                IGroupComponent aggregationGroup1 = space.findNode(nameManager.addName(Names.getScope("aggregationGroup1")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponent aggregationGroup2 = space.findNode(nameManager.addName(Names.getScope("aggregationGroup2")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponentVersion aggregationGroupVersion2 = (IGroupComponentVersion) aggregationGroup2.getCurrentVersion();

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup1, aggregationGroup2, group3)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                        node1, node2)));

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup1, aggregationGroup2, group3)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                )));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion1.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2, group3)));
                assertThat(nodeVersion1.getHost(), is((IHostComponent) host1));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2, group3)));
                assertThat(nodeVersion2.getHost(), is((IHostComponent) host1));

                assertThat(com.exametrika.common.utils.Collections.toList(aggregationGroupVersion2.getComponents().iterator()),
                        is(Arrays.<IComponent>asList(host1, host2, node1, node2)));
                assertThat(aggregationGroupVersion2.getParent(), is(group2));
                assertThat(com.exametrika.common.utils.Collections.toList(groupVersion2.getChildren().iterator()),
                        is(Arrays.<IGroupComponent>asList(aggregationGroup1, aggregationGroup2)));

                node2.delete();
                aggregationGroup1.delete();
                //aggregationGroup2.removeComponent(host1);
                group2.removeChild(group3);
            }
        });

        database.transactionSync(new Operation(false) {
            @Override
            public void run(ITransaction transaction) {
                Times.setTest(3000);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion1 = (HostComponentVersionNode) host1.getCurrentVersion();

                HostComponentNode host2 = space.findNode(nameManager.addName(Names.getScope("host2")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion2 = (HostComponentVersionNode) host2.getCurrentVersion();

                NodeComponentNode node1 = space.findNode(nameManager.addName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                INodeComponentVersion nodeVersion1 = (INodeComponentVersion) node1.getCurrentVersion();
                INodeComponentVersion nodeVersion2 = (INodeComponentVersion) node1.getCurrentVersion();

                NodeComponentNode node2 = space.findNode(nameManager.addName(Names.getScope("node2")).getId(), schema.findNode("Node"));
                node2.addVersion();

                IGroupComponent group2 = space.findNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                IGroupComponentVersion groupVersion2 = (IGroupComponentVersion) group2.getCurrentVersion();

                IGroupComponent aggregationGroup2 = space.findNode(nameManager.addName(Names.getScope("aggregationGroup2")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponentVersion aggregationGroupVersion2 = (IGroupComponentVersion) aggregationGroup2.getCurrentVersion();

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getGroups().iterator()), is(Arrays.<IGroupComponent>asList(
                        aggregationGroup2)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                        node1)));

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                )));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion1.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2)));
                assertThat(nodeVersion1.getHost(), is((IHostComponent) host1));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2)));

                assertThat(com.exametrika.common.utils.Collections.toList(aggregationGroupVersion2.getComponents().iterator()),
                        is(Arrays.<IComponent>asList(host1, host2, node1)));
                assertThat(aggregationGroupVersion2.getParent(), is(group2));
                assertThat(com.exametrika.common.utils.Collections.toList(groupVersion2.getChildren().iterator()),
                        is(Arrays.<IGroupComponent>asList(aggregationGroup2)));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(1500) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion1 = (HostComponentVersionNode) host1.get();

                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("host2")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion2 = (HostComponentVersionNode) host2.get();

                NodeComponentNode node1 = space.findNode(nameManager.findByName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                INodeComponentVersion nodeVersion1 = (INodeComponentVersion) node1.get();
                NodeComponentNode node2 = space.findNode(nameManager.findByName(Names.getScope("node2")).getId(), schema.findNode("Node"));
                INodeComponentVersion nodeVersion2 = (INodeComponentVersion) node1.get();

                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                IGroupComponentVersion groupVersion2 = (IGroupComponentVersion) group2.get();
                IGroupComponent group3 = space.findNode(nameManager.findByName(Names.getScope("group3")).getId(), schema.findNode("Group"));

                IGroupComponent aggregationGroup1 = space.findNode(nameManager.findByName(Names.getScope("aggregationGroup1")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponent aggregationGroup2 = space.findNode(nameManager.findByName(Names.getScope("aggregationGroup2")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponentVersion aggregationGroupVersion2 = (IGroupComponentVersion) aggregationGroup2.get();

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup1, aggregationGroup2, group3)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                        node1, node2)));

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup1, aggregationGroup2, group3)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                )));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion1.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2, group3)));
                assertThat(nodeVersion1.getHost(), is((IHostComponent) host1));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2, group3)));
                assertThat(nodeVersion2.getHost(), is((IHostComponent) host1));

                assertThat(com.exametrika.common.utils.Collections.toList(aggregationGroupVersion2.getComponents().iterator()),
                        is(Arrays.<IComponent>asList(host1, host2, node1, node2)));
                assertThat(aggregationGroupVersion2.getParent(), is(group2));
                assertThat(com.exametrika.common.utils.Collections.toList(groupVersion2.getChildren().iterator()),
                        is(Arrays.<IGroupComponent>asList(aggregationGroup1, aggregationGroup2)));
            }
        });

        database.transactionSync(new TimeSnapshotOperation(3500) {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion1 = (HostComponentVersionNode) host1.get();

                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("host2")).getId(), schema.findNode("Host"));
                HostComponentVersionNode hostVersion2 = (HostComponentVersionNode) host2.get();

                NodeComponentNode node1 = space.findNode(nameManager.findByName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                INodeComponentVersion nodeVersion1 = (INodeComponentVersion) node1.get();
                INodeComponentVersion nodeVersion2 = (INodeComponentVersion) node1.get();

                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                IGroupComponentVersion groupVersion2 = (IGroupComponentVersion) group2.get();

                IGroupComponent aggregationGroup2 = space.findNode(nameManager.findByName(Names.getScope("aggregationGroup2")).getId(),
                        schema.findNode("AggregationGroup"));
                IGroupComponentVersion aggregationGroupVersion2 = (IGroupComponentVersion) aggregationGroup2.get();

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getGroups().iterator()),
                        is(Arrays.<IGroupComponent>asList(aggregationGroup2)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion1.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                        node1)));

                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2)));
                assertThat(com.exametrika.common.utils.Collections.toList(hostVersion2.getNodes().iterator()), is(Arrays.<INodeComponent>asList(
                )));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion1.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2)));
                assertThat(nodeVersion1.getHost(), is((IHostComponent) host1));

                assertThat(com.exametrika.common.utils.Collections.toList(nodeVersion2.getGroups().iterator()), is(Arrays.asList(
                        aggregationGroup2)));

                assertThat(com.exametrika.common.utils.Collections.toList(aggregationGroupVersion2.getComponents().iterator()),
                        is(Arrays.<IComponent>asList(host1, host2, node1)));
                assertThat(aggregationGroupVersion2.getParent(), is(group2));
                assertThat(com.exametrika.common.utils.Collections.toList(groupVersion2.getChildren().iterator()),
                        is(Arrays.<IGroupComponent>asList(aggregationGroup2)));
            }
        });

        checkDump("groups", "groups", Arrays.asList("component-component-28.json"));
    }

    @Test
    public void testActions() throws Throwable {
        Times.setTest(1000);
        createDatabase("config1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.createNode(nameManager.addName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                HostComponentNode host2 = space.createNode(nameManager.addName(Names.getScope("host2")).getId(), schema.findNode("Host"));

                NodeComponentNode node1 = space.createNode(nameManager.addName(Names.getScope("node1")).getId(), schema.findNode("Node"));
                NodeComponentNode node2 = space.createNode(nameManager.addName(Names.getScope("node2")).getId(), schema.findNode("Node"));

                ComponentRootNode root = space.getRootNode();
                IGroupComponent group1 = space.createNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));
                root.getRootGroup().addChild(group1);
                IGroupComponent group2 = space.createNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("Group"));
                group1.addChild(group2);

                group2.addComponent(host1);
                group2.addComponent(host2);

                group2.addComponent(node1);
                group2.addComponent(node2);

                host1.setOptions(Json.object().put("option1", "host1-options").toObject());
                host1.setProperties(Json.object().put("property1", "host1-property").toObject());
            }
        });

        final boolean[] res = new boolean[4];

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));
                IGroupComponent group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(), schema.findNode("Group"));

                Times.setTest(2000);

                IAction action1 = host1.createAction("log");
                action1.execute(Collections.singletonMap("action", "action1"));
                long id = host1.logStart("test", Json.object().put("param1", "value1").toObject());
                host1.logSuccess(id, "test", "Succeeded");

                id = host1.logStart("test", Json.object().put("param2", "value2").toObject());
                host1.logError(id, "test", "Failed");

                assertThat(host1.getActionLog() != null, is(true));

                Times.setTest(3000);
                final IAsyncAction action2 = host1.createAction("localHost");
                action2.execute(Collections.<String, Object>emptyMap());
                action2.execute(new MapBuilder<String, Object>()
                        .put("p1", "p1-action")
                        .put("p2", "p2-action")
                        .put("p3", "p3-action")
                        .put("p4", "p4-action")
                        .toMap(), new CompletionHandler<String>() {
                    @Override
                    public void onSucceeded(String result) {
                        assertThat(result != null, is(true));
                        Object data = Tasks.getCurrentThreadData();
                        assertThat(data != null && data instanceof ICompartment, is(true));
                        IDatabaseContext context = ((ObjectSpaceSchema) ((ComponentNode) action2.getComponent()).getSpace().getSchema()).getContext();
                        assertThat(context.getCompartment() == data, is(true));
                        res[0] = true;
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        Assert.error();
                    }
                });

                Times.setTest(4000);

                action2.execute(new MapBuilder<String, Object>()
                        .put("error", "errorMessage")
                        .put("p2", "p2-action")
                        .toMap(), new CompletionHandler<String>() {
                    @Override
                    public void onSucceeded(String result) {
                        Assert.error();
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        assertThat(error instanceof RuntimeException, is(true));
                        assertThat(error.getMessage(), is("errorMessage"));

                        Object data = Tasks.getCurrentThreadData();
                        assertThat(data != null && data instanceof ICompartment, is(true));
                        IDatabaseContext context = ((ObjectSpaceSchema) ((ComponentNode) action2.getComponent()).getSpace().getSchema()).getContext();
                        assertThat(context.getCompartment() == data, is(true));
                        res[1] = true;
                    }
                });

                Times.setTest(5000);
                final IAsyncAction action3 = host1.createAction("remoteHost");
                action2.execute(Collections.<String, Object>emptyMap(), null);
                action3.execute(new MapBuilder<String, Object>()
                        .put("p1", "p1-action")
                        .put("p2", "p2-action")
                        .put("p3", "p3-action")
                        .put("p4", "p4-action")
                        .toMap(), new CompletionHandler<String>() {
                    @Override
                    public void onSucceeded(String result) {
                        assertThat(result != null, is(true));
                        Object data = Tasks.getCurrentThreadData();
                        assertThat(data != null && data instanceof ICompartment, is(true));
                        IDatabaseContext context = ((ObjectSpaceSchema) ((ComponentNode) action3.getComponent()).getSpace().getSchema()).getContext();
                        assertThat(context.getCompartment() == data, is(true));
                        res[2] = true;
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        Assert.error();
                    }
                });

                Times.setTest(6000);
                action3.execute(new MapBuilder<String, Object>()
                        .put("error", "errorMessage")
                        .put("p2", "p2-action")
                        .toMap(), new CompletionHandler<String>() {
                    @Override
                    public void onSucceeded(String result) {
                        Assert.error();
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        assertThat(error instanceof RuntimeException, is(true));
                        assertThat(error.getMessage(), is("errorMessage"));

                        Object data = Tasks.getCurrentThreadData();
                        assertThat(data != null && data instanceof ICompartment, is(true));
                        IDatabaseContext context = ((ObjectSpaceSchema) ((ComponentNode) action3.getComponent()).getSpace().getSchema()).getContext();
                        assertThat(context.getCompartment() == data, is(true));
                        res[3] = true;
                    }
                });

                Times.setTest(7000);
                IAction action4 = group1.createGroupAction("log", true);
                action4.execute(Collections.singletonMap("action", "action4"));
            }
        });

        Threads.sleep(2000);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("host1")).getId(), schema.findNode("Host"));


                INodeSearchResult result = host1.getActionLog().getLogField().getFullTextIndex().search(Queries.expression("time",
                        "time:[1000 TO 3000}").toQuery(host1.getActionLog().getLogField().getSchema().getDocumentSchema()), 1000);
                assertThat(result.getTotalCount(), is(5));
            }
        });

        for (int i = 0; i < res.length; i++)
            assertThat(res[i], is(true));

        checkDump("actions", "actions", Arrays.asList("component-component-28.json"));
    }

    @Test
    public void testBehaviorTypes() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("config1.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "name1.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        checkDump("behavior", "behavior", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json"));
    }

    @Test
    public void testDiscoveryDeletion() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("discovery.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "discovery.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        failureDetector.active = false;

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        checkDump("discovery", "discovery", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json"));
    }

    @Test
    public void testAvailability() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("availability.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "availability.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        Times.setTest(3000);
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");

        Threads.sleep(300);

        Times.setTest(4000);
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");
        failureDetector.listener.onAgentActivated("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(5000);
                space.addPeriod();
            }
        });

        Threads.sleep(300);

        Times.setTest(6000);
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");
        failureDetector.listener.onAgentFailed("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(7000);
                space.addPeriod();
            }
        });

        Threads.sleep(300);

        Times.setTest(8000);
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(8500);
                space.addPeriod();
            }
        });

        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                IObjectSpaceSchema componentSchema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                ComponentNodeSchema nodeSchema = componentSchema.findNode("Host");
                IObjectSpace componentSpace = componentSchema.getSpace();
                INodeIndex<Long, IHostComponent> index = componentSpace.getIndex(nodeSchema.getIndexField());
                IHostComponent component = index.find(nameManager.findByName(Names.getScope("hosts.host1")).getId());
                Times.setTest(9000);
                component.enableMaintenanceMode("Test maintenance.");
                component.enableMaintenanceMode(null);
            }
        });

        Threads.sleep(300);

        Times.setTest(10000);
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema componentSchema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                ComponentNodeSchema nodeSchema = componentSchema.findNode("Host");
                IObjectSpace componentSpace = componentSchema.getSpace();
                INodeIndex<Long, IHostComponent> index = componentSpace.getIndex(nodeSchema.getIndexField());
                IHostComponent component = index.find(nameManager.findByName(Names.getScope("hosts.host1")).getId());
                Times.setTest(11000);
                component.disableMaintenanceMode();
                component.disableMaintenanceMode();
            }
        });

        Threads.sleep(300);

        Times.setTest(12000);
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");

        Threads.sleep(300);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(13000);
                space.addPeriod();
            }
        });

        Threads.sleep(300);

        checkDump("availability", "availability", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json"));
    }

    @Test
    public void testRules() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("rules.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "rules.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));

                ComponentRootNode root = space.getRootNode();
                IGroupComponent group1 = space.createNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));
                root.getRootGroup().addChild(group1);
                IGroupComponent group2 = space.createNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("AggregationGroup"));
                root.getRootGroup().addChild(group2);

                group1.addComponent(host1);
                group2.addComponent(host1);

                ExpressionSimpleRuleSchemaConfiguration rule1 = new ExpressionSimpleRuleSchemaConfiguration("rule1",
                        "component.log('rule1 - ' + measurement.componentType + ':' + measurement.period.type)", true);
                ExpressionSimpleRuleSchemaConfiguration rule2 = new ExpressionSimpleRuleSchemaConfiguration("rule2",
                        "fact('rule2', true)", true);
                ExpressionSimpleRuleSchemaConfiguration rule3 = new ExpressionSimpleRuleSchemaConfiguration("rule3",
                        "fact('rule3', true)", true);
                ExpressionComplexRuleSchemaConfiguration rule4 = new ExpressionComplexRuleSchemaConfiguration("rule4",
                        "if (hasFact('rule2') && hasFact('rule3')) { component.log('rule4')}", true);
                ExpressionSimpleRuleSchemaConfiguration componentTestRule = new ExpressionSimpleRuleSchemaConfiguration("test",
                        "component.log('component.test - ' + measurement.componentType + ':' + measurement.period.type)", true);

                ExpressionHealthCheckSchemaConfiguration check1 = new ExpressionHealthCheckSchemaConfiguration("check1",
                        "component.log('component1.check from: ' + oldState + ' to: ' + newState)", true);

                host1.addRule(rule1);
                host1.addRule(rule2);
                host1.addRule(rule3);
                host1.addRule(rule4);
                host1.addRule(check1);
                host1.addRule(componentTestRule);

                ExpressionSimpleRuleSchemaConfiguration rule5 = new ExpressionSimpleRuleSchemaConfiguration("rule5",
                        "fact('rule5', true)", true);
                ExpressionComplexRuleSchemaConfiguration rule6 = new ExpressionComplexRuleSchemaConfiguration("rule6",
                        "if (hasFact('rule5')) {component.log('rule6')}", true);
                ExpressionSimpleRuleSchemaConfiguration group1TestRule = new ExpressionSimpleRuleSchemaConfiguration("test",
                        "component.log('group1.test - ' + measurement.componentType + ':' + measurement.period.type)", true);
                ExpressionHealthCheckSchemaConfiguration check2 = new ExpressionHealthCheckSchemaConfiguration("check2",
                        "component.log('group1.check from: ' + oldState + ' to: ' + newState)", true);
                group1.addGroupRule(rule5);
                group1.addGroupRule(rule6);
                group1.addGroupRule(group1TestRule);
                group1.addGroupRule(check2);

                ExpressionSimpleRuleSchemaConfiguration rule7 = new ExpressionSimpleRuleSchemaConfiguration("rule7",
                        "fact('rule7', true)", true);
                ExpressionComplexRuleSchemaConfiguration rule8 = new ExpressionComplexRuleSchemaConfiguration("rule8",
                        "component.log('rule8')", true);
                ExpressionSimpleRuleSchemaConfiguration group2TestRule = new ExpressionSimpleRuleSchemaConfiguration("test",
                        "component.log('group2.test - ' + measurement.componentType + ':' + measurement.period.type)", true);
                group2.addGroupRule(rule7);
                group2.addGroupRule(rule8);
                group2.addGroupRule(group2TestRule);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(3000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));

                ExpressionSimpleRuleSchemaConfiguration rule9 = new ExpressionSimpleRuleSchemaConfiguration("rule9",
                        "component.log('rule9 - ' + measurement.componentType + ':' + measurement.period.type)", true);
                group1.addGroupRule(rule9);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(4000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));

                host1.removeRule("rule1");
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(5000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                IGroupComponent group1 = space.findNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));

                group1.removeComponent(host1);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(6000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("AggregationGroup"));

                group2.delete();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(7000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                host1.enableMaintenanceMode("test");
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(8000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                host1.disableMaintenanceMode();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(9000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                host1.addRule(new ExpressionSimpleRuleSchemaConfiguration("rule10",
                        "if (hasMetric('metricType1') && metric('metricType1.std.count') >= 20) { component.log('rule10 - ' + measurement.componentType) }", true));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(10000);
                space.addPeriod();
            }
        });

        checkDump("rules", "rules", Arrays.asList("component-component-39.json", "aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json"));
    }

    @Test
    public void testJobs() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("jobs.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "jobs.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        Times.setTest(3000);
        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                IJob job11 = host1.addJob(new JobSchemaBuilder()
                        .name("hosts.host1.job1")
                        .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('hosts.host1.job1')"))
                        .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"))
                        .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1)).toJob());
                IJob job12 = host1.addJob(new JobSchemaBuilder()
                        .name("hosts.host1.job2")
                        .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('hosts.host1.job2')"))
                        .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"))
                        .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1)).toJob());

                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("hosts.host2")).getId(), schema.findNode("Host"));
                IJob job21 = host2.addJob(new JobSchemaBuilder()
                        .name("hosts.host2.job1")
                        .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('hosts.host2.job1')"))
                        .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"))
                        .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1)).toJob());
                IJob job22 = host2.addJob(new JobSchemaBuilder()
                        .name("hosts.host2.job2")
                        .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('hosts.host2.job2')"))
                        .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"))
                        .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1)).toJob());

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getJobs().iterator()), is(Arrays.asList(job11, job12)));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getJobs().iterator()), is(Arrays.asList(job21, job22)));
                assertThat(host1.findJob("hosts.host1.job1"), is(job11));
                assertThat(host2.findJob("hosts.host2.job1"), is(job21));

                assertThat(host1.addJob(new JobSchemaBuilder()
                        .name("hosts.host1.job1")
                        .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('hosts.host1.job1')"))
                        .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"))
                        .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 2)).toJob()), is(job11));
            }
        });

        Times.setTest(5000);
        Threads.sleep(3000);

        IOs.close(database);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("hosts.host2")).getId(), schema.findNode("Host"));
                IJob job11 = host1.findJob("hosts.host1.job1");
                IJob job12 = host1.findJob("hosts.host1.job2");
                IJob job21 = host2.findJob("hosts.host2.job1");
                IJob job22 = host2.findJob("hosts.host2.job2");

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getJobs().iterator()), is(Arrays.asList(job11, job12)));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getJobs().iterator()), is(Arrays.asList(job21, job22)));

                assertThat(job11.getExecutionCount(), is(1l));
                assertThat(job12.getExecutionCount(), is(1l));
                assertThat(job21.getExecutionCount(), is(1l));
                assertThat(job22.getExecutionCount(), is(1l));

                host1.removeJob("hosts.host1.job1");
                job21.delete();

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getJobs().iterator()), is(Arrays.asList(job12)));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getJobs().iterator()), is(Arrays.asList(job22)));
            }
        });

        Times.setTest(6000);
        Threads.sleep(500);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("hosts.host2")).getId(), schema.findNode("Host"));
                IJob job12 = host1.findJob("hosts.host1.job2");
                IJob job22 = host2.findJob("hosts.host2.job2");

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getJobs().iterator()), is(Arrays.asList(job12)));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getJobs().iterator()), is(Arrays.asList(job22)));

                assertThat(job12.getExecutionCount(), is(2l));
                assertThat(job22.getExecutionCount(), is(2l));

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getActionLog().getLogField().getRecords().iterator()).size(), is(3));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getActionLog().getLogField().getRecords().iterator()).size(), is(3));

                host1.enableMaintenanceMode("test");
                host2.delete();
            }
        });

        Times.setTest(7000);
        Threads.sleep(500);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("hosts.host2")).getId(), schema.findNode("Host"));
                IJob job12 = host1.findJob("hosts.host1.job2");

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getJobs().iterator()), is(Arrays.asList(job12)));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getJobs().iterator()), is(Arrays.<IJob>asList()));

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getActionLog().getLogField().getRecords().iterator()).size(), is(4));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getActionLog().getLogField().getRecords().iterator()).size(), is(3));
                assertThat(job12.getExecutionCount(), is(3l));

                host1.disableMaintenanceMode();
            }
        });

        Times.setTest(8000);
        Threads.sleep(500);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                HostComponentNode host2 = space.findNode(nameManager.findByName(Names.getScope("hosts.host2")).getId(), schema.findNode("Host"));
                JobProxy job12 = host1.findJob("hosts.host1.job2");

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getJobs().iterator()), is(Arrays.<IJob>asList(job12)));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getJobs().iterator()), is(Arrays.<IJob>asList()));

                assertThat(job12.getExecutionCount(), is(4l));

                IJobService jobs = transaction.findDomainService(IJobService.NAME);
                assertThat(com.exametrika.common.utils.Collections.toList(jobs.getJobs().iterator()), is(Arrays.asList(job12.getJob())));

                assertThat(com.exametrika.common.utils.Collections.toList(host1.getActionLog().getLogField().getRecords().iterator()).size(), is(6));
                assertThat(com.exametrika.common.utils.Collections.toList(host2.getActionLog().getLogField().getRecords().iterator()).size(), is(3));
            }
        });
    }

    @Test
    public void testNonStructuredSchemaChange() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("schemaChange1.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "schemaChange1.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        createDatabase("schemaChange2.conf");

        final JsonObject contents2 = (JsonObject) JsonSerializers.load(resourcePath + "schemaChange2.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents2, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        checkDump("schemaChange1", "schemaChange1", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json"));
    }

    @Test
    public void testStructuredSchemaChange() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("schemaChange1.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "schemaChange1.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        createDatabase("schemaChange3.conf");

        final JsonObject contents2 = (JsonObject) JsonSerializers.load(resourcePath + "schemaChange3.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents2, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                space.addPeriod();
            }
        });

        checkDump("schemaChange2", "schemaChange2", Arrays.asList("component-component-69.json", "aggregation-aggregation-p2-58.json"));
    }

    @Test
    public void testBatchSerializers() throws Throwable {
        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final String schemaResourcePath = resourcePath + "serializers.conf";
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModules(schemaResourcePath, null);
            }
        });

        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                IObjectSpaceSchema spaceSchema = database.getContext().getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = spaceSchema.getSpace();
                ComponentNode component = space.createNode(123, spaceSchema.findNode("Transaction"));

                ClosePeriodBatchOperation operation1 = new ClosePeriodBatchOperation();
                DbBatchOperation batchOperation1 = new DbBatchOperation(operation1, true, null);

                ClosePeriodBatchOperation operation2 = new ClosePeriodBatchOperation();
                operation2.setCurrentTime(123);
                operation2.setPeriodType("p2");
                operation2.setCycleSchemaState(1);
                operation2.setAggregationState(2);
                operation2.setAggregationNodeId(3);
                operation2.setInterceptResult(true);
                operation2.setTotalEndNodes(234);
                operation2.setAggregationEndNodes(235);
                operation2.setTotalNodes(236);
                operation2.setAggregationNodes(237);
                operation2.setEndNodes(238);
                operation2.setDerivedNodes(239);
                operation2.setIteration(240);

                RuleContext ruleContext = new RuleContext();
                ruleContext.setFact(component, "fact1", "value1");
                ruleContext.setFact(component, "fact2", "value2");
                operation2.setRuleContext(ruleContext);

                IPeriodSpaceSchema periodSpaceSchema = database.getContext().getSchemaSpace().getCurrentSchema().findSchemaById("space:aggregation.aggregation");
                ICycleSchema cycleSchema = periodSpaceSchema.findCycle("p2");
                IPeriod period = cycleSchema.getCurrentCycle().getSpace().getCurrentPeriod();

                IPeriodNameManager nameManager = database.getContext().findTransactionExtension(IPeriodNameManager.NAME);
                TLongObjectMap<EntryPointHierarchy> hierarchyMap = new TLongObjectHashMap();
                EntryPointHierarchy hierarchy = new EntryPointHierarchy();
                List<IScopeName> scopes = new ArrayList<IScopeName>();
                for (int i = 0; i < 10; i++) {
                    IScopeName scope = Names.getScope("scope" + i);
                    scopes.add(scope);
                    nameManager.addName(scope);
                }
                hierarchy.scopeHierarchy = new ScopeHierarchy(scopes);
                hierarchy.nodeHierarchy = new ArrayList<SecondaryEntryPointNode>();
                for (int i = 0; i < 10; i++)
                    hierarchy.nodeHierarchy.add((SecondaryEntryPointNode) period.createNode(new Location(1000 + i, 0), cycleSchema.findAggregationNode("Transaction2")));

                hierarchyMap.put(12, hierarchy);
                operation2.setHierarchyMap(hierarchyMap);

                DbBatchOperation batchOperation2 = new DbBatchOperation(operation2, true, com.exametrika.common.utils.Collections.asSet(
                        new DbBatchOperation.CacheConstraint("category1", 123), new DbBatchOperation.CacheConstraint("category2", 345)));

                ISerializationRegistry serializationRegistry = Serializers.createRegistry();
                ByteOutputStream stream = new ByteOutputStream();
                Serialization serialization = new Serialization(serializationRegistry, true, stream);
                serialization.setExtension(BatchOperation.EXTENTION_ID, database.getContext());

                serialization.writeObject(batchOperation1);
                serialization.writeObject(batchOperation2);

                ByteArray data = new ByteArray(stream.getBuffer(), 0, stream.getLength());
                ByteInputStream inStream = new ByteInputStream(data.getBuffer(), data.getOffset(), data.getLength());

                Deserialization deserialization = new Deserialization(serializationRegistry, inStream);
                deserialization.setExtension(BatchOperation.EXTENTION_ID, database.getContext());
                DbBatchOperation batchOperation11 = deserialization.readObject();
                ClosePeriodBatchOperation operation11 = (ClosePeriodBatchOperation) batchOperation11.getOperation();
                DbBatchOperation batchOperation12 = deserialization.readObject();
                ClosePeriodBatchOperation operation12 = (ClosePeriodBatchOperation) batchOperation12.getOperation();

                assertThat(batchOperation11.getConstraints(), nullValue());
                assertThat(operation11.getRuleContext(), nullValue());
                assertThat(operation11.getHierarchyMap(), nullValue());

                assertThat(batchOperation12.getConstraints(), is(com.exametrika.common.utils.Collections.asSet(
                        new DbBatchOperation.CacheConstraint("category1", 123), new DbBatchOperation.CacheConstraint("category2", 345))));
                assertThat(batchOperation12.isCachingEnabled(), is(true));

                assertThat(operation2.getCurrentTime(), is(123l));
                assertThat(operation2.getPeriodType(), is("p2"));
                assertThat(operation2.getCycleSchemaState(), is(1));
                assertThat(operation2.getAggregationState(), is(2));
                assertThat(operation2.getAggregationNodeId(), is(3l));
                assertThat(operation2.getInterceptResult(), is(true));
                assertThat(operation2.getTotalEndNodes(), is(234));
                assertThat(operation2.getAggregationEndNodes(), is(235));
                assertThat(operation2.getTotalNodes(), is(236));
                assertThat(operation2.getAggregationNodes(), is(237));
                assertThat(operation2.getEndNodes(), is(238));
                assertThat(operation2.getDerivedNodes(), is(239));
                assertThat(operation2.getIteration(), is(240));
                assertThat(operation12.getRuleContext().getExecutors().size(), is(1));
                RuleExecutorInfo info = operation12.getRuleContext().getExecutors().get(123);
                assertThat(info.getExecutor() == component, is(true));
                assertThat(info.getFacts(), is(new MapBuilder<String, Object>().put("fact1", "value1").put("fact2", "value2").toMap()));

                assertThat(operation12.getHierarchyMap().size(), is(1));
                EntryPointHierarchy hierarchy12 = operation12.getHierarchyMap().get(12);
                assertThat(hierarchy12.scopeHierarchy.getScopes().size(), is(10));
                for (int i = 0; i < 10; i++)
                    assertThat(hierarchy12.scopeHierarchy.getScopes().get(i), is(Names.getScope("scope" + i)));

                assertThat(hierarchy12.nodeHierarchy.size(), is(10));
                for (int i = 0; i < 10; i++)
                    assertThat(hierarchy12.nodeHierarchy.get(i), is((Object)period.findNode(new Location(1000 + i, 0), cycleSchema.findAggregationNode("Transaction2"))));
            }
        });
    }

    private void createDatabase(String schemaConfigName) {
        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final String schemaResourcePath = resourcePath + schemaConfigName;
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModules(schemaResourcePath, null);
            }
        });
    }

    private void checkDump(String dumpDirectoryPath, String ethalonDirectoryPath, List<String> resultFileNames) {
        database.getOperations().dump(new File(tempDir, dumpDirectoryPath).getPath(), new DumpContext(IDumpContext.DUMP_ORPHANED,
                Json.object().put("dumpLogs", true).toObject()), null);

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

    private static class ActionTask {
        private final Callable callable;
        private final ICompletionHandler completionHandler;
        private Object result;

        public ActionTask(String agentId, Callable callable, ICompletionHandler completionHandler) {
            this.callable = callable;
            this.completionHandler = completionHandler;
        }
    }

    public static class TestLocalActionSchemaConfiguration extends AsyncActionSchemaConfiguration {
        public TestLocalActionSchemaConfiguration(String name) {
            super(name);
        }

        @Override
        public Callable createLocal(Map<String, Object> parameters) {
            return new TestAction(parameters);
        }

        @Override
        public Object createRemote(Map<String, Object> parameters) {
            Assert.supports(false);
            return null;
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestLocalActionSchemaConfiguration))
                return false;

            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }

        @Override
        protected Map<String, AsyncActionParameterDefinitionSchemaConfiguration> buildParameterDefinitions() {
            return new MapBuilder<String, AsyncActionParameterDefinitionSchemaConfiguration>()
                    .put("p1", new AsyncActionParameterDefinitionSchemaConfiguration(true, "option1", null, null))
                    .put("p2", new AsyncActionParameterDefinitionSchemaConfiguration(true, null, "property1", null))
                    .put("p3", new AsyncActionParameterDefinitionSchemaConfiguration(true, null, null, "default"))
                    .put("p4", new AsyncActionParameterDefinitionSchemaConfiguration(false, null, null, null))
                    .toMap();
        }
    }

    public static class TestRemoteActionSchemaConfiguration extends AsyncActionSchemaConfiguration {
        public TestRemoteActionSchemaConfiguration(String name) {
            super(name);
        }

        @Override
        public Callable createLocal(Map<String, Object> parameters) {
            Assert.supports(false);
            return null;
        }

        @Override
        public Object createRemote(Map<String, Object> parameters) {
            return new TestAction(parameters);
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRemoteActionSchemaConfiguration))
                return false;

            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }

        @Override
        protected Map<String, AsyncActionParameterDefinitionSchemaConfiguration> buildParameterDefinitions() {
            return new MapBuilder<String, AsyncActionParameterDefinitionSchemaConfiguration>()
                    .put("p1", new AsyncActionParameterDefinitionSchemaConfiguration(true, "option1", null, null))
                    .put("p2", new AsyncActionParameterDefinitionSchemaConfiguration(true, null, "property1", null))
                    .put("p3", new AsyncActionParameterDefinitionSchemaConfiguration(true, null, null, "default"))
                    .put("p4", new AsyncActionParameterDefinitionSchemaConfiguration(false, null, null, null))
                    .toMap();
        }
    }

    public static class TestRuleSchemaConfiguration extends RuleSchemaConfiguration {
        public TestRuleSchemaConfiguration(String name) {
            super(name, true);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestRuleSchemaConfiguration))
                return false;

            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }

        @Override
        public IRule createRule(IDatabaseContext context) {
            return null;
        }
    }

    public static class TestComponentBindingStrategySchemaConfiguration extends ComponentBindingStrategySchemaConfiguration {
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestComponentBindingStrategySchemaConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public IComponentBindingStrategy createStrategy(IDatabaseContext context) {
            return new TestComponentBindingStrategy();
        }
    }

    private static class TestComponentBindingStrategy implements IComponentBindingStrategy {
        @Override
        public IScopeName getComponentScope(IAggregationNode aggregationNode) {
            return aggregationNode.getScope();
        }
    }

    public static class TestAgentFailureDetector implements IAgentFailureDetector {
        public IAgentFailureListener listener;
        public boolean active = true;

        @Override
        public boolean isActive(String agentId) {
            return active;
        }

        @Override
        public void addFailureListener(IAgentFailureListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeFailureListener(IAgentFailureListener listener) {
            listener = null;
        }
    }
}
