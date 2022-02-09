/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.tester.config.TestCaseConfiguration;
import com.exametrika.api.tester.config.TestCaseFilterConfiguration;
import com.exametrika.api.tester.config.TestConfiguration;
import com.exametrika.api.tester.config.TestStartStepConfiguration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.tasks.IActivationCondition;
import com.exametrika.common.tasks.IAsyncTaskHandleAware;
import com.exametrika.common.tasks.ITaskContext;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskScheduler;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.tester.core.messages.ActionMessage;
import com.exametrika.impl.tester.core.messages.ActionResponseMessage;
import com.exametrika.impl.tester.core.messages.ControlMessage;
import com.exametrika.impl.tester.core.messages.ControlMessage.Type;
import com.exametrika.impl.tester.core.messages.InstallMessage;
import com.exametrika.impl.tester.core.messages.InstallRoleMessage;
import com.exametrika.impl.tester.core.messages.ResponseMessage;
import com.exametrika.impl.tester.core.messages.SynchronizeRolesMessage;
import com.exametrika.impl.tester.core.messages.SynchronizeRolesResponseMessage;
import com.exametrika.spi.tester.ITestAction;
import com.exametrika.spi.tester.ITestCaseBuilder;
import com.exametrika.spi.tester.ITestReporter;
import com.exametrika.spi.tester.ITestResultAnalyzer;
import com.exametrika.spi.tester.config.TestActionConfiguration;
import com.exametrika.spi.tester.config.TestNodeConfiguration;
import com.exametrika.spi.tester.config.TestReporterConfiguration;
import com.exametrika.spi.tester.config.TestResultAnalyzerConfiguration;


