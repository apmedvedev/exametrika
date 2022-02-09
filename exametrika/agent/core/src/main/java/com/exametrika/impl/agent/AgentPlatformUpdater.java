/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.agent.messages.AgentStartMessage;
import com.exametrika.impl.agent.messages.AgentStartMessageSerializer;
import com.exametrika.impl.agent.messages.PlatformUpdateMessage;
import com.exametrika.impl.agent.messages.PlatformUpdateMessageSerializer;
import com.exametrika.impl.boot.utils.IHotDeployer;


/**
 * The {@link AgentPlatformUpdater} represents an agent part of platform updater.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentPlatformUpdater {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AgentPlatformUpdater.class);
    private final AgentConfiguration configuration;
    private final IHotDeployer hotDeployer;
    private final AgentChannel channel;

    public AgentPlatformUpdater(AgentConfiguration configuration, IHotDeployer hotDeployer, AgentChannel channel) {
        Assert.notNull(configuration);
        Assert.notNull(hotDeployer);
        Assert.notNull(channel);

        this.configuration = configuration;
        this.hotDeployer = hotDeployer;
        this.channel = channel;
    }

    public void register(ISerializationRegistry registry) {
        registry.register(new AgentStartMessageSerializer());
        registry.register(new PlatformUpdateMessageSerializer());
    }

    public void unregister(ISerializationRegistry registry) {
        registry.unregister(AgentStartMessageSerializer.ID);
        registry.unregister(PlatformUpdateMessageSerializer.ID);
    }

    public void requestAgentStart() {
        String configHash = null;
        String modulesHash = null;

        File hashFile = new File(hotDeployer.getConfigPath(), "components/" + configuration.getComponent() + "/platform.json");
        if (hashFile.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(hashFile);
                stream.getChannel().lock(0L, Long.MAX_VALUE, true);
                JsonObject hashObject = JsonSerializers.read(IOs.read(stream, "UTF-8"), false);
                configHash = hashObject.get("configHash", null);
                modulesHash = hashObject.get("modulesHash", null);
            } catch (IOException e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            } finally {
                IOs.close(stream);
            }
        }

        channel.send(new AgentStartMessage(configuration.getComponent(), configHash, modulesHash));
    }

    public void updatePlatform(PlatformUpdateMessage part, List<File> files) {
        try {
            String profilerConfiguration = null;
            if (part.getConfiguration() != null)
                profilerConfiguration = part.getConfiguration();

            File modulesFile = null;
            if (part.getModulesHash() != null)
                modulesFile = files.get(0);

            if (part.getConfigHash() != null || part.getModulesHash() != null) {
                String configHash = null;
                String modulesHash = null;

                File hashFile = new File(hotDeployer.getConfigPath(), "components/" + configuration.getComponent() + "/platform.json");
                if (hashFile.exists()) {
                    FileInputStream stream = null;
                    try {
                        stream = new FileInputStream(hashFile);
                        stream.getChannel().lock(0L, Long.MAX_VALUE, true);
                        JsonObject hashObject = JsonSerializers.read(IOs.read(stream, "UTF-8"), false);
                        configHash = hashObject.get("configHash", null);
                        modulesHash = hashObject.get("modulesHash", null);
                    } catch (IOException e) {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, e);
                    } finally {
                        IOs.close(stream);
                    }
                }

                if (part.getConfigHash() != null)
                    configHash = part.getConfigHash();

                if (part.getModulesHash() != null)
                    modulesHash = part.getModulesHash();

                JsonObject hashObject = Json.object().put("configHash", configHash).put("modulesHash", modulesHash).toObject();
                FileOutputStream stream = null;
                OutputStreamWriter writer = null;
                try {
                    stream = new FileOutputStream(hashFile);
                    stream.getChannel().lock();

                    hotDeployer.update(profilerConfiguration, "components/" + configuration.getComponent(), "profiler.conf", modulesFile);

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.platformUpdated(profilerConfiguration != null, modulesFile != null));

                    writer = new OutputStreamWriter(stream, "UTF-8");
                    JsonSerializers.write(writer, hashObject, true);
                } catch (IOException e) {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                } finally {
                    IOs.close(writer);
                    IOs.close(stream);
                }
            }
        } finally {
            if (files != null) {
                for (File file : files)
                    file.delete();
            }
        }
    }

    private interface IMessages {
        @DefaultMessage("Platform has been updated. Configuration: {0}, modules: {1}.")
        ILocalizedMessage platformUpdated(boolean configurationUpdated, boolean modulesUpdated);
    }
}
