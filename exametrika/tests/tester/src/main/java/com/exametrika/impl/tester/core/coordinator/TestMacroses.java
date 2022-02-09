/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.exametrika.api.tester.config.TestAgentConnectionConfiguration;
import com.exametrika.common.json.JsonMacroses.Argument;
import com.exametrika.common.json.JsonMacroses.IMacro;
import com.exametrika.common.json.JsonMacroses.MacroDefinition;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.schema.IJsonValidationContext.ObjectPathElement;
import com.exametrika.common.json.schema.IJsonValidationContext.PathElement;
import com.exametrika.common.json.schema.JsonDiagnostics;
import com.exametrika.common.json.schema.JsonValidationContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Files;


/**
 * The {@link TestMacroses} is a build-in test macroses.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestMacroses {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestMacroses.class);
    private static TestCoordinatorService service;

    public static void setTestCoordinatorService(TestCoordinatorService service) {
        TestMacroses.service = service;
    }

    public static void register(List<MacroDefinition> macroses) {
        macroses.add(new MacroDefinition("agentHost", Collections.<String, Argument>emptyMap(), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                TestAgentConnectionConfiguration configuration = getAgentConfiguration(context);
                if (configuration == null)
                    return null;

                String propertyValue = configuration.getProperties().get("host");
                if (propertyValue != null)
                    return propertyValue;
                else
                    return configuration.getHost();
            }
        }));
        macroses.add(new MacroDefinition("nodeHost", Collections.<String, Argument>singletonMap("node",
                new Argument("node", true, true, null)), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                TestAgentConnectionConfiguration configuration = getNodeAgentConfiguration(context, (String) args.get("node"));
                if (configuration == null)
                    return null;

                String propertyValue = configuration.getProperties().get("host");
                if (propertyValue != null)
                    return propertyValue;
                else
                    return configuration.getHost();
            }
        }));
        macroses.add(new MacroDefinition("shellFile", Collections.<String, Argument>singletonMap("file",
                new Argument("file", true, true, null)), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                TestAgentConnectionConfiguration configuration = getAgentConfiguration(context);
                if (configuration == null)
                    return null;

                String fileName = (String) args.get("file");
                String osType = configuration.getProperties().get("osType");
                if (osType != null && osType.equals("windows"))
                    return fileName + ".bat";
                else
                    return fileName + ".sh";
            }
        }));
        macroses.add(new MacroDefinition("ant", Collections.<String, Argument>emptyMap(), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                TestAgentConnectionConfiguration configuration = getAgentConfiguration(context);
                if (configuration == null)
                    return null;

                String osType = configuration.getProperties().get("osType");
                if (osType != null && osType.equals("windows"))
                    return "ant.bat";
                else
                    return "ant";
            }
        }));
        macroses.add(new MacroDefinition("agentProperty", Collections.<String, Argument>singletonMap("name",
                new Argument("name", true, true, null)), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                TestAgentConnectionConfiguration configuration = getAgentConfiguration(context);
                if (configuration == null)
                    return null;

                String propertyName = (String) args.get("name");
                String propertyValue = configuration.getProperties().get(propertyName);
                if (propertyValue != null)
                    return propertyValue;

                JsonDiagnostics diagnostics = context.getDiagnostics();
                diagnostics.addError(messages.propertyNotFound(diagnostics.getPath(), configuration.getName(), propertyName));
                return null;
            }
        }));
        macroses.add(new MacroDefinition("content", Collections.<String, Argument>emptyMap(), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                JsonDiagnostics diagnostics = context.getDiagnostics();

                File file = new File((String) args.get("file"));
                if (file.exists())
                    return Files.read(file);
                else {
                    diagnostics.addError(messages.fileNotFound(diagnostics.getPath(), (String) args.get("file")));
                    return null;
                }
            }
        }));
        macroses.add(new MacroDefinition("com.exametrika.home", Collections.<String, Argument>emptyMap(), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                return "${com.exametrika.home}";
            }
        }));
        macroses.add(new MacroDefinition("logLevel", Collections.<String, Argument>singletonMap("debug",
                new Argument("debug", false, true, false)), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                if (Boolean.TRUE.equals(args.get("debug")))
                    return "debug";
                else
                    return "error";
            }
        }));

        macroses.add(new MacroDefinition("agentExists", Collections.<String, Argument>singletonMap("properties",
                new Argument("properties", true, true, Collections.emptyMap())), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                JsonDiagnostics diagnostics = context.getDiagnostics();

                Map<String, String> properties = (Map<String, String>) args.get("properties");
                List<TestCoordinatorChannel> channels = service.selectChannels(properties);
                if (!channels.isEmpty())
                    return true;
                else {
                    if (logger.isLogEnabled(LogLevel.WARNING))
                        logger.log(LogLevel.WARNING, messages.agentNotFound(diagnostics.getPath(), properties));

                    return false;
                }
            }
        }));

        macroses.add(new MacroDefinition("selectAgent", Collections.<String, Argument>singletonMap("properties",
                new Argument("properties", true, true, Collections.emptyMap())), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                Map<String, String> properties = (Map<String, String>) args.get("properties");
                List<TestCoordinatorChannel> channels = service.selectChannels(properties);
                if (!channels.isEmpty())
                    return channels.get(0).getConfiguration().getName();
                else
                    return null;
            }
        }));

        macroses.add(new MacroDefinition("selectAgents", Collections.<String, Argument>singletonMap("properties",
                new Argument("properties", true, true, Collections.emptyMap())), new IMacro() {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args) {
                Map<String, String> properties = (Map<String, String>) args.get("properties");
                List<TestCoordinatorChannel> channels = service.selectChannels(properties);
                List<String> agents = new ArrayList<String>();
                for (TestCoordinatorChannel channel : channels)
                    agents.add(channel.getConfiguration().getName());

                return agents;
            }
        }));
    }

    private static TestAgentConnectionConfiguration getAgentConfiguration(JsonValidationContext context) {
        String agent = null;
        if (!context.getPathList().isEmpty()) {
            List<PathElement> path = context.getPathList();
            for (int i = path.size() - 1; i >= 0; i--) {
                PathElement element = path.get(i);
                if (element instanceof ObjectPathElement) {
                    agent = ((ObjectPathElement) element).object.get("agent", null);
                    if (agent != null)
                        break;
                }
            }
        }
        return getAgentConfiguration(context, agent);
    }

    private static TestAgentConnectionConfiguration getNodeAgentConfiguration(JsonValidationContext context, String node) {
        String agent = null;
        if (!context.getPathList().isEmpty()) {
            List<PathElement> path = context.getPathList();
            for (int i = path.size() - 1; i >= 0; i--) {
                PathElement element = path.get(i);
                if (element instanceof ObjectPathElement) {
                    JsonObject nodeObject = ((ObjectPathElement) element).object.get(node, null);
                    if (nodeObject != null) {
                        agent = nodeObject.get("agent", null);
                        break;
                    }
                }
            }
        }
        return getAgentConfiguration(context, agent);
    }

    private static TestAgentConnectionConfiguration getAgentConfiguration(JsonValidationContext context, String agent) {
        JsonDiagnostics diagnostics = context.getDiagnostics();
        if (agent == null) {
            diagnostics.addError(messages.agentPropertyNotFound(diagnostics.getPath()));
            return null;
        }

        TestCoordinatorChannel channel = service.findChannel(agent);
        if (channel != null)
            return channel.getConfiguration();
        else {
            diagnostics.addError(messages.agentNotFound(diagnostics.getPath(), agent));
            return null;
        }
    }

    private TestMacroses() {
    }

    private interface IMessages {
        @DefaultMessage("Validation error of ''{0}''. Property ''agent'' is not found in current context.")
        ILocalizedMessage agentPropertyNotFound(String path);

        @DefaultMessage("Validation error of ''{0}''. Agent ''{1}'' is not connected.")
        ILocalizedMessage agentNotFound(String path, String agentName);

        @DefaultMessage("Validation error of ''{0}''. Property ''{2}'' is not found in agent ''{1}''.")
        ILocalizedMessage propertyNotFound(String path, String agentName, String tagName);

        @DefaultMessage("Validation error of ''{0}''. File ''{1}'' is not found.")
        ILocalizedMessage fileNotFound(String path, String fileName);

        @DefaultMessage("Validation warning of ''{0}''. Test agent with properties ''{1}'' is not found.")
        ILocalizedMessage agentNotFound(String path, Map<String, String> properties);
    }
}
