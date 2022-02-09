/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.io.impl.ByteOutputStream;
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
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Serializers;
import com.exametrika.common.utils.Strings;


/**
 * The {@link UltraFastMethodManager} is an ultra fast method manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UltraFastMethodManager {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(UltraFastMethodManager.class);
    private static final Object blocker = new Object();
    private static final MessageDigest digest = getMessageDigest();
    private final File workPath;
    private final int maxInstrumentedMethodsCount;
    private volatile HashSet<String> methods = new HashSet<String>();
    private final TIntSet methodIndexes = new TIntHashSet();
    private final Set<String> classes = new HashSet<String>();
    private boolean methodsChanged;

    public static class UltraFastMethodInfo {
        public Object[] methodBlocks;
        public TIntSet methodIndexes;
        public Set<String> classes;
    }

    public UltraFastMethodManager(File workPath, int maxInstrumentedMethodsCount) {
        Assert.notNull(workPath);

        this.workPath = workPath;
        this.maxInstrumentedMethodsCount = maxInstrumentedMethodsCount;
    }

    public Set<String> get() {
        return methods;
    }

    public synchronized UltraFastMethodInfo getInfo() {
        final UltraFastMethodInfo info = new UltraFastMethodInfo();

        info.methodBlocks = new Object[maxInstrumentedMethodsCount];
        info.methodIndexes = new TIntHashSet(methodIndexes);
        info.classes = new HashSet<String>(classes);

        methodIndexes.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int index) {
                if (index < maxInstrumentedMethodsCount)
                    info.methodBlocks[index] = blocker;
                return true;
            }
        });

        return info;
    }

    public synchronized void add(Set<UltraFastMethod> methods, final Object[] methodBlocks) {
        HashSet<String> namesSet = (HashSet<String>) this.methods.clone();

        List<String> list = new ArrayList<String>();
        for (UltraFastMethod method : methods) {
            if (this.methods.contains(method.name))
                continue;

            namesSet.add(method.name);
            methodIndexes.add(method.index);
            classes.add(method.className);
            list.add(method.name + ":" + method.count + ":" + method.duration);
            methodsChanged = true;
        }

        if (logger.isLogEnabled(LogLevel.DEBUG) && !list.isEmpty())
            logger.log(LogLevel.DEBUG, messages.ultraFastMethodsAdded(Strings.toString(list, true)));

        this.methods = namesSet;

        if (methodBlocks != null) {
            methodIndexes.forEach(new TIntProcedure() {
                @Override
                public boolean execute(int index) {
                    if (index < maxInstrumentedMethodsCount)
                        methodBlocks[index] = blocker;
                    return true;
                }
            });
        }
    }

    public synchronized void load(StackProbeConfiguration configuration, ProfilerConfiguration profilerConfiguration) {
        String configHash = getConfigHash(configuration, profilerConfiguration);

        HashSet<String> methods = new HashSet<String>();

        File file = new File(workPath, "stackProbe.json");
        if (file.exists()) {

            Reader reader = null;
            try {
                reader = new FileReader(file);
                JsonObject object = JsonSerializers.read(reader, false);
                for (String element : (List<String>) object.get("ultraFastMethods"))
                    methods.add(element);
                String loadedConfigHash = object.get("configHash");
                if (!configHash.equals(loadedConfigHash))
                    return;
            } catch (IOException e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            } finally {
                IOs.close(reader);
            }
        }

        this.methods = methods;
        methodsChanged = false;

        if (logger.isLogEnabled(LogLevel.DEBUG) && !methods.isEmpty())
            logger.log(LogLevel.DEBUG, messages.ultraFastMethodsLoaded(Strings.toString(new TreeSet(methods), true)));
    }

    public synchronized void save(StackProbeConfiguration configuration, ProfilerConfiguration profilerConfiguration) {
        if (!methodsChanged)
            return;

        String configHash = getConfigHash(configuration, profilerConfiguration);

        workPath.mkdirs();
        File file = new File(workPath, "stackProbe.json");
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            Json json = Json.object().putArray("ultraFastMethods");
            for (String method : new TreeSet<String>(methods))
                json.add(method.toString());

            json = json.end();
            json.put("configHash", configHash);

            JsonSerializers.write(writer, json.toObject(), true);
        } catch (IOException e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        } finally {
            IOs.close(writer);
        }

        methodsChanged = false;
    }

    private String getConfigHash(StackProbeConfiguration configuration, ProfilerConfiguration profilerConfiguration) {
        ByteOutputStream stream = new ByteOutputStream();
        Serializers.serialize(stream, configuration.getUltraFastMethodThreshold());
        Serializers.serialize(stream, profilerConfiguration.getTimeSource());

        return Strings.digestToString(digest.digest(stream.toByteArray()));
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return Exceptions.wrapAndThrow(e);
        }
    }

    private interface IMessages {
        @DefaultMessage("Ultra-fast methods are added:\n{0}")
        ILocalizedMessage ultraFastMethodsAdded(String methods);

        @DefaultMessage("Ultra-fast methods are loaded:\n{0}")
        ILocalizedMessage ultraFastMethodsLoaded(String methods);
    }
}
