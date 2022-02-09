/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.api.instrument.config.ArraySetPointcut;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.api.instrument.config.FieldSetPointcut;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.instrument.config.ThrowPointcut;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;


/**
 * The {@link InstrumentationConfigurationLoader} is a configuration loader for instrumentation configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstrumentationConfigurationLoader extends AbstractElementLoader implements IExtensionLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        InstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);

        instrumentationContext.setDebug((Boolean) element.get("debug", false));
        instrumentationContext.setMaxJoinPointCount(((Long) element.get("maxJoinPointCount")).intValue());

        JsonObject pointcuts = element.get("pointcuts", null);
        if (pointcuts != null) {
            for (Map.Entry<String, Object> entry : pointcuts) {
                Pointcut pointcut = loadPointcut(entry.getKey(), (JsonObject) entry.getValue(), context);
                if (pointcut != null)
                    instrumentationContext.addPointcut(pointcut);
            }
        }
    }

    @Override
    public Object loadExtension(String name, String type, Object element, ILoadContext context) {
        if (type.equals("ClassNameFilter"))
            return loadClassNameFilter((JsonObject) element);
        else if (type.equals("ClassFilter"))
            return loadClassFilter((JsonObject) element);
        else if (type.equals("MemberNameFilter"))
            return loadMemberNameFilter((JsonObject) element);
        else if (type.equals("MemberFilter"))
            return loadMemberFilter((JsonObject) element);
        else if (type.equals("QualifiedMemberNameFilter"))
            return loadQualifiedMemberNameFilter((JsonObject) element);
        else if (type.equals("QualifiedMethodFilter"))
            return loadQualifiedMethodFilter((JsonObject) element);
        else if (type.equals("CompoundClassFilterExpression"))
            return loadCompoundClassFilter(element);
        else if (type.equals("CompoundClassNameFilterExpression"))
            return loadCompoundClassNameFilter(element);
        else
            throw new InvalidConfigurationException();
    }

    public ClassFilter loadCompoundClassFilter(Object classExpression) {
        ClassFilter classFilter = null;
        if (classExpression instanceof String)
            classFilter = new ClassFilter((String) classExpression);
        else if (classExpression instanceof JsonObject)
            classFilter = loadClassFilter((JsonObject) classExpression);
        return classFilter;
    }

    private Pointcut loadPointcut(String name, JsonObject element, ILoadContext context) {
        if (!(Boolean) element.get("enabled"))
            return null;

        String type = getType(element);

        QualifiedMethodFilter methodFilter = loadQualifiedMethodFilter((JsonObject) element.get("intercepted", null));
        InterceptorConfiguration interceptor = load(null, type, (JsonObject) element.get("interceptor"), context);

        boolean singleton = element.get("singleton");

        Pointcut pointcut;

        if (type.equals("ArrayGetPointcut")) {
            boolean useParams = element.get("useParams");
            pointcut = new ArrayGetPointcut(name, methodFilter, interceptor, useParams, singleton);
        } else if (type.equals("ArraySetPointcut")) {
            boolean useParams = element.get("useParams");
            pointcut = new ArraySetPointcut(name, methodFilter, interceptor, useParams, singleton);
        } else if (type.equals("CallPointcut")) {
            QualifiedMemberNameFilter calledFilter = loadQualifiedMemberNameFilter((JsonObject) element.get("called", null));
            boolean useParams = element.get("useParams");
            pointcut = new CallPointcut(name, methodFilter, interceptor, calledFilter, useParams, singleton, 0);
        } else if (type.equals("CatchPointcut")) {
            ClassNameFilter exceptionFilter = loadClassNameFilter((JsonObject) element.get("exception", null));
            pointcut = new CatchPointcut(name, methodFilter, interceptor, exceptionFilter, singleton);
        } else if (type.equals("FieldGetPointcut")) {
            QualifiedMemberNameFilter fieldFilter = loadQualifiedMemberNameFilter((JsonObject) element.get("field", null));
            boolean useParams = element.get("useParams");
            pointcut = new FieldGetPointcut(name, methodFilter, interceptor, fieldFilter, useParams, singleton);
        } else if (type.equals("FieldSetPointcut")) {
            QualifiedMemberNameFilter fieldFilter = loadQualifiedMemberNameFilter((JsonObject) element.get("field", null));
            boolean useParams = element.get("useParams");
            pointcut = new FieldSetPointcut(name, methodFilter, interceptor, fieldFilter, useParams, singleton);
        } else if (type.equals("InterceptPointcut")) {
            Set<InterceptPointcut.Kind> kinds = loadInterceptPointcutKinds((JsonArray) element.get("kinds"));
            boolean useParams = element.get("useParams");
            pointcut = new InterceptPointcut(name, methodFilter, kinds, interceptor, useParams, singleton, 0);
        } else if (type.equals("LinePointcut")) {
            long startLine = element.get("startLine");
            long endLine = element.get("endLine");
            pointcut = new LinePointcut(name, methodFilter, interceptor, (int) startLine, (int) endLine, singleton);
        } else if (type.equals("MonitorInterceptPointcut")) {
            Set<MonitorInterceptPointcut.Kind> kinds = loadMonitorInterceptPointcutKinds((JsonArray) element.get("kinds"));
            pointcut = new MonitorInterceptPointcut(name, methodFilter, kinds, interceptor, singleton);
        } else if (type.equals("NewArrayPointcut")) {
            ClassNameFilter elementFilter = loadClassNameFilter((JsonObject) element.get("element", null));
            pointcut = new NewArrayPointcut(name, methodFilter, interceptor, elementFilter, singleton);
        } else if (type.equals("NewObjectPointcut")) {
            ClassNameFilter objectFilter = loadClassNameFilter((JsonObject) element.get("object", null));
            pointcut = new NewObjectPointcut(name, methodFilter, interceptor, objectFilter, singleton);
        } else if (type.equals("ThrowPointcut"))
            pointcut = new ThrowPointcut(name, methodFilter, interceptor, singleton);
        else {
            Assert.error();
            pointcut = null;
        }

        if (interceptor instanceof StaticInterceptorConfiguration) {
            StaticInterceptorClassValidator validator = new StaticInterceptorClassValidator();
            Assert.isTrue(validator.validate(pointcut.getClass().getName(),
                    ((StaticInterceptorConfiguration) interceptor).getInterceptorClass()));
        }

        return pointcut;
    }

    private Set<InterceptPointcut.Kind> loadInterceptPointcutKinds(JsonArray array) {
        Set<InterceptPointcut.Kind> kinds = Enums.noneOf(InterceptPointcut.Kind.class);
        for (Object element : array)
            kinds.add(loadInterceptPointcutKind((String) element));

        return kinds;
    }

    private InterceptPointcut.Kind loadInterceptPointcutKind(String part) {
        if (part.equals("enter"))
            return InterceptPointcut.Kind.ENTER;
        else if (part.equals("returnExit"))
            return InterceptPointcut.Kind.RETURN_EXIT;
        else if (part.equals("throwExit"))
            return InterceptPointcut.Kind.THROW_EXIT;
        else
            return Assert.error();
    }

    private Set<MonitorInterceptPointcut.Kind> loadMonitorInterceptPointcutKinds(JsonArray array) {
        Set<MonitorInterceptPointcut.Kind> kinds = Enums.noneOf(MonitorInterceptPointcut.Kind.class);
        for (Object element : array)
            kinds.add(loadMonitorInterceptPointcutKind((String) element));

        return kinds;
    }

    private MonitorInterceptPointcut.Kind loadMonitorInterceptPointcutKind(String part) {
        if (part.equals("beforeEnter"))
            return MonitorInterceptPointcut.Kind.BEFORE_ENTER;
        else if (part.equals("afterEnter"))
            return MonitorInterceptPointcut.Kind.AFTER_ENTER;
        else if (part.equals("beforeExit"))
            return MonitorInterceptPointcut.Kind.BEFORE_EXIT;
        else if (part.equals("afterExit"))
            return MonitorInterceptPointcut.Kind.AFTER_EXIT;
        else
            return Assert.error();
    }

    private ClassNameFilter loadClassNameFilter(JsonObject element) {
        if (element == null)
            return null;

        List<ClassNameFilter> includeClasses = loadClassNameFilters((JsonArray) element.get("include", null));
        List<ClassNameFilter> excludeClasses = loadClassNameFilters((JsonArray) element.get("exclude", null));

        String classNameExpression = element.get("class", null);

        return new ClassNameFilter(classNameExpression, includeClasses, excludeClasses);
    }

    private List<ClassNameFilter> loadClassNameFilters(JsonArray array) {
        if (array == null)
            return null;

        List<ClassNameFilter> filters = new ArrayList<ClassNameFilter>(array.size());
        for (Object element : array)
            filters.add(loadCompoundClassNameFilter(element));

        return filters;
    }

    private ClassFilter loadClassFilter(JsonObject element) {
        if (element == null)
            return null;

        List<ClassFilter> includeClasses = loadClassFilters((JsonArray) element.get("include", null));
        List<ClassFilter> excludeClasses = loadClassFilters((JsonArray) element.get("exclude", null));

        ClassNameFilter classNameFilter = loadCompoundClassNameFilter(element.get("class", null));

        boolean includeSubclasses = element.get("includeSubclasses");
        List<ClassNameFilter> annotations = loadClassNameFilters((JsonArray) element.get("annotations", null));

        return new ClassFilter(classNameFilter, includeSubclasses, annotations, includeClasses, excludeClasses);
    }

    private ClassNameFilter loadCompoundClassNameFilter(Object classNameExpression) {
        ClassNameFilter classNameFilter = null;
        if (classNameExpression instanceof String)
            classNameFilter = new ClassNameFilter((String) classNameExpression);
        else if (classNameExpression instanceof JsonObject)
            classNameFilter = loadClassNameFilter((JsonObject) classNameExpression);
        return classNameFilter;
    }

    private List<ClassFilter> loadClassFilters(JsonArray array) {
        if (array == null)
            return null;

        List<ClassFilter> filters = new ArrayList<ClassFilter>(array.size());
        for (Object element : array)
            filters.add(loadCompoundClassFilter(element));

        return filters;
    }

    private MemberNameFilter loadMemberNameFilter(JsonObject element) {
        if (element == null)
            return null;

        List<MemberNameFilter> includeMembers = loadMemberNameFilters((JsonArray) element.get("include", null));
        List<MemberNameFilter> excludeMembers = loadMemberNameFilters((JsonArray) element.get("exclude", null));

        String memberNameExpression = element.get("member", null);

        return new MemberNameFilter(memberNameExpression, includeMembers, excludeMembers);
    }

    private List<MemberNameFilter> loadMemberNameFilters(JsonArray array) {
        if (array == null)
            return null;

        List<MemberNameFilter> filters = new ArrayList<MemberNameFilter>(array.size());
        for (Object element : array)
            filters.add(loadCompoundMemberNameFilter(element));

        return filters;
    }

    private MemberFilter loadMemberFilter(JsonObject element) {
        if (element == null)
            return null;

        List<MemberFilter> includeMembers = loadMemberFilters((JsonArray) element.get("include", null));
        List<MemberFilter> excludeMembers = loadMemberFilters((JsonArray) element.get("exclude", null));

        MemberNameFilter memberNameFilter = loadCompoundMemberNameFilter(element.get("member", null));

        List<ClassNameFilter> annotations = loadClassNameFilters((JsonArray) element.get("annotations", null));

        return new MemberFilter(memberNameFilter, annotations, includeMembers, excludeMembers);
    }

    private MemberNameFilter loadCompoundMemberNameFilter(Object memberNameExpression) {
        MemberNameFilter memberNameFilter = null;
        if (memberNameExpression instanceof String)
            memberNameFilter = new MemberNameFilter((String) memberNameExpression);
        else if (memberNameExpression instanceof JsonObject)
            memberNameFilter = loadMemberNameFilter((JsonObject) memberNameExpression);
        return memberNameFilter;
    }

    private MemberFilter loadCompoundMemberFilter(Object memberExpression) {
        MemberFilter memberFilter = null;
        if (memberExpression instanceof String)
            memberFilter = new MemberFilter((String) memberExpression);
        else if (memberExpression instanceof JsonObject)
            memberFilter = loadMemberFilter((JsonObject) memberExpression);
        return memberFilter;
    }

    private List<MemberFilter> loadMemberFilters(JsonArray array) {
        if (array == null)
            return null;

        List<MemberFilter> filters = new ArrayList<MemberFilter>(array.size());
        for (Object element : array)
            filters.add(loadCompoundMemberFilter(element));

        return filters;
    }

    private QualifiedMemberNameFilter loadQualifiedMemberNameFilter(JsonObject element) {
        if (element == null)
            return null;

        List<QualifiedMemberNameFilter> includeMembers = loadQualifiedMemberNameFilters((JsonArray) element.get("include", null));
        List<QualifiedMemberNameFilter> excludeMembers = loadQualifiedMemberNameFilters((JsonArray) element.get("exclude", null));

        ClassNameFilter classNameFilter = loadCompoundClassNameFilter(element.get("class", null));
        MemberNameFilter memberNameFilter = loadCompoundMemberNameFilter(element.get("member", null));

        return new QualifiedMemberNameFilter(classNameFilter, memberNameFilter, includeMembers, excludeMembers);
    }

    private List<QualifiedMemberNameFilter> loadQualifiedMemberNameFilters(JsonArray array) {
        if (array == null)
            return null;

        List<QualifiedMemberNameFilter> filters = new ArrayList<QualifiedMemberNameFilter>(array.size());
        for (Object element : array)
            filters.add(loadQualifiedMemberNameFilter((JsonObject) element));

        return filters;
    }

    private QualifiedMethodFilter loadQualifiedMethodFilter(JsonObject element) {
        if (element == null)
            return null;

        List<QualifiedMethodFilter> includeMembers = loadQualifiedMethodFilters((JsonArray) element.get("include", null));
        List<QualifiedMethodFilter> excludeMembers = loadQualifiedMethodFilters((JsonArray) element.get("exclude", null));

        ClassFilter classFilter = loadCompoundClassFilter(element.get("class", null));
        MemberFilter memberFilter = loadCompoundMemberFilter(element.get("method", null));
        long minInstruction = element.get("minInstruction");
        long maxInstruction = element.get("maxInstruction");

        return new QualifiedMethodFilter(classFilter, memberFilter, includeMembers, excludeMembers, (int) minInstruction, (int) maxInstruction);
    }

    private List<QualifiedMethodFilter> loadQualifiedMethodFilters(JsonArray array) {
        if (array == null)
            return null;

        List<QualifiedMethodFilter> filters = new ArrayList<QualifiedMethodFilter>(array.size());
        for (Object element : array)
            filters.add(loadQualifiedMethodFilter((JsonObject) element));

        return filters;
    }
}
