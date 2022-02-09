/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.instrument.instrumentors.Instrumentors;


/**
 * The {@link MemberFilter} represents a member (method or field) filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MemberFilter extends Configuration {
    private final MemberNameFilter memberNameFilter;
    private final List<ClassNameFilter> annotations;
    private final List<MemberFilter> includeMembers;
    private final List<MemberFilter> excludeMembers;

    /**
     * Creates a filter.
     *
     * @param memberNameExpression member name expression. Member name expression has the following format:
     *                             #reg_exp_pattern | exact _member_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> exact_member_name - this filter matches if member name equals to exact_member_name pattern
     */
    public MemberFilter(String memberNameExpression) {
        this(new MemberNameFilter(memberNameExpression), null, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param memberNameFilter member name filter. Can be null if not used
     * @param annotations      list of annotation filters. Can be null if not used
     */
    public MemberFilter(MemberNameFilter memberNameFilter, List<ClassNameFilter> annotations) {
        this(memberNameFilter != null ? memberNameFilter : new MemberNameFilter("*"), annotations, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param includeMembers members to include. Can be null if not used
     * @param excludeMembers members to exclude. Can be null if not used
     */
    public MemberFilter(List<MemberFilter> includeMembers, List<MemberFilter> excludeMembers) {
        this(null, null, includeMembers, excludeMembers);
    }

    /**
     * Creates a filter.
     *
     * @param memberNameFilter member name filter. Can be null if not used
     * @param annotations      list of annotation filters. Can be null if not used
     * @param includeMembers   members to include. Can be null if not used
     * @param excludeMembers   members to exclude. Can be null if not used
     */
    public MemberFilter(MemberNameFilter memberNameFilter, List<ClassNameFilter> annotations, List<MemberFilter> includeMembers,
                        List<MemberFilter> excludeMembers) {
        this.memberNameFilter = memberNameFilter;
        this.annotations = Immutables.wrap(annotations);
        this.includeMembers = Immutables.wrap(includeMembers);
        this.excludeMembers = Immutables.wrap(excludeMembers);
    }

    public MemberNameFilter getMemberNameFilter() {
        return memberNameFilter;
    }

    public List<ClassNameFilter> getAnnotations() {
        return annotations;
    }

    public List<MemberFilter> getIncludeMembers() {
        return includeMembers;
    }

    public List<MemberFilter> getExcludeMembers() {
        return excludeMembers;
    }

    /**
     * Matches specified member name against this filter.
     *
     * @param memberName member name to match
     * @return true if member name does not match the filter, false if result of matching is unknown
     */
    public boolean notMatchMember(String memberName) {
        Assert.notNull(memberName);

        boolean res = !matchMemberName(memberName);

        if (res && includeMembers != null && !includeMembers.isEmpty()) {
            for (MemberFilter memberFilter : includeMembers) {
                if (!memberFilter.notMatchMember(memberName)) {
                    res = false;
                    break;
                }
            }
        }

        if (!res && excludeMembers != null && !excludeMembers.isEmpty()) {
            for (MemberFilter memberFilter : excludeMembers) {
                if (memberFilter.matchMember(memberName)) {
                    res = true;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified member name against this filter.
     *
     * @param memberName member name to match
     * @return false if member name matches the filter, false if result of matching is unknown
     */
    public boolean matchMember(String memberName) {
        Assert.notNull(memberName);

        if (annotations != null)
            return false;

        boolean res = matchMemberName(memberName);

        if (!res && includeMembers != null && !includeMembers.isEmpty()) {
            for (MemberFilter memberFilter : includeMembers) {
                if (memberFilter.matchMember(memberName)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMembers != null && !excludeMembers.isEmpty()) {
            for (MemberFilter memberFilter : excludeMembers) {
                if (!memberFilter.notMatchMember(memberName)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified member name against this filter.
     *
     * @param memberName  member name to match
     * @param annotations member annotations to match
     * @return true if member name matches the filter, false if member does not match the filter
     */
    public boolean matchMember(String memberName, Set<String> annotations) {
        Assert.notNull(memberName);

        boolean res = matchMemberName(memberName) && matchAnnotations(annotations);

        if (!res && includeMembers != null && !includeMembers.isEmpty()) {
            for (MemberFilter memberFilter : includeMembers) {
                if (memberFilter.matchMember(memberName, annotations)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMembers != null && !excludeMembers.isEmpty()) {
            for (MemberFilter memberFilter : excludeMembers) {
                if (memberFilter.matchMember(memberName, annotations)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified member against this filter.
     *
     * @param member member to match
     * @return true if member matches the filter, false if member does not match the filter
     */
    public boolean matchMember(Member member) {
        if (member == null)
            return false;

        String name;
        if (member instanceof Constructor)
            name = Instrumentors.getMethodSignature((Constructor) member);
        else if (member instanceof Method)
            name = Instrumentors.getMethodSignature((Method) member);
        else
            name = member.getName();

        boolean res = matchMemberName(name) && matchAnnotations((AnnotatedElement) member);

        if (!res && includeMembers != null && !includeMembers.isEmpty()) {
            for (MemberFilter memberFilter : includeMembers) {
                if (memberFilter.matchMember(member)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMembers != null && !excludeMembers.isEmpty()) {
            for (MemberFilter memberFilter : excludeMembers) {
                if (memberFilter.matchMember(member)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MemberFilter))
            return false;

        MemberFilter memberFilter = (MemberFilter) o;
        return Objects.equals(memberNameFilter, memberFilter.memberNameFilter) && Objects.equals(annotations, memberFilter.annotations) &&
                Objects.equals(includeMembers, memberFilter.includeMembers) && Objects.equals(excludeMembers, memberFilter.excludeMembers);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(memberNameFilter, annotations, includeMembers, excludeMembers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean appended = false;

        if (memberNameFilter != null) {
            builder.append(memberNameFilter);
            appended = true;
        }

        if (annotations != null) {
            if (appended)
                builder.append(", ");

            builder.append("annotations: ");
            builder.append(annotations);
            appended = true;
        }

        if (includeMembers != null) {
            if (appended)
                builder.append(", ");

            builder.append("include: ");
            builder.append(includeMembers);
            appended = true;
        }

        if (excludeMembers != null) {
            if (appended)
                builder.append(", ");

            builder.append("exclude: ");
            builder.append(excludeMembers);
            appended = true;
        }

        return builder.toString();
    }

    private boolean matchAnnotations(AnnotatedElement member) {
        if (annotations == null || annotations.isEmpty())
            return true;

        for (Annotation annotation : member.getAnnotations()) {
            if (matchAnnotation(annotation.annotationType().getName()))
                return true;
        }

        return false;
    }

    private boolean matchAnnotations(Set<String> annotations) {
        if (this.annotations == null || this.annotations.isEmpty())
            return true;

        for (String annotation : annotations) {
            if (matchAnnotation(annotation))
                return true;
        }

        return false;
    }

    private boolean matchAnnotation(String annotationName) {
        for (ClassNameFilter annotation : annotations) {
            if (annotation.matchClass(annotationName))
                return true;
        }

        return false;
    }

    private boolean matchMemberName(String memberName) {
        if (memberNameFilter != null)
            return memberNameFilter.matchMember(memberName);
        else
            return false;
    }
}
