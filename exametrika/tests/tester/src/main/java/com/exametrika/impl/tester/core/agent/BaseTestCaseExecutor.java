/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.impl.CompletionCompartmentTask;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.tester.ITestCaseExecutor;


/**
 * The {@link BaseTestCaseExecutor} represents a base test case executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BaseTestCaseExecutor implements ITestCaseExecutor {
    private static final IMessages messages = Messages.get(IMessages.class);
    protected static final ILogger logger = Loggers.get(BaseTestCaseExecutor.class);
    protected final File path;
    protected final File console;
    protected final Map<String, Object> parameters;
    protected final ICompartment compartment;
    private TestProcess process;

    public BaseTestCaseExecutor(String path, Map<String, Object> parameters, ICompartment compartment) {
        Assert.notNull(path);
        Assert.notNull(parameters);
        Assert.notNull(compartment);

        this.path = new File(path);
        this.console = new File(path, "console.log");
        this.parameters = parameters;
        this.compartment = compartment;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.created(this.path, parameters));
    }

    @Override
    public void install(String path) {
        Files.unzip(new File(path), this.path);
    }

    @Override
    public State getState() {
        if (process != null) {
            if (process.isAlive())
                return State.RUNNING;
            else
                return process.getExitValue() == 0 ? State.SUCCEEDED : State.FAILED;
        }

        return State.STOPPED;
    }

    @Override
    public File getConsole() {
        return console;
    }

    @Override
    public void start(final ICompletionHandler completionHandler) {
        try {
            doStart(new CompletionHandler() {
                @Override
                protected void onCompleted(Object result) {
                    try {
                        startProcess();
                        doPostStart(completionHandler);
                    } catch (Exception e) {
                        completionHandler.onFailed(e);

                        Exceptions.wrapAndThrow(e);
                    }
                }
            });
        } catch (Exception e) {
            completionHandler.onFailed(e);

            Exceptions.wrapAndThrow(e);
        }
    }

    @Override
    public void stop(final ICompletionHandler completionHandler) {
        if (process != null) {
            try {
                doStop(new CompletionHandler() {
                    @Override
                    protected void onCompleted(Object result) {
                        stopProcess(completionHandler);
                    }
                });
            } catch (Exception e) {
                completionHandler.onFailed(e);

                Exceptions.wrapAndThrow(e);
            }
        } else
            completionHandler.onSucceeded(null);
    }

    @Override
    public void destroy(String path) {
        destroyProcess();
        if (path != null)
            doDestroy(path);
    }

    @Override
    public void execute(String action, Map<String, Object> parameters, ICompletionHandler completionHandler) {
        completionHandler.onSucceeded(null);
    }

    protected void startProcess() {
        if (process != null || !path.exists())
            return;

        List<String> command = buildCommand(parameters);
        File workingDir = getWorkingDir();
        ProcessBuilder processBuilder = new ProcessBuilder(command).directory(workingDir).redirectError(console).redirectOutput(console);
        configureProcessBuilder(processBuilder, parameters);
        process = new TestProcess(processBuilder);
        process.start();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started(path, command));
    }

    protected void stopProcess(ICompletionHandler completionHandler) {
        if (process != null) {
            final TestProcess process = this.process;
            BaseTestCaseExecutor.this.process = null;
            compartment.execute(new CompletionCompartmentTask(completionHandler) {
                @Override
                public Object execute() {
                    process.stop(10000);

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.stopped(path));

                    return null;
                }
            });
        } else
            completionHandler.onSucceeded(null);
    }

    protected void destroyProcess() {
        if (process != null) {
            process.destroy();
            process = null;

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.destroyed(path));
        }
    }

    protected final long getPid() {
        if (process != null)
            return process.getPid();
        else
            return -1;
    }

    protected abstract List<String> buildCommand(Map<String, Object> parameters);

    protected abstract File getWorkingDir();

    protected void doStart(ICompletionHandler completionHandler) {
        completionHandler.onSucceeded(null);
    }

    protected void doPostStart(ICompletionHandler completionHandler) {
        completionHandler.onSucceeded(null);
    }

    protected void doStop(ICompletionHandler completionHandler) {
        completionHandler.onSucceeded(null);
    }

    protected void doDestroy(String path) {
    }

    protected void configureProcessBuilder(ProcessBuilder processBuilder, Map<String, Object> parameters) {
    }

    private interface IMessages {
        @DefaultMessage("Test case executor has been created. Path: {0}, parameters: {1}")
        ILocalizedMessage created(File path, Map<String, Object> parameters);

        @DefaultMessage("Test case executor process has been started. Path: {0}, command: {1}")
        ILocalizedMessage started(File path, List<String> command);

        @DefaultMessage("Test case executor process has been stopped. Path: {0}")
        ILocalizedMessage stopped(File path);

        @DefaultMessage("Test case executor process has been destroyed. Path: {0}")
        ILocalizedMessage destroyed(File path);
    }
}
