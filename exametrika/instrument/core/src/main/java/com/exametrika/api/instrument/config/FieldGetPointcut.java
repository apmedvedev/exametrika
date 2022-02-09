/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.instrument.instrumentors.FieldGetInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.spi.instrument.IInstrumentor;
import com.exametrika.spi.instrument.IInterceptorAllocator;
import com.exametrika.spi.instrument.IMethodInstrumentor;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link FieldGetPointcut} represents a field get pointcut.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FieldGetPointcut extends Pointcut {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final QualifiedMemberNameFilter fieldFilter;
    private final boolean useParams;

    /**
     * Creates an object.
     *
     * @param name         pointcut name
     * @param methodFilter intercepted method filter. Can be null
     * @param interceptor  interceptor
     * @param fieldFilter  field filter. Can be null
     * @param useParams    use params
     * @param singleton    if true pointcut generates single join point
     */
    public FieldGetPointcut(String name, QualifiedMethodFilter methodFilter,
                            InterceptorConfiguration interceptor, QualifiedMemberNameFilter fieldFilter, boolean useParams, boolean singleton) {
        super(name, methodFilter, interceptor, singleton, 0);

        this.fieldFilter = fieldFilter;
        this.useParams = useParams;
    }

    public QualifiedMemberNameFilter getFieldFilter() {
        return fieldFilter;
    }

    public boolean getUseParams() {
        return useParams;
    }

    @Override
    public IInstrumentor createInstrumentor(IInterceptorAllocator interceptorAllocator, String className, String methodName,
                                            String methodSignature, int overloadNumber, boolean isStatic, IMethodInstrumentor generator, ClassLoader classLoader,
                                            IJoinPointFilter joinPointFilter, String sourceFileName, String sourceDebug) {
        return new FieldGetInstrumentor(this, interceptorAllocator, className, methodName, methodSignature, overloadNumber,
                isStatic, (MethodInstrumentor) generator, classLoader, joinPointFilter, sourceFileName, sourceDebug);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldGetPointcut))
            return false;

        FieldGetPointcut pointcut = (FieldGetPointcut) o;
        return super.equals(o) && Objects.equals(fieldFilter, pointcut.fieldFilter) &&
                Objects.equals(useParams, pointcut.useParams);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fieldFilter, useParams);
    }

    @Override
    public String toString() {
        return super.toString() + messages.toString(fieldFilter).toString();
    }

    private interface IMessages {
        @DefaultMessage(", field filter: [{0}]")
        ILocalizedMessage toString(QualifiedMemberNameFilter fieldFilter);
    }
}