/**
 * The {@link TestCoordinator} represents a test coordinator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCoordinator {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestCoordinator.class);
    private final TestCoordinatorService service;
    private TaskScheduler<Runnable> scheduler;
    private TestConfiguration configuration;
    private List<TestCaseConfiguration> testCases;
    private long lastProcessTime = Times.getCurrentTime();
    private State state;
    private int testCaseIndex = -1;
    private TestCaseConfiguration testCaseConfiguration;
    private int startStepIndex = -1;
    private TestStartStepConfiguration startStepConfiguration;
    private Set<IAddress> agents;
    private Map<String, IAddress> nodes;
    private Map<String, IAddress> responses;
    private List<String> failedTestCases = new ArrayList<String>();
    private int runNumber;
    private final NumberFormat format = new DecimalFormat("0000");
    private final Map<String, RoleInfo> roleInfos = new HashMap<String, RoleInfo>();

    public TestCoordinator(TestCoordinatorService service) {
        Assert.notNull(service);

        this.service = service;
    }

    public void receive(IMessage message) {
        if (message.getPart() instanceof ResponseMessage) {
            ResponseMessage part = (ResponseMessage) message.getPart();

            if (part.isResultsOnly()) {
                writeResults(part, message.getFiles());
                return;
            }

            if ((state == State.INSTALL || state == State.START || state == State.STOP || state == State.COLLECT_RESULTS || state == State.RUN)) {
                responses.remove(part.getNodeName());

                if (part.getException() != null) {
                    if (logger.isLogEnabled(LogLevel.TRACE))
                        logger.log(LogLevel.TRACE, messages.failureResponseReceived(state, part.getNodeName(), message.getSource(), responses), part.getException());

                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.agentFailed(part.getNodeName(), message.getSource()), part.getException());

                    writeResults(part, message.getFiles());
                    failTestCase();
                    return;
                }

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.successResponseReceived(state, part.getNodeName(), message.getSource(), responses));

                if (state == State.COLLECT_RESULTS)
                    writeResults(part, message.getFiles());

                if (responses.isEmpty()) {
                    responses = null;
                    if (state == State.INSTALL)
                        startTestCase();
                    else if (state == State.START)
                        startWaitTestCase();
                    else if (state == State.STOP)
                        collectResults();
                    else if (state == State.COLLECT_RESULTS)
                        succeedTestCase();
                }
            }
        } else if (message.getPart() instanceof ControlMessage) {
            ControlMessage part = (ControlMessage) message.getPart();
            if (state == State.RUN && part.getType() == Type.RUN_STOPPED_RESPONSE) {
                responses.remove(part.getNodeName());

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.successResponseReceived(state, part.getNodeName(), message.getSource(), responses));

                if (responses.isEmpty())
                    collectResults();
            }
        } else if (message.getPart() instanceof ActionResponseMessage) {
            ActionResponseMessage part = message.getPart();
            if (state == State.RUN) {
                TestActionTask task = (TestActionTask) scheduler.findTask(part.getNodeName() + "." + part.getActionName());
                if (task != null && task.testCaseName.equals(testCaseConfiguration.getName())) {
                    if (part.getError() == null)
                        task.onSucceeded();
                    else
                        task.onFailed(part.getError());
                }
            }
        } else if (message.getPart() instanceof SynchronizeRolesResponseMessage) {
            SynchronizeRolesResponseMessage part = message.getPart();
            if (state == State.SYNCHRONIZE_ROLES && agents != null && agents.contains(message.getSource())) {
                for (Map.Entry<String, String> entry : part.getRolesHashes().entrySet()) {
                    RoleInfo info = roleInfos.get(entry.getKey());
                    if (info == null)
                        continue;

                    info.agentHashes.put(message.getSource(), entry.getValue());
                }

                agents.remove(message.getSource());

                if (agents.isEmpty()) {
                    agents = null;
                    selectTestCase();
                }
            }
        }
    }

    public void onNodeFailed(IAddress address) {
        if (nodes != null) {
            for (Iterator<Map.Entry<String, IAddress>> it = nodes.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, IAddress> entry = it.next();
                if (entry.getValue().equals(address)) {
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.nodeFailed(entry.getKey(), address));

                    it.remove();
                }
            }

        }

        failTest();
    }

    public void onTimer(long currentTime) {
        if (state == null) {
            if (service.isConnected())
                startTest();
        } else if (state == State.RUN) {
            if (currentTime > lastProcessTime + testCaseConfiguration.getDuration())
                stopTestCase();
        } else if (state == State.START_WAIT) {
            if (currentTime > lastProcessTime + startStepConfiguration.getPeriod())
                startTestCase();
        } else if (state == State.STOP_FAILED) {
            if (currentTime > lastProcessTime + 30000)
                stopTest(true);
        }
    }

    public void clear() {
        state = null;
        lastProcessTime = Times.getCurrentTime();
        testCaseIndex = -1;
        testCaseConfiguration = null;
        startStepIndex = -1;
        startStepConfiguration = null;
        nodes = null;
        responses = null;
    }

    private void startTest() {
        File workPath = getWorkPath();
        if (workPath.exists())
            Files.emptyDir(workPath);
        else
            workPath.mkdirs();

        TestMacroses.setTestCoordinatorService(service);
        ConfigurationLoader loader = new ConfigurationLoader(Collections.asSet("test"));
        ILoadContext context = loader.loadConfiguration(service.getConfiguration().getTestConfigurationPath());
        configuration = context.get(TestConfiguration.SCHEMA);

        File resultsPath = new File(configuration.getResultsPath());
        if (!resultsPath.exists()) {
            resultsPath.mkdirs();
            runNumber = 1;
        } else {
            Assert.isTrue(resultsPath.isDirectory());
            String[] runs = resultsPath.list();
            int maxRun = 0;
            for (String runStr : runs) {
                try {
                    int run = format.parse(runStr).intValue();
                    if (run > maxRun)
                        maxRun = run;
                } catch (ParseException e) {
                }
            }

            runNumber = maxRun + 1;
        }
        testCases = Collections.toList(configuration.getTestCases().values().iterator());
        scheduler = new TaskScheduler<Runnable>(1, new RunnableTaskHandler<Runnable>(),
                new SystemTimeService(), 1000, "[test coordinator] task scheduler thread", "[test coordinator] task executor thread", null);
        scheduler.start();

        state = State.SYNCHRONIZE_ROLES;
        for (String role : configuration.getRoles()) {
            File roleFile = new File(configuration.getInstallationPath(), "roles" + File.separator + role);
            roleInfos.put(role, new RoleInfo(Files.md5Hash(roleFile)));
        }

        this.agents = new HashSet<IAddress>();
        Map<IAddress, TestCoordinatorChannel> agents = service.getChannels();
        for (TestCoordinatorChannel agent : agents.values()) {
            agent.send(new SynchronizeRolesMessage(configuration.getRoles()));
            this.agents.add(agent.getAddress());
        }
    }

    private void stopTest(final boolean failure) {
        scheduler.stop();
        clear();
        state = State.FINISHED;
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.exit(failure ? 1 : 0);
            }
        }).start();
    }

    private void selectTestCase() {
        scheduler.removeAllTasks();
        testCaseIndex++;
        testCaseConfiguration = null;
        for (int i = testCaseIndex; i < testCases.size(); i++) {
            TestCaseConfiguration testCase = testCases.get(i);
            if (canExecute(testCase)) {
                testCaseIndex = i;
                testCaseConfiguration = testCase;

                installTestCase();
                return;
            }
        }

        completeTest();
    }

    private boolean canExecute(TestCaseConfiguration testCase) {
        NameFilter testCaseFilter = service.getTestCaseFilter();
        if (testCaseFilter != null)
            return testCaseFilter.match(testCase.getName());

        for (TestCaseFilterConfiguration filter : service.getConfiguration().getExecute()) {
            if (filter.match(testCase))
                return true;
        }
        return false;
    }

    private void installTestCase() {
        System.out.println(messages.testCase(testCaseConfiguration.getName()).toString());

        state = State.INSTALL;
        if (!buildRequiredNodes()) {
            failTest();
            return;
        }

        for (TestNodeConfiguration node : testCaseConfiguration.getNodes().values()) {
            if (node.getRole() == null)
                continue;

            IAddress address = nodes.get(node.getName());
            RoleInfo roleInfo = roleInfos.get(node.getRole());
            Assert.checkState(roleInfo != null);

            String roleAgentHash = roleInfo.agentHashes.get(address);
            if (!roleInfo.md5Hash.equals(roleAgentHash)) {
                roleInfo.agentHashes.put(address, roleInfo.md5Hash);

                File roleDir = new File(configuration.getInstallationPath(), "roles" + File.separator + node.getRole());
                if (roleDir.exists()) {
                    File roleArchive = Files.createTempFile("tester", ".zip");
                    Files.zip(roleDir, roleArchive);

                    service.send(address, new InstallRoleMessage(node.getRole(), roleInfo.md5Hash), Arrays.asList(roleArchive));

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.installRole(node.getRole(), address));
                } else if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.roleDirNotExist(roleDir));
            }
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.installTestCase(testCaseConfiguration.getName(), nodes));

        responses = new HashMap<String, IAddress>(nodes);
        for (TestNodeConfiguration node : testCaseConfiguration.getNodes().values()) {
            File build = getBuildPath(node.getName(), testCaseConfiguration.getName());
            build.mkdirs();
            Files.emptyDir(build);
            ITestCaseBuilder testCaseBuilder = node.getTestCaseBuilder().createBuilder();
            testCaseBuilder.build(configuration.getInstallationPath(), build.getPath(), testCaseConfiguration, node);
            File archive = Files.createTempFile("tester", ".zip");
            Files.zip(build, archive);

            IAddress address = nodes.get(node.getName());
            service.send(address, new InstallMessage(testCaseConfiguration.getName(), node.getRole(), node.getName(),
                    node.getExecutorName(), node.getExecutorParameters()), Arrays.asList(archive));
        }
    }

    private void startTestCase() {
        startStepIndex++;
        if (startStepIndex >= testCaseConfiguration.getStartSteps().size()) {
            startStepIndex = -1;
            startStepConfiguration = null;
            runTestCase();
            return;
        }

        startStepConfiguration = testCaseConfiguration.getStartSteps().get(startStepIndex);
        state = State.START;
        responses = new HashMap<String, IAddress>();
        for (Map.Entry<String, IAddress> entry : nodes.entrySet()) {
            if (startStepConfiguration.getNodes().contains(entry.getKey()))
                responses.put(entry.getKey(), entry.getValue());
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.startStep(startStepIndex, responses));

        for (Map.Entry<String, IAddress> entry : responses.entrySet())
            service.send(entry.getValue(), new ControlMessage(entry.getKey(), Type.START));
    }

    private void startWaitTestCase() {
        state = State.START_WAIT;
        lastProcessTime = Times.getCurrentTime();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.startStepWait());
    }

    private void runTestCase() {
        state = State.RUN;
        lastProcessTime = Times.getCurrentTime();

        responses = new HashMap<String, IAddress>(nodes);
        for (Map.Entry<String, IAddress> entry : nodes.entrySet())
            service.send(entry.getValue(), new ControlMessage(entry.getKey(), Type.RUN));

        for (TestNodeConfiguration node : testCaseConfiguration.getNodes().values()) {
            for (TestActionConfiguration action : node.getActions().values()) {
                TestActionTask task = new TestActionTask(nodes.get(node.getName()), testCaseConfiguration.getName(),
                        node.getName(), action.getName(), action.createAction(node.getName()));
                scheduler.addTask(node.getName() + "." + action.getName(), task, task, action.isRecurrent(), true, task);
            }
        }
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.runTestCase());
    }

    private void stopTestCase() {
        scheduler.removeAllTasks();
        state = State.STOP;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopTestCase());

        responses = new HashMap<String, IAddress>(nodes);
        for (Map.Entry<String, IAddress> entry : nodes.entrySet())
            service.send(entry.getValue(), new ControlMessage(entry.getKey(), Type.STOP));
    }

    private void collectResults() {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.collectResults());

        state = State.COLLECT_RESULTS;
        responses = new HashMap<String, IAddress>(nodes);
        for (Map.Entry<String, IAddress> entry : nodes.entrySet())
            service.send(entry.getValue(), new ControlMessage(entry.getKey(), Type.COLLECT_RESULTS));
    }

    private void succeedTestCase() {
        boolean succeeded = true;
        for (TestNodeConfiguration node : testCaseConfiguration.getNodes().values()) {
            File resultsPath = getResultsPath(node.getName(), testCaseConfiguration.getName());
            if (!resultsPath.exists())
                continue;

            for (TestResultAnalyzerConfiguration analyzerConfiguration : node.getAnalyzers()) {
                ITestResultAnalyzer analyzer = analyzerConfiguration.createAnalyzer();
                succeeded = analyzer.analyze(resultsPath) && succeeded;
            }
        }

        if (succeeded) {
            System.out.println(messages.succeeded().toString());

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.testCaseSucceeded());
        } else {
            failedTestCases.add(testCaseConfiguration.getName());

            System.out.println(messages.failed().toString());

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.testCaseFailed());
        }

        selectTestCase();
    }

    private void failTestCase() {
        clearFailedTestCase();

        failedTestCases.add(testCaseConfiguration.getName());

        System.out.println(messages.failed().toString());

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.testCaseFailed());

        selectTestCase();
    }

    private void completeTest() {
        File resultsPath = getResultsPath();
        if (resultsPath.exists()) {
            for (TestReporterConfiguration reporterConfiguration : configuration.getReporters().values()) {
                ITestReporter reporter = reporterConfiguration.createReporter();
                reporter.report(resultsPath);
            }
        }

        if (failedTestCases.isEmpty()) {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.testSucceeded());

            System.out.println(messages.testSucceeded().toString());
            stopTest(false);
        } else {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.testFailed(failedTestCases));

            System.out.println(messages.testFailed(failedTestCases).toString());
            stopTest(true);
        }
    }

    private void failTest() {
        if (state == State.STOP_FAILED || state == State.FINISHED)
            return;

        if (testCaseConfiguration != null)
            failedTestCases.add(testCaseConfiguration.getName());

        clearFailedTestCase();
        clear();

        state = State.STOP_FAILED;
        lastProcessTime = Times.getCurrentTime();

        System.out.println(messages.failed().toString());

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.testFailed(failedTestCases));

        System.out.println(messages.testFailed(failedTestCases).toString());
    }

    private void clearFailedTestCase() {
        if (state == State.START || state == State.START_WAIT || state == State.RUN) {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.stopFailedTestCase());

            for (Map.Entry<String, IAddress> entry : nodes.entrySet())
                service.send(entry.getValue(), new ControlMessage(entry.getKey(), Type.STOP_FAILED));
        }
    }

    private boolean buildRequiredNodes() {
        Map<IAddress, TestCoordinatorChannel> agents = service.getChannels();

        this.nodes = new HashMap<String, IAddress>();
        List<Pair<String, String>> absentNodes = new ArrayList<Pair<String, String>>();
        for (Map.Entry<String, TestNodeConfiguration> nodeEntry : testCaseConfiguration.getNodes().entrySet()) {
            IAddress address = null;
            for (Map.Entry<IAddress, TestCoordinatorChannel> agentEntry : agents.entrySet()) {
                if (agentEntry.getValue().getConfiguration().getName().equals(nodeEntry.getValue().getAgent())) {
                    address = agentEntry.getKey();
                    break;
                }
            }
            if (address == null)
                absentNodes.add(new Pair<String, String>(nodeEntry.getKey(), nodeEntry.getValue().getAgent()));
            else
                this.nodes.put(nodeEntry.getKey(), address);
        }

        if (!absentNodes.isEmpty()) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.requiredNodesNotFound(absentNodes));

            return false;
        }

        return true;
    }

    private void writeResults(ResponseMessage part, List<File> results) {
        if (results == null)
            return;

        File resultsPath = getResultsPath(part.getNodeName(), part.getTestCaseName());
        resultsPath.mkdirs();
        Files.emptyDir(resultsPath);

        File temp = results.get(0);
        Files.unzip(temp, resultsPath);
        temp.delete();
    }

    private File getWorkPath() {
        return new File(System.getProperty("com.exametrika.workPath"), "tester" + File.separator + "coordinator");
    }

    private File getResultsPath() {
        return new File(configuration.getResultsPath(), format.format(runNumber));
    }

    private File getResultsPath(String nodeName, String testCaseName) {
        return new File(getResultsPath(), testCaseName + File.separator + nodeName);
    }

    private File getBuildPath(String nodeName, String testCaseName) {
        return new File(getWorkPath(), testCaseName + File.separator + nodeName + File.separator + "build");
    }

    private enum State {
        SYNCHRONIZE_ROLES,
        INSTALL,
        START,
        START_WAIT,
        RUN,
        STOP,
        STOP_FAILED,
        COLLECT_RESULTS,
        FINISHED
    }

    private class TestActionTask implements Runnable, IActivationCondition<Runnable>, IAsyncTaskHandleAware {
        private final IAddress testAgentAddress;
        private final String testCaseName;
        private final String nodeName;
        private final String actionName;
        private final ITestAction action;
        private Object asyncTaskHandle;

        public TestActionTask(IAddress testAgentAddress, String testCaseName, String nodeName, String actionName, ITestAction action) {
            Assert.notNull(testAgentAddress);
            Assert.notNull(testCaseName);
            Assert.notNull(nodeName);
            Assert.notNull(actionName);
            Assert.notNull(action);

            this.testAgentAddress = testAgentAddress;
            this.testCaseName = testCaseName;
            this.nodeName = nodeName;
            this.actionName = actionName;
            this.action = action;
        }

        @Override
        public boolean evaluate(Long value) {
            Assert.supports(false);
            return false;
        }

        @Override
        public boolean canActivate(long currentTime, ITaskContext context) {
            return action.canActivate(currentTime, context);
        }

        @Override
        public void tryInterrupt(long currentTime) {
        }

        @Override
        public void onCompleted(ITaskContext context) {
            action.onCompleted(context);
        }

        @Override
        public void run() {
            service.send(testAgentAddress, new ActionMessage(nodeName, actionName, action.getParameters()));
        }

        @Override
        public void setAsyncTaskHandle(Object taskHandle) {
            this.asyncTaskHandle = taskHandle;
        }

        public void onSucceeded() {
            if (asyncTaskHandle != null)
                scheduler.onAsyncTaskSucceeded(asyncTaskHandle);
        }

        public void onFailed(Throwable error) {
            if (asyncTaskHandle != null)
                scheduler.onAsyncTaskFailed(asyncTaskHandle, error);
        }
    }

    private static class RoleInfo {
        private final String md5Hash;
        private final Map<IAddress, String> agentHashes = new HashMap<IAddress, String>();

        public RoleInfo(String md5Hash) {
            Assert.notNull(md5Hash);

            this.md5Hash = md5Hash;
        }
    }

    private interface IMessages {
        @DefaultMessage("Required nodes {0} are not found.")
        ILocalizedMessage requiredNodesNotFound(List<Pair<String, String>> nodes);

        @DefaultMessage("Role installation directory ''{0}'' does not exist.")
        ILocalizedMessage roleDirNotExist(File roleDir);

        @DefaultMessage("Success response ''{1}({2})'' has been received. State: {0}, responses: {3}.")
        ILocalizedMessage successResponseReceived(State state, String nodeName, IAddress source, Map<String, IAddress> responses);

        @DefaultMessage("Failure response ''{1}({2})'' has been received. State: {0}, responses: {3}.")
        ILocalizedMessage failureResponseReceived(State state, String nodeName, IAddress source, Map<String, IAddress> responses);

        @DefaultMessage("Execution of test agent ''{0}({1})'' has been failed.")
        ILocalizedMessage agentFailed(String nodeName, IAddress source);

        @DefaultMessage("Node ''{0}({1})'' has been failed.")
        ILocalizedMessage nodeFailed(String nodeName, IAddress address);

        @DefaultMessage("Test has been succeeded.")
        ILocalizedMessage testSucceeded();

        @DefaultMessage("Installing of test case ''{0}'' has been started to nodes: {1}.")
        ILocalizedMessage installTestCase(String name, Map<String, IAddress> nodes);

        @DefaultMessage("Role ''{0}'' is installed to agent ''{1}''.")
        ILocalizedMessage installRole(String role, IAddress agent);

        @DefaultMessage("Step ''{0}'' has been started for nodes: {1}.")
        ILocalizedMessage startStep(int startStepIndex, Map<String, IAddress> responses);

        @DefaultMessage("Start step is waiting.")
        ILocalizedMessage startStepWait();

        @DefaultMessage("Test case is running.")
        ILocalizedMessage runTestCase();

        @DefaultMessage("Test case is stopping as failed.")
        ILocalizedMessage stopFailedTestCase();

        @DefaultMessage("Test case is stopping.")
        ILocalizedMessage stopTestCase();

        @DefaultMessage("Collecting test case results.")
        ILocalizedMessage collectResults();

        @DefaultMessage("Test case has been succeeded.")
        ILocalizedMessage testCaseSucceeded();

        @DefaultMessage("Test case has been failed.")
        ILocalizedMessage testCaseFailed();

        @DefaultMessage("Test has been failed. Failed test cases: {0}")
        ILocalizedMessage testFailed(List<String> failedTestCases);

        @DefaultMessage("Test case ''{0}'' - ")
        ILocalizedMessage testCase(String name);

        @DefaultMessage("succeeded.")
        ILocalizedMessage succeeded();

        @DefaultMessage("failed.")
        ILocalizedMessage failed();
    }
}
