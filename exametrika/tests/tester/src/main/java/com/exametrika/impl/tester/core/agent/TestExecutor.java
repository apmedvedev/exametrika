/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.SyncCompletionHandler;
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
import com.exametrika.spi.tester.ITestCaseExecutor;
import com.exametrika.spi.tester.ITestCaseExecutor.State;
import com.exametrika.spi.tester.ITestCaseExecutorFactory;


/**
 * The {@link TestExecutor} represents a test executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestExecutor implements ICompartmentTimerProcessor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestExecutor.class);
    private final TestAgentCoordinatorChannel channel;
    private final ICompartment compartment;
    private final Map<String, ITestCaseExecutorFactory> testCaseExecutorFactories;
    private final Map<String, ExecutorInfo> testCaseExecutors = new HashMap<String, ExecutorInfo>();
    private long lastCheckTime = Times.getCurrentTime();

    public TestExecutor(TestAgentCoordinatorChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
        ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
        compartmentParameters.timerProcessors.add(this);
        compartmentParameters.name = channel.getMarker().getName();
        compartment = new CompartmentFactory().createCompartment(compartmentParameters);

        Map<String, ITestCaseExecutorFactory> testCaseExecutorFactories = new LinkedHashMap<String, ITestCaseExecutorFactory>();
        for (ITestCaseExecutorFactory factory : Services.loadProviders(ITestCaseExecutorFactory.class))
            testCaseExecutorFactories.put(factory.getName(), factory);

        this.testCaseExecutorFactories = testCaseExecutorFactories;

        Files.emptyDir(getWorkPath());

        compartment.start();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.started());
    }

    @Override
    public void onTimer(long currentTime) {
        if (currentTime < lastCheckTime + 1000)
            return;

        lastCheckTime = currentTime;

        for (Iterator<Map.Entry<String, ExecutorInfo>> it = testCaseExecutors.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ExecutorInfo> entry = it.next();
            if (entry.getValue().run && entry.getValue().executor.getState() != State.RUNNING && entry.getValue().executor.getState() != State.STOPPED) {
                entry.getValue().run = false;
                if (entry.getValue().executor.getState() == State.SUCCEEDED)
                    channel.send(new ControlMessage(entry.getKey(), Type.RUN_STOPPED_RESPONSE));
                else {
                    String console = "";
                    List<File> results = null;
                    try {
                        results = collectResults(entry.getKey(), entry.getValue().executor);
                        if (entry.getValue().executor.getConsole().exists())
                            console = Files.read(entry.getValue().executor.getConsole());
                    } catch (Exception e) {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, e);
                    }

                    channel.send(new ResponseMessage(entry.getValue().testCaseName, entry.getKey(), false,
                            new SystemException(new NonLocalizedMessage(console))), results);

                    it.remove();
                }
            }
        }
    }

    public void receive(final IMessage message) {
        compartment.offer(new Runnable() {
            @Override
            public void run() {
                receiveMessage(message);
            }
        });
    }

    public void close() {
        final SyncCompletionHandler handler = new SyncCompletionHandler();
        compartment.offer(new Runnable() {
            @Override
            public void run() {
                for (ExecutorInfo info : testCaseExecutors.values()) {
                    try {
                        info.executor.destroy(null);
                    } catch (Throwable e) {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, e);
                    }
                }

                testCaseExecutors.clear();

                handler.onSucceeded(null);
            }
        });

        handler.await(10000);
        compartment.stop();

        Files.emptyDir(getWorkPath());

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.closed());
    }

    private void receiveMessage(IMessage message) {
        if (message.getPart() instanceof InstallRoleMessage) {
            InstallRoleMessage part = (InstallRoleMessage) message.getPart();

            File rolePath = new File(getRolesPath(), part.getRoleName());
            File roleHashPath = new File(getRolesPath(), part.getRoleName() + ".md5");
            if (rolePath.exists())
                Files.emptyDir(rolePath);

            File file = message.getFiles().get(0);
            Files.unzip(file, rolePath);
            file.delete();

            Files.write(roleHashPath, part.getMd5Hash());

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, channel.getMarker(), messages.roleInstalled(part.getRoleName()));
        } else if (message.getPart() instanceof SynchronizeRolesMessage) {
            SynchronizeRolesMessage part = (SynchronizeRolesMessage) message.getPart();
            File rolesPath = getRolesPath();
            Map<String, String> rolesHashes = new HashMap<String, String>();
            if (rolesPath.exists()) {
                File[] files = rolesPath.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (!part.getRoles().contains(file.getName()))
                            Files.delete(file);
                    } else if (file.getName().endsWith(".md5")) {
                        String roleName = file.getName().substring(0, file.getName().length() - ".md5".length());
                        if (!part.getRoles().contains(roleName))
                            Files.delete(file);
                        else
                            rolesHashes.put(roleName, Files.read(file));
                    } else
                        Files.delete(file);
                }
            }

            channel.send(new SynchronizeRolesResponseMessage(rolesHashes));
        } else if (message.getPart() instanceof InstallMessage) {
            InstallMessage part = (InstallMessage) message.getPart();

            Assert.notNull(message.getFiles());
            Assert.isTrue(message.getFiles().size() == 1);

            Throwable exception = null;
            try {
                Assert.isTrue(!testCaseExecutors.containsKey(part.getNodeName()));

                File destination = getExecutorPath(part.getNodeName());
                destination.mkdirs();
                Files.emptyDir(destination);

                if (part.getRoleName() != null) {
                    File rolePath = new File(getRolesPath(), part.getRoleName());
                    Files.copy(rolePath, destination);
                }
                File file = message.getFiles().get(0);

                ITestCaseExecutorFactory factory = testCaseExecutorFactories.get(part.getExecutorName());
                Assert.notNull(factory);

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, channel.getMarker(), messages.nodeInstalled(part.getNodeName()));

                ITestCaseExecutor testCaseExecutor = factory.createExecutor(destination.getPath(), part.getExecutorParameters(), compartment);
                testCaseExecutor.install(file.getPath());
                file.delete();

                testCaseExecutors.put(part.getNodeName(), new ExecutorInfo(part.getTestCaseName(), testCaseExecutor));
            } catch (Throwable e) {
                exception = e;

                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }

            channel.send(new ResponseMessage(part.getTestCaseName(), part.getNodeName(), false, exception));
        } else if (message.getPart() instanceof ControlMessage) {
            final ControlMessage part = (ControlMessage) message.getPart();
            ExecutorInfo info = null;

            try {
                info = testCaseExecutors.get(part.getNodeName());
                if (info == null)
                    return;

                final String testCaseName = info.testCaseName;
                final ITestCaseExecutor testCaseExecutor = info.executor;

                switch (part.getType()) {
                    case START:
                        testCaseExecutor.start(new CompletionHandler() {
                            @Override
                            protected void onCompleted(Object result) {
                                if (logger.isLogEnabled(LogLevel.DEBUG))
                                    logger.log(LogLevel.DEBUG, channel.getMarker(), messages.nodeStarted(part.getNodeName()));

                                channel.send(new ResponseMessage(testCaseName, part.getNodeName(), false, null));
                            }
                        });

                        break;
                    case RUN:
                        info.run = true;
                        break;
                    case STOP:
                        info.run = false;
                        testCaseExecutor.stop(new CompletionHandler() {
                            @Override
                            protected void onCompleted(Object result) {
                                if (logger.isLogEnabled(LogLevel.DEBUG))
                                    logger.log(LogLevel.DEBUG, channel.getMarker(), messages.nodeStopped(part.getNodeName()));

                                channel.send(new ResponseMessage(testCaseName, part.getNodeName(), false, null));
                            }
                        });

                        break;
                    case STOP_FAILED: {
                        info.run = false;
                        testCaseExecutor.stop(new CompletionHandler() {
                            @Override
                            protected void onCompleted(Object result) {
                                List<File> results = collectResults(part.getNodeName(), testCaseExecutor);
                                testCaseExecutors.remove(part.getNodeName());

                                if (logger.isLogEnabled(LogLevel.DEBUG))
                                    logger.log(LogLevel.DEBUG, channel.getMarker(), messages.nodeFailStopped(part.getNodeName()));

                                channel.send(new ResponseMessage(testCaseName, part.getNodeName(), true, null), results);
                            }
                        });

                        break;
                    }
                    case COLLECT_RESULTS: {
                        List<File> results = collectResults(part.getNodeName(), testCaseExecutor);
                        testCaseExecutors.remove(part.getNodeName());

                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.nodeResultsCollected(part.getNodeName()));

                        channel.send(new ResponseMessage(info.testCaseName, part.getNodeName(), false, null), results);
                        break;
                    }
                    default:
                        Assert.error();
                }
            } catch (Throwable e) {
                if (part.getType() != Type.STOP_FAILED) {
                    String testCaseName = "<unknown>";
                    List<File> results = null;
                    if (info != null) {
                        info.run = false;
                        testCaseName = info.testCaseName;
                        results = collectResults(part.getNodeName(), info.executor);
                        testCaseExecutors.remove(part.getNodeName());
                    }

                    channel.send(new ResponseMessage(testCaseName, part.getNodeName(), false, e), results);
                }

                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        } else if (message.getPart() instanceof ActionMessage) {
            final ActionMessage part = (ActionMessage) message.getPart();
            ExecutorInfo info = testCaseExecutors.get(part.getNodeName());
            if (info == null)
                return;

            info.executor.execute(part.getActionName(), part.getParameters(), new CompletionHandler() {
                @Override
                protected void onCompleted(Object result) {
                    Throwable error = null;
                    if (result instanceof Throwable)
                        error = (Throwable) result;

                    channel.send(new ActionResponseMessage(part.getNodeName(), part.getActionName(), error));

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, channel.getMarker(), messages.nodeActionExecuted(part.getNodeName(), part.getActionName()));
                }
            });
        }
    }

    private List<File> collectResults(String nodeName, ITestCaseExecutor testCaseExecutor) {
        try {
            File path = Files.createTempDirectory("tester");
            testCaseExecutor.destroy(path.getPath());
            Files.delete(getExecutorPath(nodeName));
            File archive = Files.createTempFile("tester", ".zip");

            Files.zip(path, archive);
            Files.delete(path);
            return Arrays.asList(archive);
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);

            return null;
        }
    }

    private File getExecutorPath(String nodeName) {
        return new File(getWorkPath(), nodeName);
    }

    private File getWorkPath() {
        return new File(getAgentWorkPath(), "nodes");
    }

    private File getRolesPath() {
        return new File(getAgentWorkPath(), "roles");
    }

    private File getAgentWorkPath() {
        return new File(System.getProperty("com.exametrika.workPath"), "tester" + File.separator + "agent");
    }

    private static class ExecutorInfo {
        private final String testCaseName;
        private final ITestCaseExecutor executor;
        private boolean run;

        public ExecutorInfo(String testCaseName, ITestCaseExecutor executor) {
            this.testCaseName = testCaseName;
            this.executor = executor;
        }
    }

    private interface IMessages {
        @DefaultMessage("Test executor has been started.")
        ILocalizedMessage started();

        @DefaultMessage("Test role ''{0}'' has been installed.")
        ILocalizedMessage roleInstalled(String roleName);

        @DefaultMessage("Test node ''{0}'' has been installed.")
        ILocalizedMessage nodeInstalled(String nodeName);

        @DefaultMessage("Test node ''{0}'' has been started.")
        ILocalizedMessage nodeStarted(String nodeName);

        @DefaultMessage("Test node ''{0}'' has been stopped.")
        ILocalizedMessage nodeStopped(String nodeName);

        @DefaultMessage("Test node ''{0}'' has been stopped because of failure.")
        ILocalizedMessage nodeFailStopped(String nodeName);

        @DefaultMessage("Test node ''{0}'' results have been collected.")
        ILocalizedMessage nodeResultsCollected(String nodeName);

        @DefaultMessage("Test node ''{0}'' action ''{1}'' has been executed.")
        ILocalizedMessage nodeActionExecuted(String nodeName, String actionName);

        @DefaultMessage("Test executor has been stopped.")
        ILocalizedMessage closed();
    }
}
