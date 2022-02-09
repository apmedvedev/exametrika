/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessFinder;

import com.exametrika.impl.instrument.client.ConnectionClassLoader;
import com.exametrika.impl.instrument.client.JdkConnection;
import com.exametrika.impl.instrument.client.JdkConnectionFactory;


/**
 * The {@link Main} is main client entrypoint.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Main {
    private static final String BOOT_JAR = "boot.core.jar";

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("-l"))
            printAvailableProcesses();
        else if (args.length >= 2 && args.length <= 3 && args[0].equals("-s"))
            startInstrumentation(args);
        else if (args.length == 2 && args[0].equals("-f"))
            finishInstrumentation(args);
        else
            printUsage();
    }

    private static void printAvailableProcesses() throws IOException {
        IConnectionFactory factory = createConnectionFactory();
        List<ProcessInfo> list = factory.getAvailableProcesses();
        for (ProcessInfo processInfo : list)
            System.out.println(processInfo);
    }

    private static void startInstrumentation(String[] args) throws IOException {
        String instrumentHome = getInstrumentHome();

        IConnectionFactory factory = createConnectionFactory();

        String pid = getPid(factory, args[1]);
        if (pid == null)
            return;

        String agentPath = new File(instrumentHome, "lib" + File.separator + BOOT_JAR).getAbsolutePath();
        IConnection connection = factory.connect(pid, agentPath);
        if (connection.isStarted()) {
            System.out.println("Exametrika agent is already started.");
            return;
        }

        String configurationPath = null;
        if (args.length == 3)
            configurationPath = args[2];

        connection.start(configurationPath);
    }

    private static void finishInstrumentation(String[] args) throws IOException {
        IConnectionFactory factory = createConnectionFactory();

        String pid = getPid(factory, args[1]);
        if (pid == null)
            return;

        IConnection connection = factory.connect(pid);
        if (connection != null && connection.isStarted())
            connection.stop();
    }

    private static String getPid(IConnectionFactory connectionFactory, String pid) {
        try {
            Integer.parseInt(pid);
            return pid;
        } catch (NumberFormatException e) {
        }

        ProcessFinder finder = new ProcessFinder(new Sigar());
        try {
            long id = finder.findSingleProcess(pid);
            return Long.toString(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Process with identifier '" + pid + "' is not found.");
        return null;
    }

    private static IConnectionFactory createConnectionFactory() {
        try {
            File toolsJar = getToolsJarPath();
            if (toolsJar == null)
                return new JdkConnectionFactory();

            Set<String> classes = new HashSet<String>();
            classes.add(JdkConnectionFactory.class.getName());
            classes.add(JdkConnection.class.getName());
            ConnectionClassLoader classLoader = new ConnectionClassLoader(new URL[]{new URL("file:" + toolsJar.getAbsolutePath())},
                    classes, Main.class.getClassLoader());
            Class clazz = classLoader.loadClass(JdkConnectionFactory.class.getName());
            return (IConnectionFactory) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getInstrumentHome() {
        String instrumentHome = System.getProperty("com.exametrika.home");
        if (instrumentHome == null)
            throw new IllegalArgumentException("Exametrika home is not set.");
        return instrumentHome;
    }

    private static File getToolsJarPath() {
        File toolsJar = new File(new File(System.getProperty("java.home")), "lib" + File.separator + "tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(new File(System.getProperty("java.home")).getParent(), "lib" + File.separator + "tools.jar");
            if (!toolsJar.exists()) {
                toolsJar = new File(getInstrumentHome(), "lib" + File.separator + "tools.jar");
                if (!toolsJar.exists())
                    return null;
            }
        }
        return toolsJar;
    }

    private static void printUsage() {
        System.out.println("Attach agent utility v1.0");
        System.out.println("\nUsage: options");
        System.out.println("\nWhere options can be one of:");
        System.out.println("    -l - prints list of processes available for instrumentation");
        System.out.println("    -s <pid> [<configurationPath>] - starts instrumentation for the process with specified <pid>, ");
        System.out.println("        if configuration is not set, <exametrika_home>/conf/exametrika.conf is used.");
        System.out.println("        Where <pid> can be numeric process identifier or sigar ptql expression.");
        System.out.println("    -f <pid> - finishes instrumentation for the process with specified <pid>");
        System.out.println("        Where <pid> can be numeric process identifier or sigar ptql expression.");
    }
}
