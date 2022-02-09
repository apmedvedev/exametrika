/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.IClassTransformer;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.instrument.IReentrancyListener;
import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.Services;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.boot.utils.PathClassLoader;
import com.exametrika.impl.instrument.instrumentors.ClassInstrumentor;
import com.exametrika.impl.instrument.instrumentors.SkipInstrumentationException;
import com.exametrika.spi.instrument.IClassTransformerExtension;
import com.exametrika.spi.instrument.boot.INoTransform;
import com.exametrika.spi.instrument.boot.Interceptors;


/**
 * The {@link ClassTransformer} represents a class transformer which is used at loadtime or runtime and supports class retransformation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ClassTransformer implements ClassFileTransformer, ITimerListener, IClassTransformer {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ClassTransformer.class);
    private final Instrumentation instrumentation;
    private final IInterceptorManager interceptorManager;
    private final IJoinPointFilter joinPointFilter;
    private final IReentrancyListener reentrancyListener;
    private volatile InstrumentationConfiguration configuration;
    private final List<IClassTransformerExtension> transformerExtensions;
    private final AtomicInteger totalTransformedClassesCount = new AtomicInteger();
    private final AtomicInteger totalSkippedClassesCount = new AtomicInteger();
    private final AtomicInteger totalTransformationErrorsCount = new AtomicInteger();
    private final AtomicLong totalTransformationTime = new AtomicLong();
    private final AtomicInteger totalTransformedClassesInitialSize = new AtomicInteger();
    private final AtomicInteger totalTransformedClassesResultingSize = new AtomicInteger();
    private final String computeFrames = System.getProperty("com.exametrika.instrument.computeFrames");
    private Set<String> systemRetransforming = new HashSet<String>();
    private long lastRetransformTime = 0;

    public ClassTransformer(Instrumentation instrumentation, IInterceptorManager interceptorManager, IJoinPointFilter joinPointFilter,
                            IReentrancyListener reentrancyListener, Map<String, String> agentArgs) {
        Assert.notNull(instrumentation);
        Assert.notNull(interceptorManager);
        Assert.notNull(agentArgs);

        this.instrumentation = instrumentation;
        this.interceptorManager = interceptorManager;
        this.joinPointFilter = joinPointFilter;
        this.reentrancyListener = reentrancyListener;

        boolean attached = agentArgs.containsKey("attached");

        transformerExtensions = Services.loadProviders(IClassTransformerExtension.class, getClass().getClassLoader());
        for (IClassTransformerExtension extension : transformerExtensions)
            extension.setAttached(attached);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.created());
    }

    public int getTotalTransformedClassesCount() {
        return totalTransformedClassesCount.get();
    }

    public int getTotalSkippedClassesCount() {
        return totalSkippedClassesCount.get();
    }

    public int getTotalTransformationErrorsCount() {
        return totalTransformationErrorsCount.get();
    }

    public long getTotalTransformationTime() {
        return totalTransformationTime.get();
    }

    public int getTotalTransformedClassesInitialSize() {
        return totalTransformedClassesInitialSize.get();
    }

    public int getTotalTransformedClassesResultingSize() {
        return totalTransformedClassesResultingSize.get();
    }

    public int getJoinPointCount() {
        return ((IJoinPointProvider) interceptorManager).getJoinPointCount();
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        if (className == null)
            return null;

        InstrumentationConfiguration configuration = this.configuration;
        if (configuration == null)
            return null;

        Object param = null;

        long startTime = Times.getCurrentTime();

        String binaryClassName = Type.getObjectType(className).getClassName();

        byte[] result = null;
        boolean error = false, interceptResult = false;
        ;
        if (canTransform(classLoader, binaryClassName, classBeingRedefined != null)) {
            interceptResult = InstrumentInterceptor.INSTANCE.onBeforeTransform();

            if (reentrancyListener != null)
                param = reentrancyListener.onTransformEntered();

            result = classFileBuffer;
            boolean transformed = false;

            for (ClassFileTransformer transformerExtension : transformerExtensions) {
                try {
                    byte[] extensionResult = transformerExtension.transform(classLoader, className, classBeingRedefined, protectionDomain, result);
                    if (extensionResult != null) {
                        result = extensionResult;
                        transformed = true;
                    }
                } catch (Throwable e) {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.transformationError(binaryClassName), e);

                    totalTransformationErrorsCount.incrementAndGet();
                    InstrumentInterceptor.INSTANCE.onTransformError(binaryClassName, e);
                }
            }

            if (!transformed)
                result = null;

            Set<Pointcut> pointcuts = configuration.getPointcuts();
            className = binaryClassName;

            try {
                boolean skip = false;
                if (classBeingRedefined != null) {
                    if (classBeingRedefined.isInterface())
                        skip = true;
                    else {
                        pointcuts = match(classBeingRedefined, pointcuts);
                        if (pointcuts == null)
                            skip = true;
                    }
                } else {
                    pointcuts = match(className, pointcuts);
                    if (pointcuts == null)
                        skip = true;
                }

                if (!skip) {
                    boolean computeFrames;
                    if (this.computeFrames != null)
                        computeFrames = this.computeFrames.equals("true");
                    else {
                        int classMajorVersion = classFileBuffer[7];
                        computeFrames = classMajorVersion >= 51;
                    }

                    int flags = ClassWriter.COMPUTE_MAXS | (computeFrames ? ClassWriter.COMPUTE_FRAMES : 0);

                    ClassReader classReader = new ClassReader(result != null ? result : classFileBuffer);
                    ComputeClassWriter classWriter = new ComputeClassWriter(classReader, flags, classLoader);
                    ClassInstrumentor classInstrumentor = new ClassInstrumentor(classWriter, interceptorManager,
                            classLoader, pointcuts, classBeingRedefined, joinPointFilter, new HashSet<String>());

                    classReader.accept(classInstrumentor, ClassReader.EXPAND_FRAMES);

                    if (!classInstrumentor.getErrorMethods().isEmpty()) {
                        classReader = new ClassReader(result != null ? result : classFileBuffer);
                        classWriter = new ComputeClassWriter(classReader, flags, classLoader);
                        classInstrumentor = new ClassInstrumentor(classWriter, interceptorManager, classLoader,
                                pointcuts, classBeingRedefined, joinPointFilter, classInstrumentor.getErrorMethods());

                        classReader.accept(classInstrumentor, ClassReader.EXPAND_FRAMES);
                    }

                    result = classWriter.toByteArray();
                    if (configuration.isDebug())
                        saveData(configuration, className, result);

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.classTransformed(className));
                }
            } catch (SkipInstrumentationException e) {
            } catch (Throwable e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.transformationError(className), e);

                totalTransformationErrorsCount.incrementAndGet();
                InstrumentInterceptor.INSTANCE.onTransformError(binaryClassName, e);
                error = true;
            }

            if (result != null) {
                totalTransformedClassesCount.incrementAndGet();
                totalTransformedClassesInitialSize.addAndGet(classFileBuffer.length);
                totalTransformedClassesResultingSize.addAndGet(result.length);
                if (interceptResult)
                    InstrumentInterceptor.INSTANCE.onTransformSuccess(classFileBuffer.length, result.length, interceptorManager.getJoinPointCount());
            } else if (!error) {
                totalSkippedClassesCount.incrementAndGet();
                if (interceptResult)
                    InstrumentInterceptor.INSTANCE.onTransformSkip();
            }

            totalTransformationTime.addAndGet(Times.getCurrentTime() - startTime);
            if (interceptResult)
                InstrumentInterceptor.INSTANCE.onAfterTransform();
        }

        if (reentrancyListener != null)
            reentrancyListener.onTransformExited(param);

        return result;
    }

    public void setConfiguration(InstrumentationConfiguration configuration) {
        if (!instrumentation.isRetransformClassesSupported())
            return;

        InstrumentationConfiguration oldConfiguration;

        synchronized (this) {
            oldConfiguration = this.configuration;
            this.configuration = configuration;
        }

        try {
            retransform(oldConfiguration, configuration);
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);

            throw new InstrumentationException(e);
        }
    }

    public void close(boolean fromShutdownHook) {
        InstrumentationConfiguration configuration;
        synchronized (this) {
            configuration = this.configuration;
            if (configuration == null)
                return;

            this.configuration = null;
        }

        try {
            interceptorManager.freeAll();

            if (!fromShutdownHook)
                retransform(configuration, null);

            if (logger.isLogEnabled(LogLevel.INFO))
                logger.log(LogLevel.INFO, messages.closed());
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }

    @Override
    public void onTimer() {
        Set<String> systemRetransforming = null;

        long currentTime = Times.getCurrentTime();
        if (lastRetransformTime == 0 || currentTime > lastRetransformTime + 10000) {
            synchronized (this) {
                if (!this.systemRetransforming.isEmpty()) {
                    systemRetransforming = this.systemRetransforming;
                    this.systemRetransforming = new HashSet<String>();
                }
            }

            lastRetransformTime = currentTime;
        }

        if (systemRetransforming != null)
            retransformClasses(systemRetransforming);

        Interceptors.enableAll();
        interceptorManager.freeUnloaded();
    }

    @Override
    public void retransformClasses(Set<String> classNames) {
        InstrumentationConfiguration configuration = this.configuration;

        if (!instrumentation.isRetransformClassesSupported() || configuration == null)
            return;

        retransform(configuration.getPointcuts(), Collections.<Pointcut>emptySet(), classNames);
    }

    private void retransform(InstrumentationConfiguration oldConfiguration, InstrumentationConfiguration newConfiguration) {
        if (oldConfiguration == null && newConfiguration == null)
            return;

        Set<Pointcut> newPointcuts = new LinkedHashSet<Pointcut>();
        Set<Pointcut> deletedPointcuts = new LinkedHashSet<Pointcut>();
        Set<Pointcut> unmodifiedPointcuts = new LinkedHashSet<Pointcut>();
        if (oldConfiguration == null)
            newPointcuts.addAll(newConfiguration.getPointcuts());
        else if (newConfiguration == null)
            deletedPointcuts.addAll(oldConfiguration.getPointcuts());
        else {
            newPointcuts = getNewPointcuts(oldConfiguration, newConfiguration);
            deletedPointcuts = getDeletedPointcuts(oldConfiguration, newConfiguration);
            unmodifiedPointcuts.addAll(newConfiguration.getPointcuts());
            unmodifiedPointcuts.retainAll(oldConfiguration.getPointcuts());
        }

        retransform(newPointcuts, deletedPointcuts, null);
    }

    private void retransform(Set<Pointcut> newPointcuts, Set<Pointcut> deletedPointcuts, Set<String> classNames) {
        long t = Times.getCurrentTime();

        Class[] loadedClasses = instrumentation.getAllLoadedClasses();
        int retransformCount = 0;
        for (int i = 0; i < loadedClasses.length; i++) {
            Class clazz = loadedClasses[i];
            if (classNames != null && !classNames.contains(clazz.getName()))
                continue;

            if (!instrumentation.isModifiableClass(clazz) || clazz.isInterface() ||
                    (classNames == null && !canTransform(clazz.getClassLoader(), clazz.getName(), true))) {
                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.classSkipped(clazz.getName()));
                continue;
            }

            try {
                boolean matched = false;
                for (Pointcut pointcut : newPointcuts) {
                    if (pointcut.getMethodFilter() == null || pointcut.getMethodFilter().matchClass(clazz)) {
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    for (Pointcut pointcut : deletedPointcuts) {
                        if (pointcut.getMethodFilter() == null || pointcut.getMethodFilter().matchClass(clazz)) {
                            matched = true;
                            interceptorManager.free(clazz.getClassLoader(), clazz.getName());
                            break;
                        }
                    }
                }

                if (matched) {
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.classRetransformed(clazz.getName()));

                    interceptorManager.free(clazz.getClassLoader(), clazz.getName());
                    instrumentation.retransformClasses(clazz);
                    retransformCount++;
                } else if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.classSkipped(clazz.getName()));
            } catch (Throwable e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.transformationError(clazz.getName()), e);
            }
        }

        t = Times.getCurrentTime() - t;

        if (retransformCount > 0 && logger.isLogEnabled(LogLevel.INFO))
            logger.log(LogLevel.INFO, messages.retransformed(loadedClasses.length, retransformCount, t));

        if (retransformCount > 0)
            Interceptors.enableAll();
    }

    private Set<Pointcut> getNewPointcuts(InstrumentationConfiguration oldConfiguration, InstrumentationConfiguration newConfiguration) {
        Set<Pointcut> newPointcuts = new LinkedHashSet<Pointcut>();

        for (Pointcut pointcut : newConfiguration.getPointcuts()) {
            if (!oldConfiguration.getPointcuts().contains(pointcut))
                newPointcuts.add(pointcut);
        }

        return newPointcuts;
    }

    private Set<Pointcut> getDeletedPointcuts(InstrumentationConfiguration oldConfiguration, InstrumentationConfiguration newConfiguration) {
        Set<Pointcut> oldPointcuts = new LinkedHashSet<Pointcut>();

        for (Pointcut pointcut : oldConfiguration.getPointcuts()) {
            if (!newConfiguration.getPointcuts().contains(pointcut))
                oldPointcuts.add(pointcut);
        }

        return oldPointcuts;
    }

    private boolean canTransform(ClassLoader classLoader, String className, boolean retransformed) {
        if (!retransformed && classLoader == null) {
            synchronized (this) {
                systemRetransforming.add(className);
            }
            return false;
        }

        if (classLoader instanceof INoTransform)
            return false;

        if (className.startsWith("com.exametrika") && className.contains(".boot."))
            return false;

        if (Debug.isProfile()) {
            if (className.startsWith("com.exametrika.impl.aggregator") || className.startsWith("com.exametrika.impl.component") ||
                    className.startsWith("com.exametrika.impl.exadb") || className.startsWith("com.exametrika.common.rawdb"))
                return true;
        }

        if (classLoader instanceof PathClassLoader)
            return false;

        while (classLoader != null) {
            if (classLoader == getClass().getClassLoader())
                return false;

            classLoader = classLoader.getParent();
        }

        return true;
    }

    private Set<Pointcut> match(Class clazz, Set<Pointcut> pointcuts) {
        Set<Pointcut> matched = null;
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.getMethodFilter() == null || pointcut.getMethodFilter().matchClass(clazz)) {
                if (matched == null)
                    matched = new LinkedHashSet<Pointcut>();

                matched.add(pointcut);
            }
        }

        return matched;
    }

    private Set<Pointcut> match(String className, Set<Pointcut> pointcuts) {
        Set<Pointcut> matched = null;
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.getMethodFilter() != null && pointcut.getMethodFilter().notMatchClass(className))
                continue;

            if (matched == null)
                matched = new LinkedHashSet<Pointcut>();

            matched.add(pointcut);
        }

        return matched;
    }

    private void saveData(InstrumentationConfiguration configuration, String className, byte[] data) {
        File debugFile = new File(configuration.getDebugPath(), className.replace('.', File.separatorChar) + ".class");
        debugFile.getParentFile().mkdirs();
        OutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(debugFile));
            stream.write(data);
        } catch (IOException e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        } finally {
            IOs.close(stream);
        }
    }

    private interface IMessages {
        @DefaultMessage("Class transformer is created.")
        ILocalizedMessage created();

        @DefaultMessage("Class ''{0}'' has been transformed.")
        ILocalizedMessage classTransformed(String className);

        @DefaultMessage("Class ''{0}'' transformation has been skipped.")
        ILocalizedMessage classSkipped(String className);

        @DefaultMessage("Class ''{0}'' can not be transformed.")
        ILocalizedMessage transformationError(String className);

        @DefaultMessage("Retransformation for class ''{0}'' has been started.")
        ILocalizedMessage classRetransformed(String className);

        @DefaultMessage("Class transformer is closed.")
        ILocalizedMessage closed();

        @DefaultMessage("Class retransformation has been completed: total - {0}, retransforming - {1}, time - {2}.")
        ILocalizedMessage retransformed(int length, int retransformCount, long time);
    }
}
