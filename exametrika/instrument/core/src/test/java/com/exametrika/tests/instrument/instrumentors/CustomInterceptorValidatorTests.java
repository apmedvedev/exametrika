/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.api.instrument.config.ArraySetPointcut;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.api.instrument.config.FieldSetPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.ThrowPointcut;
import com.exametrika.impl.instrument.config.StaticInterceptorClassValidator;
import com.exametrika.spi.instrument.boot.Interceptors;


/**
 * The {@link CustomInterceptorValidatorTests} are tests for {@link StaticInterceptorClassValidator}.
 *
 * @author Medvedev-A
 * @see StaticInterceptorClassValidator
 */
public class CustomInterceptorValidatorTests extends AbstractInstrumentorTests {
    @Test
    public void testValidate() throws Throwable {
        StaticInterceptorClassValidator validator = new StaticInterceptorClassValidator();
        assertThat(validator.validate(ArrayGetPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(ArraySetPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(CallPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(CatchPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(FieldGetPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(FieldSetPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(InterceptPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(LinePointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(MonitorInterceptPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(NewArrayPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(NewObjectPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(ThrowPointcut.class.getName(), Interceptors.class), is(true));
        assertThat(validator.validate(InterceptPointcut.class.getName(), getClass()), is(false));
    }
}
