/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.config;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.utils.Collections;


/**
 * The {@link FilterTests} are tests for class and member filter implementations.
 *
 * @author Medvedev-A
 * @see ClassFilter
 * @see ClassNameFilter
 * @see MemberFilter
 * @see MemberNameFilter
 * @see QualifiedMethodFilter
 * @see QualifiedMemberNameFilter
 */
public class FilterTests {
    @Test
    public void testClassFilter() {
        ClassFilter filter1 = new ClassFilter(new ClassNameFilter("test.*"), false, null);
        assertThat(filter1.matchClass("test.TestClass", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter1.matchClass("com.test.TestClass", Collections.<String>asSet(), Collections.<String>asSet()), is(false));
        assertThat(filter1.matchClass("test.TestClass", Collections.<String>asSet(), Collections.<String>asSet(Deprecated.class.getName())), is(true));

        ClassFilter filter2 = new ClassFilter(new ClassNameFilter("test.*", null, null), true, Arrays.asList(new ClassNameFilter("test.*")));
        assertThat(filter2.matchClass("test.TestClass", Collections.<String>asSet(), Collections.<String>asSet()), is(false));
        assertThat(filter2.matchClass("test.TestClass", Collections.<String>asSet(), Collections.<String>asSet("test.TestClass")), is(true));
        assertThat(filter2.matchClass("test.TestClass", Collections.<String>asSet(), Collections.<String>asSet("com.test.TestClass")), is(false));
        assertThat(filter2.matchClass("com.test.TestClass", Collections.<String>asSet(), Collections.<String>asSet()), is(false));
        assertThat(filter2.matchClass("com.test.TestClass", Collections.<String>asSet("test.TestClass"), Collections.<String>asSet()), is(false));
        assertThat(filter2.matchClass("com.test.TestClass", Collections.<String>asSet("test.TestClass"), Collections.<String>asSet("test.TestClass")), is(true));
        assertThat(filter2.matchClass("com.test.TestClass", Collections.<String>asSet("test.TestClass"), Collections.<String>asSet("com.test.TestClass")), is(false));

        ClassFilter filter3 = new ClassFilter(new ClassNameFilter(Test1.class), true, null);
        assertThat(filter3.matchClass(Test2.class), is(true));

        ClassFilter filter4 = new ClassFilter(null, true, Arrays.asList(new ClassNameFilter(Deprecated.class)));
        assertThat(filter4.matchClass(Test2.class), is(true));
        assertThat(filter4.matchClass(Test1.class), is(false));

        ClassFilter filter5 = new ClassFilter(new ClassNameFilter("com.test.TestClass1"), false, null);
        ClassFilter filter6 = new ClassFilter(new ClassNameFilter("com.test.TestClass2"), false, null);
        ClassFilter filter7 = new ClassFilter(new ClassNameFilter("test.TestClass3"), false, null);
        ClassFilter filter8 = new ClassFilter(new ClassNameFilter("test.TestClass4"), false, null);

        ClassFilter filter9 = new ClassFilter(new ClassNameFilter("test.*"), false, null, Arrays.<ClassFilter>asList(filter5, filter6),
                Arrays.<ClassFilter>asList(filter7, filter8));
        assertThat(filter9.matchClass("test.TestClass1", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter9.matchClass("test.TestClass2", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter9.matchClass("com.test.TestClass1", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter9.matchClass("com.test.TestClass2", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter9.matchClass("test.TestClass3", Collections.<String>asSet(), Collections.<String>asSet()), is(false));
        assertThat(filter9.matchClass("test.TestClass4", Collections.<String>asSet(), Collections.<String>asSet()), is(false));

        assertThat(filter1, not(filter2));
        assertThat(filter1, not(filter9));
        assertThat(filter1, is(new ClassFilter(new ClassNameFilter("test.*"), false, null)));
        assertThat(filter9, is(new ClassFilter(new ClassNameFilter("test.*"), false, null, Arrays.<ClassFilter>asList(filter5, filter6),
                Arrays.<ClassFilter>asList(filter7, filter8))));

        ClassFilter filter10 = new ClassFilter(new ClassNameFilter("test.com.TestClass1"), false, null);
        ClassFilter filter11 = new ClassFilter(new ClassNameFilter("test.com.*"), false, null, Arrays.<ClassFilter>asList(),
                Arrays.<ClassFilter>asList(filter10));
        ClassFilter filter12 = new ClassFilter(new ClassNameFilter("test.*"), false, null, Arrays.<ClassFilter>asList(),
                Arrays.<ClassFilter>asList(filter11));

        assertThat(filter12.matchClass("test.TestClass1", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter12.matchClass("test.com.TestClass1", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter12.matchClass("test.com.TestClass2", Collections.<String>asSet(), Collections.<String>asSet()), is(false));

        ClassFilter filter13 = new ClassFilter(null, true, null);
        assertThat(filter13.notMatchClass("test.TestClass1"), is(false));

        ClassFilter filter14 = new ClassFilter(new ClassNameFilter("test.*"), false, null);
        assertThat(filter14.notMatchClass("test.TestClass1"), is(false));
        assertThat(filter14.notMatchClass("com.test.TestClass1"), is(true));

        ClassFilter filter15 = new ClassFilter(new ClassNameFilter(""), false, null, Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("test.*"), false, null)),
                Arrays.<ClassFilter>asList());
        assertThat(filter15.notMatchClass("test.TestClass1"), is(false));
        assertThat(filter15.notMatchClass("com.test.TestClass1"), is(true));

        ClassFilter filter16 = new ClassFilter(new ClassNameFilter("*"), false, null, Arrays.<ClassFilter>asList(),
                Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("test.*"), false, null)));
        assertThat(filter16.notMatchClass("test.TestClass1"), is(true));
        assertThat(filter16.notMatchClass("com.test.TestClass1"), is(false));

        ClassFilter filter17 = new ClassFilter(new ClassNameFilter("*"), false, null, Arrays.<ClassFilter>asList(),
                Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("test.*"), true, null)));
        assertThat(filter17.notMatchClass("test.TestClass1"), is(false));

        ClassFilter filter18 = new ClassFilter(new ClassNameFilter("*"), false, null, Arrays.<ClassFilter>asList(),
                Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("test.*"), false,
                        Arrays.<ClassNameFilter>asList(new ClassNameFilter("test.*")))));
        assertThat(filter18.notMatchClass("test.TestClass1"), is(false));

