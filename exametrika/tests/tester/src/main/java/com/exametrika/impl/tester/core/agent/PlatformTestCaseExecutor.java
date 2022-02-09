/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.impl.CompletionCompartmentTask;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.MapBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The {@link PlatformTestCaseExecutor} represents a platform test case executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformTestCaseExecutor extends BaseTestCaseExecutor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final File actionsConsole;
    private final Set<TestProcess> processes = new HashSet<TestProcess>();

    public PlatformTestCaseExecutor(String path, Map<String, Object> parameters, ICompartment compartment) {
        super(path, parameters, compartment);

        this.actionsConsole = new File(path, "actionsConsole.log");
    }

    @Override
    public void install(String path) {
        super.install(path);

        File binPath = new File(this.path, "bin");
        if (binPath.isDirectory()) {
            for (File file : binPath.listFiles())
                file.setExecutable(true);
        }
    }

    @Override
    public void execute(final String action, final Map<String, Object> parameters, final ICompletionHandler completionHandler) {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.actionStarted(path, action, parameters));

        ICompletionHandler handler = new CompletionHandler() {
            @Override
            public void onSucceeded(Object result) {
                completionHandler.onSucceeded(result);

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.actionSucceeded(path, action, parameters));
            }

            @Override
            public void onFailed(Throwable error) {
                completionHandler.onFailed(error);

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.actionFailed(path, action, parameters));
            }
        };

        try {
            if (action.equals("start")) {
                startProcess();
                handler.onSucceeded(null);
            } else if (action.equals("stop"))
                stopProcess(handler);
            else if (action.equals("destroy")) {
                destroyProcess();
                handler.onSucceeded(null);
            } else if (action.equals("run"))
                runProcess(action, parameters, handler);
            else if (action.equals("dump"))
                dump(parameters, handler);
            else if (action.equals("snapshot"))
                snapshot(parameters, handler);
            else if (action.equals("attach"))
                attach(parameters, handler);
            else
                handler.onSucceeded(null);
        } catch (Exception e) {
            handler.onFailed(e);

            Exceptions.wrapAndThrow(e);
        }
    }

    @Override
    protected List<String> buildCommand(Map<String, Object> parameters) {
        JsonArray array = (JsonArray) parameters.get("command");
        List<String> command = new ArrayList<String>();
        File commandFile = new File(path, "bin" + File.separator + array.get(0));
        if (commandFile.exists())
            command.add(commandFile.getPath());
        else
            command.add((String) array.get(0));

        if (array.size() > 1)
            command.addAll((List) array.subList(1, array.size()));

        return command;
    }

    @Override
    protected File getWorkingDir() {
        return new File(path, "bin");
    }

    @Override
    protected void doStart(ICompletionHandler completionHandler) {
        executeActions((JsonArray) parameters.get("startActions"), completionHandler);
    }

    @Override
    protected void doPostStart(ICompletionHandler completionHandler) {
        executeActions((JsonArray) parameters.get("postStartActions"), completionHandler);
    }

    @Override
    protected void doStop(ICompletionHandler completionHandler) {
        executeActions((JsonArray) parameters.get("stopActions"), completionHandler);
    }

    @Override
    protected void destroyProcess() {
        super.destroyProcess();

        for (TestProcess process : processes)
            process.stop(0);

        processes.clear();
    }

    @Override
    protected void doDestroy(String path) {
        if (console.exists())
            Files.copy(console, new File(path, "console.log"));
        if (actionsConsole.exists())
            Files.copy(actionsConsole, new File(path, "actionsConsole.log"));

        File logs = new File(this.path, "work" + File.separator + "logs");
        if (logs.exists())
            Files.copy(logs, new File(path, "logs"));

        File dump = new File(this.path, "work" + File.separator + "dump");
        if (dump.exists())
            Files.copy(dump, new File(path, "dump"));

        File snapshot = new File(this.path, "work" + File.separator + "snapshot");
        if (snapshot.exists())
            Files.copy(snapshot, new File(path, "snapshot"));

        File profiler = new File(this.path, "work" + File.separator + "profiler");
        if (profiler.exists())
            Files.copy(profiler, new File(path, "profiler"));

        File tests = new File(this.path, "work" + File.separator + "tests");
        if (tests.exists())
            Files.copy(tests, new File(path, "tests"));

        File perftests = new File(this.path, "work" + File.separator + "perftests");
        if (perftests.exists())
            Files.copy(perftests, new File(path, "perftests"));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.destroyed(this.path));
    }

    @Override
    protected void configureProcessBuilder(ProcessBuilder processBuilder, Map<String, Object> parameters) {
        super.configureProcessBuilder(processBuilder, parameters);

        Map<String, String> env = processBuilder.environment();
        env.put("EXA_HOME", path.getPath());
        if (Boolean.TRUE.equals(parameters.get("debug")))
            env.put("EXA_DEBUG", "true");
    }

    private void executeActions(JsonArray actions, final ICompletionHandler completionHandler) {
        ICompletionHandler prevHandler = completionHandler;
        if (actions != null) {
            for (int i = actions.size() - 1; i >= 0; i--) {
                Object element = actions.get(i);
                final String action;
                final Map<String, Object> actionParameters;
                if (element instanceof String) {
                    action = (String) element;
                    actionParameters = null;
                } else {
                    JsonObject object = (JsonObject) (element);
                    action = object.get("name");
                    actionParameters = object.get("parameters");
                }

                final ICompletionHandler handler = prevHandler;
                prevHandler = new CompletionHandler() {
                    @Override
                    public void onSucceeded(Object result) {
                        execute(action, actionParameters, handler);
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        completionHandler.onFailed(error);
                    }
                };
            }
        }

        prevHandler.onSucceeded(null);
    }

    private void runProcess(final String action, final Map<String, Object> parameters, ICompletionHandler completionHandler) {
        if (!path.exists()) {
            completionHandler.onSucceeded(null);
            return;
        }

        List<String> command = buildCommand(parameters);
        File workingDir = getWorkingDir();

        final File temp = Files.createTempFile("console", ".log");
        ProcessBuilder processBuilder = new ProcessBuilder(command).directory(workingDir).redirectError(temp).redirectOutput(temp);
        configureProcessBuilder(processBuilder, parameters);
        final TestProcess process = new TestProcess(processBuilder);

        processes.add(process);

        compartment.execute(new CompletionCompartmentTask(completionHandler) {
            @Override
            public Object execute() {
                process.start();
                process.waitFor();
                return null;
            }

            @Override
            protected void onCompleted(Object result) {
                processes.remove(process);

                if (path.exists()) {
                    String actions = "";
                    if (actionsConsole.exists())
                        actions = Files.read(actionsConsole);
                    if (!actions.isEmpty())
                        actions += "\n\n";
                    actions += messages.actionConsole(path, action, parameters, Files.read(temp));

                    Files.write(actionsConsole, actions);
                }

                temp.delete();
            }
        });
    }

    private void dump(final Map<String, Object> parameters, ICompletionHandler completionHandler) throws Exception {
        URL url = new URL("https://localhost:" + parameters.get("port") + "/api/ops/dump");
        JsonObject query = (JsonObject) parameters.get("query");
        post(url, query.toString(), completionHandler, compartment);
    }

    private void snapshot(final Map<String, Object> parameters, ICompletionHandler completionHandler) throws Exception {
        URL url = new URL("https://localhost:" + parameters.get("port") + "/api/ops/snapshot");
        post(url, "", completionHandler, compartment);
    }

    private static void post(final URL url, final String query, ICompletionHandler completionHandler, ICompartment compartment) {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("Admin", "adminadmin".toCharArray());
            }
        });

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                return true;
            }
        });

        compartment.execute(new CompletionCompartmentTask(completionHandler) {
            @Override
            public Object execute() {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setSSLSocketFactory(createSslSocketFactory());
                    connection.setRequestMethod("POST");

                    connection.setDoOutput(true);

                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                    out.write(query);
                    out.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    connection.getContentLength();

                    while (in.readLine() != null)
                        ;
                    in.close();
                } catch (Exception e) {
                    Exceptions.wrapAndThrow(e);
                }

                return null;
            }
        });
    }

    private void attach(Map<String, Object> parameters, ICompletionHandler completionHandler) throws Exception {
        runProcess("attach", new MapBuilder().put("command",
                Json.array().add(parameters.get("command")).add("-s").add(Long.toString(getPid())).toArray()).toMap(), completionHandler);
    }

    private static SSLSocketFactory createSslSocketFactory() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        File file = new File(System.getProperty("com.exametrika.home"), "conf" + File.separator + "keystore-web.jks");
        ByteArray bytes = Files.readBytes(file);
        keyStore.load(new ByteInputStream(bytes.getBuffer(), bytes.getOffset(), bytes.getLength()), "testtest".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "testtest".toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private interface IMessages {
        @DefaultMessage("Test case executor has been destroyed. Path: {0}")
        ILocalizedMessage destroyed(File path);

        @DefaultMessage("Action ''{1}'' with parameters ''{2}'' has been started. Path: {0}")
        ILocalizedMessage actionStarted(File path, String action, Map<String, Object> parameters);

        @DefaultMessage("Action ''{1}'' with parameters ''{2}'' has been succeeded. Path: {0}")
        ILocalizedMessage actionSucceeded(File path, String action, Map<String, Object> parameters);

        @DefaultMessage("Action ''{1}'' with parameters ''{2}'' has been failed. Path: {0}")
        ILocalizedMessage actionFailed(File path, String action, Map<String, Object> parameters);

        @DefaultMessage("Action ''{1}'' has been started. \nPath: {0} \nParameters: \n{2}\n{3}")
        ILocalizedMessage actionConsole(File path, String action, Map<String, Object> parameters, String actionConsole);
    }
}
