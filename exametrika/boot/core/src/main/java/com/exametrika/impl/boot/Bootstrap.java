/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.exametrika.api.boot.IAgentMXBean;
import com.exametrika.api.boot.config.BootConfiguration;
import com.exametrika.impl.boot.config.BootConfigurationLoader;
import com.exametrika.impl.boot.utils.HotDeployer;
import com.exametrika.impl.boot.utils.IHotDeployController;
import com.exametrika.impl.boot.utils.Loggers;
import com.exametrika.impl.boot.utils.PathClassLoader;
import com.exametrika.impl.boot.utils.Utils;

/**
 * The {@link Bootstrap} represents a bootstrap of platform runtime.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Bootstrap implements IAgentMXBean, IHotDeployController {
    private static final String VERSION = "1.0.0";
    private static final String SERVICE_CONTAINER_CLASS_NAME = "com.exametrika.common.services.impl.ServiceContainer";
    private static final String SERVICES_CLASS_NAME = "com.exametrika.common.services.Services";
    private static final String BOOT_JAR = "boot.core.jar";
    private static Bootstrap bootstrap;
    private final Instrumentation instrumentation;
    private final Map<String, String> agentArgs;
    private final String home;
    private PathClassLoader classLoader;
    private Object container;
    private HotDeployer hotDeployer;
    private boolean started;
    private String serviceConfigurationPath;
    private String bootConfigurationPath;
    private BootConfiguration configuration;
    private boolean bootClassPathSet;
    private final Stopper stopper = new Stopper();
    private final boolean attached;
    public static volatile boolean calibrated = true;

    public static synchronized void premain(String agentArgs, Instrumentation instrumentation) {
        if (bootstrap != null)
            throw new IllegalStateException();

        calibrated = false;
        initWorkPath();
        initNodeName(false);

        AgentArgs args = getAgentArgs(agentArgs, true);
        bootstrap = new Bootstrap(getHome(), instrumentation, args.args, false, "agent");

        bootstrap.start(args.configPath);
    }

    public static synchronized void agentmain(String agentModulePath, Instrumentation instrumentation) {
        if (bootstrap != null)
            throw new IllegalStateException();

        initWorkPath();
        initNodeName(false);
        bootstrap = new Bootstrap(getHome(agentModulePath), instrumentation, Collections.singletonMap("attached", "true"), true, "agent");
    }

    public static synchronized void main(String[] args) throws Exception {
        try {
            while (!calibrated)
                Thread.sleep(100);
        } catch (Throwable e) {
        }

        if (bootstrap == null) {
            if (args.length < 2)
                throw new IllegalArgumentException("Service configuration path must be specified.");

            System.setProperty("com.exametrika.hostAgent", "true");

            initWorkPath();
            initNodeName(true);
            AgentArgs agentArgs;
            if (args.length >= 3)
                agentArgs = getAgentArgs(args[2], false);
            else
                agentArgs = new AgentArgs();
            bootstrap = new Bootstrap(getHome(), null, agentArgs.args, false, args[1]);
            bootstrap.start(args[0]);
        }

        System.out.println("Press 'q' to exit...");
        while (System.in.read() != 'q')
            Thread.sleep(1000);

        bootstrap.stop();
    }

    public static Bootstrap getInstance() {
        return bootstrap;
    }

    public boolean isAttached() {
        return attached;
    }

    public String getPlatformHome() {
        return home;
    }

    public String getPlatformVersion() {
        return VERSION;
    }

    public String getPlatformBootConfigurationPath() {
        return bootConfigurationPath;
    }

    public Object getServiceContainer() {
        return container;
    }

    @Override
    public synchronized void start(String serviceConfigurationPath) {
        if (started)
            throw new IllegalStateException();

        Loggers.logDebug(getClass().getName(), "Platform is started. Platform home - ''{0}''. Service configuration - ''{1}''.",
                home, serviceConfigurationPath);

        try {
            started = true;
            this.bootConfigurationPath = getBootConfigurationPath();

            Loggers.logDebug(getClass().getName(), "Boot configuration - ''{0}''.", bootConfigurationPath);

            String expandedPath = getPath(serviceConfigurationPath);
            if (serviceConfigurationPath != null && !serviceConfigurationPath.isEmpty() && expandedPath == null)
                throw new IllegalArgumentException(MessageFormat.format("Service configuration path ''{0}'' is not found.", serviceConfigurationPath));
            this.serviceConfigurationPath = expandedPath;

            BootConfigurationLoader loader = new BootConfigurationLoader();
            configuration = loader.load(bootConfigurationPath);

            setBootClassPath();

            hotDeployer = new HotDeployer(configuration.getHotDeployConfigPath(), configuration.getHotDeployModulesPath(),
                    new File(configuration.getWorkPath(), "hotdeploy"), this, configuration.getHotDeployDetectionPeriod(),
                    configuration.getHotDeployRedeploymentPeriod(), configuration.getHotDeployRestartDelayPeriod());

            hotDeployer.start();

            createContainer();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        Runtime.getRuntime().addShutdownHook(stopper);
    }

    @Override
    public synchronized boolean isStarted() {
        return started;
    }

    @Override
    public synchronized void setConfiguration(String configurationPath) {
        if (configurationPath == null || configurationPath.isEmpty())
            throw new IllegalArgumentException();

        if (!started)
            throw new IllegalStateException();

        String expandedPath = getPath(configurationPath);
        if (expandedPath == null)
            throw new IllegalArgumentException(MessageFormat.format("Service configuration path ''{0}'' is not found.",
                    serviceConfigurationPath));

        this.serviceConfigurationPath = expandedPath;
        updateContainerConfiguration();
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public synchronized void onConfigurationChanged() {
        if (!started)
            return;

        Loggers.logDebug(getClass().getName(), "Hot deploy detected that configuration is changed.");

        BootConfigurationLoader loader = new BootConfigurationLoader();
        BootConfiguration configuration = loader.load(bootConfigurationPath);

        if (container != null) {
            if (!this.configuration.getRunModes().equals(configuration.getRunModes()) ||
                    !this.configuration.getBootClassPaths().equals(configuration.getBootClassPaths()) ||
                    !this.configuration.getSystemClassPaths().equals(configuration.getSystemClassPaths()) ||
                    !this.configuration.getClassPaths().equals(configuration.getClassPaths()) ||
                    !this.configuration.getLibraryPaths().equals(configuration.getLibraryPaths()) ||
                    !this.configuration.getSystemPackages().equals(configuration.getSystemPackages()) ||
                    !this.configuration.getWorkPath().equals(configuration.getWorkPath())) {
                destroyContainer(false);

                this.configuration = configuration;

                createContainer();
                return;
            }

            updateContainerConfiguration();
        } else {
            this.configuration = configuration;
            createContainer();
        }
    }

    @Override
    public synchronized void onBeginHotDeploy() {
        if (!started)
            return;

        Loggers.logDebug(getClass().getName(), "Hot deploy is started.");

        destroyContainer(false);
    }

    @Override
    public synchronized void onEndHotDeploy() {
        if (!started)
            return;

        BootConfigurationLoader loader = new BootConfigurationLoader();
        configuration = loader.load(bootConfigurationPath);

        createContainer();

        Loggers.logDebug(getClass().getName(), "Hot deploy is finished.");
    }

    private synchronized void stop(boolean fromShutdownHook) {
        if (!started)
            return;

        started = false;
        bootConfigurationPath = null;
        serviceConfigurationPath = null;

        destroyContainer(fromShutdownHook);

        if (hotDeployer != null) {
            hotDeployer.stop();
            hotDeployer = null;
        }

        configuration = null;

        if (!fromShutdownHook)
            Runtime.getRuntime().removeShutdownHook(stopper);

        Loggers.logDebug(getClass().getName(), "Platform is stopped.");
    }

    private static void initWorkPath() {
        String workPath = System.getProperty(WORKPATH_PROPERTY_NAME);
        if (workPath != null)
            return;

        workPath = System.getenv("EXA_WORKPATH");
        if (workPath != null)
            System.setProperty(WORKPATH_PROPERTY_NAME, workPath);
    }

    private static String getHome() {
        String home = System.getProperty(HOME_PROPERTY_NAME);
        if (home != null)
            return home;

        home = System.getenv("EXA_HOME");
        if (home != null)
            return home;

        String classPath = System.getProperty("java.class.path");
        String[] paths = classPath.split(File.pathSeparator);
        for (String path : paths) {
            if (path.contains(BOOT_JAR))
                return getHome(path);
        }

        throw new IllegalArgumentException("Platform home is not found.");
    }

    private static String getHome(String modulePath) {
        return new File(modulePath).getParentFile().getParent();
    }

    private Bootstrap(String home, Instrumentation instrumentation, Map<String, String> agentArgs, boolean attached, String name) {
        System.out.println("=====================================");
        System.out.println(MessageFormat.format("= Exametrika {0} v''{1}''", name, VERSION));
        System.out.println("=====================================");

        this.home = home;
        this.instrumentation = instrumentation;
        this.agentArgs = agentArgs;
        this.attached = attached;

        System.setProperty(AGENT_PROPERTY_NAME, "true");
        if (System.getProperty(HOME_PROPERTY_NAME) == null)
            System.setProperty(HOME_PROPERTY_NAME, home);
        if (System.getProperty(VERSION_PROPERTY_NAME) == null)
            System.setProperty(VERSION_PROPERTY_NAME, VERSION);

        registerMBean();
    }

    private void registerMBean() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();

            ObjectName name =
                    new ObjectName(MBEAN_NAME);

            server.registerMBean(this, name);
        } catch (Exception e) {
            Loggers.logError(getClass().getName(), e);
        }
    }

    private void createContainer() {
        try {
            List<File> libraryPaths = getPaths(configuration.getLibraryPaths());
            classLoader = new PathClassLoader(getPaths(configuration.getClassPaths()), libraryPaths,
                    configuration.getSystemPackages(), ClassLoader.getSystemClassLoader());

            Class<?> servicesClass = classLoader.loadClass(SERVICES_CLASS_NAME);
            servicesClass.getMethod("setDefaultRunModes", Set.class).invoke(null, configuration.getRunModes());

            Class<?> containerClass = classLoader.loadClass(SERVICE_CONTAINER_CLASS_NAME);
            container = containerClass.getConstructor(Map.class, List.class).newInstance(createParameters(), libraryPaths);

            containerClass.getMethod("start", String.class).invoke(container, serviceConfigurationPath);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void destroyContainer(boolean fromShutdownHook) {
        if (classLoader == null)
            return;

        try {
            if (container != null)
                container.getClass().getMethod("stop", boolean.class).invoke(container, fromShutdownHook);
        } catch (Exception e) {
            Loggers.logError(getClass().getName(), e);
        }

        container = null;

        try {
            if (classLoader != null)
                classLoader.close();
        } catch (IOException e) {
            Loggers.logError(getClass().getName(), e);
        }

        classLoader = null;
    }

    private void updateContainerConfiguration() {
        try {
            container.getClass().getMethod("setConfiguration", String.class).invoke(container, serviceConfigurationPath);
        } catch (Exception e) {
            Loggers.logError(getClass().getName(), e);
        }
    }

    private Map<String, Object> createParameters() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        map.put("home", home);
        map.put("work", configuration.getWorkPath().getPath());
        System.setProperty(WORKPATH_PROPERTY_NAME, configuration.getWorkPath().getPath());
        map.put("agentArgs", agentArgs);

        if (instrumentation != null)
            map.put("instrumentation", instrumentation);

        map.put("runModes", configuration.getRunModes());
        map.put("hotDeployer", hotDeployer);

        if (configuration.isProductionMode())
            map.put("productionMode", true);

        return map;
    }

    private List<File> getPaths(List<File> sourcePaths) {
        File modulesPath = configuration.getHotDeployModulesPath();
        File workPath = hotDeployer.getWorkModulesPath();

        List<File> targetPaths = new ArrayList<File>();
        for (File path : sourcePaths) {
            if (path.equals(modulesPath))
                targetPaths.add(workPath);
            else if (path.getPath().startsWith(modulesPath.getPath())) {
                String relativePath = path.getPath().substring(modulesPath.getPath().length());
                targetPaths.add(new File(workPath, relativePath));
            } else
                targetPaths.add(path);
        }
        return targetPaths;
    }

    private String getBootConfigurationPath() {
        String configurationPath = getPath(System.getProperty("com.exametrika.boot.config"));
        if (configurationPath != null)
            return configurationPath;

        configurationPath = getPath(System.getenv("EXA_BOOT_CONFIG"));
        if (configurationPath != null)
            return configurationPath;

        configurationPath = getPath("${user.home}/.exametrika/boot.conf" + File.pathSeparatorChar +
                "${com.exametrika.home}/conf/boot.conf");
        if (configurationPath != null)
            return configurationPath;

        throw new IllegalArgumentException("Boot configuration is not found.");
    }

    private String getPath(String pathsStr) {
        if (pathsStr == null)
            return null;

        String[] paths = pathsStr.split("[" + File.pathSeparatorChar + "]");
        for (String path : paths) {
            path = Utils.expandProperties(path, false);
            File file = new File(path);
            if (file.exists())
                return path.toString();
        }

        return null;
    }

    private static String initHostName() {
        String hostName = System.getProperty("com.exametrika.hostName");
        if (hostName != null)
            return hostName;

        hostName = System.getenv("HOSTNAME");
        if (hostName == null) {
            hostName = System.getenv("COMPUTERNAME");
            if (hostName == null) {
                try {
                    hostName = InetAddress.getLocalHost().getCanonicalHostName();
                } catch (Exception e) {
                    hostName = "unknownHost";
                }
            }
        }

        System.setProperty("com.exametrika.hostName", hostName);
        return hostName;
    }

    private static void initNodeName(boolean isHost) {
        String hostName = initHostName();

        String nodeName = System.getProperty("com.exametrika.nodeName");
        if (nodeName != null)
            return;

        if (isHost)
            nodeName = hostName;
        else
            nodeName = hostName + System.getProperty("java.class.path").hashCode();

        System.setProperty("com.exametrika.nodeName", nodeName);
    }

    private void setBootClassPath() {
        if (!bootClassPathSet && instrumentation != null) {
            bootClassPathSet = true;
            for (File file : configuration.getBootClassPaths()) {
                if (file.exists()) {
                    try {
                        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(file));
                    } catch (IOException e) {
                        Loggers.logError(getClass().getName(), e);
                    }
                }
            }

            for (File file : configuration.getSystemClassPaths()) {
                if (file.exists()) {
                    try {
                        instrumentation.appendToSystemClassLoaderSearch(new JarFile(file));
                    } catch (IOException e) {
                        Loggers.logError(getClass().getName(), e);
                    }
                }
            }
        }
    }

    private static AgentArgs getAgentArgs(String value, boolean parseConfigPath) {
        AgentArgs agentArgs = new AgentArgs();

        if (value != null) {
            String[] args = value.split(",");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i].trim();
                if (arg.isEmpty())
                    continue;

                if (i == 0 && parseConfigPath)
                    agentArgs.configPath = arg;
                else {
                    int pos = arg.indexOf('=');
                    if (pos != -1)
                        agentArgs.args.put(arg.substring(0, pos), arg.substring(pos + 1));
                    else
                        agentArgs.args.put(arg, "");
                }
            }
        }

        return agentArgs;
    }

    private static class AgentArgs {
        private String configPath;
        private Map<String, String> args = new LinkedHashMap<String, String>();
    }

    private class Stopper extends Thread {
        @Override
        public void run() {
            Bootstrap.this.stop(true);
        }
    }
}
