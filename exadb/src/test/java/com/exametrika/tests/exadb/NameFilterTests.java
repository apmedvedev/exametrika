/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;

import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.core.ops.OperationManager;


/**
 * The {@link NameFilterTests} are tests for {@link NameFilter}.
 *
 * @author Medvedev-A
 * @see OperationManager
 */
public class NameFilterTests {
    @Test
    public void testNameFilter() {
        assertThat(Pattern.compile(Strings.globToRegEx("", true)).matcher("").matches(), is(true));
        assertThat(Pattern.compile(Strings.globToRegEx("", true)).matcher("a").matches(), is(false));
        assertThat(new NameFilter("name1").match("name1"), is(true));
        assertThat(new NameFilter("name1").match("name2"), is(false));
        assertThat(new NameFilter("name*").match("name123"), is(true));
        assertThat(new NameFilter("name*").match("nam123"), is(false));
        assertThat(new NameFilter("#[a-z]*").match("name"), is(true));
        assertThat(new NameFilter("#[a-z]*").match("name1"), is(false));
        assertThat(new NameFilter(null, Arrays.asList(new NameFilter("name*")), null).match("name1"), is(true));
        assertThat(new NameFilter(null, null, Arrays.asList(new NameFilter("name*"))).match("abc"), is(true));
        assertThat(new NameFilter(null, null, Arrays.asList(new NameFilter("name*"))).match("name1"), is(false));
        assertThat(new NameFilter("name*", Arrays.asList(new NameFilter("abc")),
                Arrays.asList(new NameFilter("name2"))).match("name1"), is(true));
        assertThat(new NameFilter("name*", Arrays.asList(new NameFilter("abc")),
                Arrays.asList(new NameFilter("name2"))).match("name2"), is(false));
        assertThat(new NameFilter("name*", Arrays.asList(new NameFilter("abc")),
                Arrays.asList(new NameFilter("name2"))).match("abc"), is(true));
    }
}