        ClassFilter filter19 = new ClassFilter(new ClassNameFilter("*"), false, null, Arrays.<ClassFilter>asList(),
                Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("test.*"), false, null,
                        Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("com.test.*"), false, null)),
                        Arrays.<ClassFilter>asList(new ClassFilter(new ClassNameFilter("test.com.*"), false, null)))));

        assertThat(filter19.notMatchClass("test.TestClass1"), is(true));
        assertThat(filter19.notMatchClass("com.test.TestClass1"), is(true));
        assertThat(filter19.notMatchClass("test.com.TestClass1"), is(false));
    }

    @Test
    public void testClassNameFilter() {
        ClassNameFilter filter1 = new ClassNameFilter("test.TestClass");
        assertThat(filter1.matchClass("test.TestClass"), is(true));
        assertThat(filter1.matchClass("test.TestClass1"), is(false));

        ClassNameFilter filter2 = new ClassNameFilter("test.*");
        assertThat(filter2.matchClass("test.TestClass"), is(true));
        assertThat(filter2.matchClass("test.TestClass1"), is(true));
        assertThat(filter2.matchClass("com.test.TestClass1"), is(false));

        ClassNameFilter filter3 = new ClassNameFilter("#.*test.*");
        assertThat(filter3.matchClass("com.test.TestClass"), is(true));
        assertThat(filter3.matchClass("test.TestClass1"), is(true));
        assertThat(filter3.matchClass("com.TestClass1"), is(false));

        ClassNameFilter filter4 = new ClassNameFilter("com.test.TestClass1");
        ClassNameFilter filter5 = new ClassNameFilter("com.test.TestClass2");
        ClassNameFilter filter6 = new ClassNameFilter("test.TestClass3");
        ClassNameFilter filter7 = new ClassNameFilter("test.TestClass4");

        ClassNameFilter filter8 = new ClassNameFilter("test.*", Arrays.<ClassNameFilter>asList(filter4, filter5),
                Arrays.<ClassNameFilter>asList(filter6, filter7));
        assertThat(filter8.matchClass("test.TestClass1"), is(true));
        assertThat(filter8.matchClass("test.TestClass2"), is(true));
        assertThat(filter8.matchClass("com.test.TestClass1"), is(true));
        assertThat(filter8.matchClass("com.test.TestClass2"), is(true));
        assertThat(filter8.matchClass("test.TestClass3"), is(false));
        assertThat(filter8.matchClass("test.TestClass4"), is(false));

        assertThat(filter1, not(filter2));
        assertThat(filter1, not(filter8));
        assertThat(filter1, is(new ClassNameFilter("test.TestClass")));
        assertThat(filter8, is(new ClassNameFilter("test.*", Arrays.<ClassNameFilter>asList(filter4, filter5),
                Arrays.<ClassNameFilter>asList(filter6, filter7))));
    }

    @Test
    public void testMemberFilter() throws Exception {
        MemberFilter filter1 = new MemberFilter(new MemberNameFilter("#test.*"), null);
        assertThat(filter1.matchMember("test.TestMember", Collections.<String>asSet()), is(true));
        assertThat(filter1.matchMember("test.TestMember", Collections.<String>asSet("test.Test")), is(true));
        assertThat(filter1.matchMember("com.test.TestMember", Collections.<String>asSet()), is(false));

        MemberFilter filter2 = new MemberFilter(new MemberNameFilter("#test.*"), Arrays.asList(new ClassNameFilter("test.Test")));
        assertThat(filter2.matchMember("test.TestMember", Collections.<String>asSet()), is(false));
        assertThat(filter2.matchMember("test.TestMember", Collections.<String>asSet("test.Test")), is(true));
        assertThat(filter2.matchMember("test.TestMember", Collections.<String>asSet("test.Test1")), is(false));
        assertThat(filter2.matchMember("com.test.TestMember", Collections.<String>asSet()), is(false));
        assertThat(filter2.matchMember("com.test.TestMember", Collections.<String>asSet("test.Test")), is(false));

        MemberFilter filter3 = new MemberFilter(new MemberNameFilter("f"), null);
        assertThat(filter3.matchMember(Test2.class.getField("f")), is(true));

        MemberFilter filter4 = new MemberFilter(null, Arrays.asList(new ClassNameFilter(Deprecated.class)));
        assertThat(filter4.matchMember(Test2.class.getMethod("m")), is(true));

        MemberFilter filter5 = new MemberFilter(new MemberNameFilter("com.test.TestMember1"), null);
        MemberFilter filter6 = new MemberFilter(new MemberNameFilter("com.test.TestMember2"), null);
        MemberFilter filter7 = new MemberFilter(new MemberNameFilter("test.TestMember3"), null);
        MemberFilter filter8 = new MemberFilter(new MemberNameFilter("test.TestMember4"), null);

        MemberFilter filter9 = new MemberFilter(new MemberNameFilter("#test.*"), null, Arrays.<MemberFilter>asList(filter5, filter6),
                Arrays.<MemberFilter>asList(filter7, filter8));
        assertThat(filter9.matchMember("test.TestMember1", Collections.<String>asSet()), is(true));
        assertThat(filter9.matchMember("test.TestMember2", Collections.<String>asSet()), is(true));
        assertThat(filter9.matchMember("com.test.TestMember1", Collections.<String>asSet()), is(true));
        assertThat(filter9.matchMember("com.test.TestMember2", Collections.<String>asSet()), is(true));
        assertThat(filter9.matchMember("test.TestMember3", Collections.<String>asSet()), is(false));
        assertThat(filter9.matchMember("test.TestMember4", Collections.<String>asSet()), is(false));

        assertThat(filter1, not(filter2));
        assertThat(filter1, not(filter9));
        assertThat(filter1, is(new MemberFilter(new MemberNameFilter("#test.*"), null)));
        assertThat(filter9, is(new MemberFilter(new MemberNameFilter("#test.*"), null, Arrays.<MemberFilter>asList(filter5, filter6),
                Arrays.<MemberFilter>asList(filter7, filter8))));


        MemberFilter filter10 = new MemberFilter(new MemberNameFilter("test.com.TestClass1"), null);
        MemberFilter filter11 = new MemberFilter(new MemberNameFilter("#test\\.com.*"), null, Arrays.<MemberFilter>asList(),
                Arrays.<MemberFilter>asList(filter10));
        MemberFilter filter12 = new MemberFilter(new MemberNameFilter("#test.*"), null, Arrays.<MemberFilter>asList(),
                Arrays.<MemberFilter>asList(filter11));

        assertThat(filter12.matchMember("test.TestClass1", Collections.<String>asSet()), is(true));
        assertThat(filter12.matchMember("test.com.TestClass1", Collections.<String>asSet()), is(true));
        assertThat(filter12.matchMember("test.com.TestClass2", Collections.<String>asSet()), is(false));

        MemberFilter filter13 = new MemberFilter((List) null, null);
        assertThat(filter13.notMatchMember("test.TestClass1"), is(true));

        MemberFilter filter14 = new MemberFilter(new MemberNameFilter("#test.*"), null);
        assertThat(filter14.notMatchMember("test.TestClass1"), is(false));
        assertThat(filter14.notMatchMember("com.test.TestClass1"), is(true));

        MemberFilter filter15 = new MemberFilter(new MemberNameFilter(""), null, Arrays.<MemberFilter>asList(
                new MemberFilter(new MemberNameFilter("#test.*"), null)), Arrays.<MemberFilter>asList());
        assertThat(filter15.notMatchMember("test.TestClass1"), is(false));
        assertThat(filter15.notMatchMember("com.test.TestClass1"), is(true));

        MemberFilter filter16 = new MemberFilter(new MemberNameFilter("#.*"), null, Arrays.<MemberFilter>asList(),
                Arrays.<MemberFilter>asList(new MemberFilter(new MemberNameFilter("#test.*"), null)));
        assertThat(filter16.notMatchMember("test.TestClass1"), is(true));
        assertThat(filter16.notMatchMember("com.test.TestClass1"), is(false));

        MemberFilter filter18 = new MemberFilter(new MemberNameFilter("#.*"), null, Arrays.<MemberFilter>asList(),
                Arrays.<MemberFilter>asList(new MemberFilter(new MemberNameFilter("#test.*"),
                        Arrays.<ClassNameFilter>asList(new ClassNameFilter("#test.*")))));
        assertThat(filter18.notMatchMember("test.TestClass1"), is(false));

        MemberFilter filter19 = new MemberFilter(new MemberNameFilter("#.*"), null, Arrays.<MemberFilter>asList(),
                Arrays.<MemberFilter>asList(new MemberFilter(new MemberNameFilter("#test.*"), null,
                        Arrays.<MemberFilter>asList(new MemberFilter(new MemberNameFilter("#com\\.test.*"), null)),
                        Arrays.<MemberFilter>asList(new MemberFilter(new MemberNameFilter("#test\\.com.*"), null)))));

        assertThat(filter19.notMatchMember("test.TestClass1"), is(true));
        assertThat(filter19.notMatchMember("com.test.TestClass1"), is(true));
        assertThat(filter19.notMatchMember("test.com.TestClass1"), is(false));
    }

    @Test
    public void testMemberNameFilter() {
        MemberNameFilter filter1 = new MemberNameFilter("test.TestMember");
        assertThat(filter1.matchMember("test.TestMember"), is(true));
        assertThat(filter1.matchMember("test.TestMember1"), is(false));

        MemberNameFilter filter3 = new MemberNameFilter("#.*test.*");
        assertThat(filter3.matchMember("com.test.TestMember"), is(true));
        assertThat(filter3.matchMember("test.TestMember1"), is(true));
        assertThat(filter3.matchMember("com.TestMember1"), is(false));

        MemberNameFilter filter4 = new MemberNameFilter("com.test.TestMember1");
        MemberNameFilter filter5 = new MemberNameFilter("com.test.TestMember2");
        MemberNameFilter filter6 = new MemberNameFilter("test.TestMember3");
        MemberNameFilter filter7 = new MemberNameFilter("test.TestMember4");

        MemberNameFilter filter8 = new MemberNameFilter("#test.*", Arrays.<MemberNameFilter>asList(filter4, filter5),
                Arrays.<MemberNameFilter>asList(filter6, filter7));
        assertThat(filter8.matchMember("test.TestMember1"), is(true));
        assertThat(filter8.matchMember("test.TestMember2"), is(true));
        assertThat(filter8.matchMember("com.test.TestMember1"), is(true));
        assertThat(filter8.matchMember("com.test.TestMember2"), is(true));
        assertThat(filter8.matchMember("test.TestMember3"), is(false));
        assertThat(filter8.matchMember("test.TestMember4"), is(false));

        assertThat(filter1, not(filter3));
        assertThat(filter1, not(filter8));
        assertThat(filter1, is(new MemberNameFilter("test.TestMember")));
        assertThat(filter8, is(new MemberNameFilter("#test.*", Arrays.<MemberNameFilter>asList(filter4, filter5),
                Arrays.<MemberNameFilter>asList(filter6, filter7))));
    }

    @Test
    public void testQualifiedMemberNameFilter() {
        QualifiedMemberNameFilter filter1 = new QualifiedMemberNameFilter(new ClassNameFilter("Test"), null);
        assertThat(filter1.matchMember("Test", "test"), is(true));
        assertThat(filter1.matchMember("Test2", "test"), is(false));

        filter1 = new QualifiedMemberNameFilter(null, new MemberNameFilter("test"));
        assertThat(filter1.matchMember("Test", "test"), is(true));
        assertThat(filter1.matchMember("Test", "test2"), is(false));

        filter1 = new QualifiedMemberNameFilter(new ClassNameFilter(""), new MemberNameFilter(""), Arrays.asList(new QualifiedMemberNameFilter(new ClassNameFilter("Test"), null)), null);
        assertThat(filter1.matchMember("Test", "test"), is(true));
        assertThat(filter1.matchMember("Test2", "test"), is(false));

        filter1 = new QualifiedMemberNameFilter(new ClassNameFilter("*"), new MemberNameFilter("*"), null,
                Arrays.asList(new QualifiedMemberNameFilter(new ClassNameFilter("Test2"), null)));
        assertThat(filter1.matchMember("Test", "test"), is(true));
        assertThat(filter1.matchMember("Test2", "test"), is(false));

        assertThat(new QualifiedMemberNameFilter(new ClassNameFilter("Test"), new MemberNameFilter("test"),
                        Arrays.asList(new QualifiedMemberNameFilter(new ClassNameFilter("Test2"), null)),
                        Arrays.asList(new QualifiedMemberNameFilter(new ClassNameFilter("Test2"), null))),
                is(new QualifiedMemberNameFilter(new ClassNameFilter("Test"), new MemberNameFilter("test"),
                        Arrays.asList(new QualifiedMemberNameFilter(new ClassNameFilter("Test2"), null)),
                        Arrays.asList(new QualifiedMemberNameFilter(new ClassNameFilter("Test2"), null)))));
        assertThat(new QualifiedMemberNameFilter((String) null, null), is(new QualifiedMemberNameFilter((String) null, null)));
    }

    @Test
    public void testQualifiedMethodFilterMatchClass() {
        QualifiedMethodFilter filter1 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("Test2"), false, null),
                new MemberFilter(new MemberNameFilter("m1(*"), null));
        assertThat(filter1.matchClass((Class) null), is(false));
        assertThat(filter1.matchClass(Test2.class), is(false));
        assertThat(filter1.matchClass(Test1.class), is(false));

        filter1 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("#.*Test2"), false, null),
                new MemberFilter(new MemberNameFilter("m(*"), null));
        assertThat(filter1.matchClass(Test2.class), is(true));
        assertThat(filter1.matchClass(Test1.class), is(false));

        filter1 = new QualifiedMethodFilter(new ClassFilter("*"), new MemberFilter(new MemberNameFilter("*"), null),
                Arrays.asList(new QualifiedMethodFilter((List) null, null)), null, 0, Integer.MAX_VALUE);
        assertThat(filter1.matchClass(Test2.class), is(true));
        assertThat(filter1.matchClass(Test1.class), is(true));

        filter1 = new QualifiedMethodFilter(null, null, null,
                Arrays.asList(new QualifiedMethodFilter((List) null, null)), 0, Integer.MAX_VALUE);
        assertThat(filter1.matchClass(Test2.class), is(false));
        assertThat(filter1.matchClass(Test1.class), is(false));
    }

    @Test
    public void testQualifiedMethodFilterMatchMethod() {
        QualifiedMethodFilter filter1 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("Test1"), false, null),
                new MemberFilter(new MemberNameFilter("m1"), null));
        assertThat(filter1.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m1", Collections.<String>asSet()), is(true));
        assertThat(filter1.matchMethod("Test2", Collections.<String>asSet(), Collections.<String>asSet(), "m1", Collections.<String>asSet()), is(false));
        assertThat(filter1.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m2", Collections.<String>asSet()), is(false));

        QualifiedMethodFilter filter2 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("Test2"), false, null),
                new MemberFilter(new MemberNameFilter("m2"), null));

        QualifiedMethodFilter filter3 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(""), false, null),
                new MemberFilter(new MemberNameFilter(""), null), Arrays.<QualifiedMethodFilter>asList(filter1), Arrays.<QualifiedMethodFilter>asList(), 0, Integer.MAX_VALUE);
        assertThat(filter3.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m1", Collections.<String>asSet()), is(true));
        assertThat(filter3.matchMethod("Test2", Collections.<String>asSet(), Collections.<String>asSet(), "m1", Collections.<String>asSet()), is(false));
        assertThat(filter3.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m2", Collections.<String>asSet()), is(false));

        QualifiedMethodFilter filter4 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("*"), false, null),
                new MemberFilter(new MemberNameFilter("#.*"), null), Arrays.<QualifiedMethodFilter>asList(), Arrays.<QualifiedMethodFilter>asList(filter2), 0, Integer.MAX_VALUE);

        assertThat(filter4.matchMethod("Test2", Collections.<String>asSet(), Collections.<String>asSet(), "m2", Collections.<String>asSet()), is(false));
        assertThat(filter4.matchMethod("Test2", Collections.<String>asSet(), Collections.<String>asSet(), "m1", Collections.<String>asSet()), is(true));
        assertThat(filter4.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m2", Collections.<String>asSet()), is(true));
    }

    @Test
    public void testQualifiedMethodFilterPartialMatchClass() {
        QualifiedMethodFilter filter1 = new QualifiedMethodFilter((ClassFilter) null, null);
        assertThat(filter1.notMatchClass("Test1"), is(false));
        assertThat(filter1.matchClass("Test1"), is(false));
        assertThat(filter1.notMatchClass("Test1", Collections.<String>asSet(), Collections.<String>asSet()), is(false));
        assertThat(filter1.matchClass("Test1", Collections.<String>asSet(), Collections.<String>asSet()), is(false));

        QualifiedMethodFilter filter2 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("Test2"), false, null), new MemberFilter(new MemberNameFilter("m1"), null));
        assertThat(filter2.notMatchClass("Test1"), is(true));
        assertThat(filter2.matchClass("Test2"), is(false));
        assertThat(filter2.notMatchClass("Test1", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter2.matchClass("Test2", Collections.<String>asSet(), Collections.<String>asSet()), is(false));

        QualifiedMethodFilter filter3 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("Test2"), false, null), null);
        assertThat(filter3.notMatchClass("Test1"), is(true));
        assertThat(filter3.matchClass("Test2"), is(false));
        assertThat(filter3.notMatchClass("Test1", Collections.<String>asSet(), Collections.<String>asSet()), is(true));
        assertThat(filter3.matchClass("Test2", Collections.<String>asSet(), Collections.<String>asSet()), is(false));
    }

    @Test
    public void testQualifiedMethodFilterPartialMatchMethod() {
        QualifiedMethodFilter filter1 = new QualifiedMethodFilter((ClassFilter) null, null);
        assertThat(filter1.notMatchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m1"), is(false));
        assertThat(filter1.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m1"), is(true));

        QualifiedMethodFilter filter2 = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("Test1"), false, null), new MemberFilter(new MemberNameFilter("m1"), null));
        assertThat(filter2.notMatchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m1"), is(false));
        assertThat(filter2.notMatchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m2"), is(true));
        assertThat(filter2.matchMethod("Test1", Collections.<String>asSet(), Collections.<String>asSet(), "m1"), is(true));
        assertThat(filter2.matchMethod("Test2", Collections.<String>asSet(), Collections.<String>asSet(), "m1"), is(false));
    }

    @Test
    public void testQualifiedMethodFilterEquals() {
        assertThat(new QualifiedMethodFilter(new ClassFilter(null, false, null), new MemberFilter((List) null, null),
                        Arrays.asList(new QualifiedMethodFilter(new ClassFilter(null, false, null), new MemberFilter((List) null, null))),
                        Arrays.asList(new QualifiedMethodFilter(new ClassFilter(null, false, null), new MemberFilter((List) null, null))), 0, Integer.MAX_VALUE),
                is(new QualifiedMethodFilter(new ClassFilter(null, false, null), new MemberFilter((List) null, null),
                        Arrays.asList(new QualifiedMethodFilter(new ClassFilter(null, false, null), new MemberFilter((List) null, null))),
                        Arrays.asList(new QualifiedMethodFilter(new ClassFilter(null, false, null), new MemberFilter((List) null, null))), 0, Integer.MAX_VALUE)));
        assertThat(new QualifiedMethodFilter((ClassFilter) null, null), is(new QualifiedMethodFilter((ClassFilter) null, null)));
    }

    public static class Test1 {
    }

    @Deprecated
    public static class Test2 extends Test1 {
        @Deprecated
        public int f;

        @Deprecated
        public void m() {
        }

        ;
    }
}
