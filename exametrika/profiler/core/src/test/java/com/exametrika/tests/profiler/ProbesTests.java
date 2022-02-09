/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.instrument.StaticJoinPointProvider;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ScopeContainer;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.strategies.ExternalMeasurementStrategy;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestProbe1;
import com.exametrika.tests.profiler.support.TestProbe2;
import com.exametrika.tests.profiler.support.TestProbeCollector1;
import com.exametrika.tests.profiler.support.TestProbeCollector2;
import com.exametrika.tests.profiler.support.TestProbeConfiguration1;
import com.exametrika.tests.profiler.support.TestProbeConfiguration2;
import com.exametrika.tests.profiler.support.TestTimeService;


/**
 * The {@link ProbesTests} are tests for probes.
 *
 * @author Medvedev-A
 */
public class ProbesTests {
    private IJoinPointProvider joinPointProvider = new StaticJoinPointProvider(java.util.Collections.<IJoinPoint>emptyList());
    private TestTimeService timeService = new TestTimeService();
    private IMeasurementHandler measurementHandler = new TestMeasurementHandler();
    private MeasurementStrategyManager measurementStrategyManager = new MeasurementStrategyManager();
    private ThreadLocalAccessor accessor = createAccessor();
    private Sequencer sequencer = new Sequencer();

    @Before
    public void setUp() {
        Times.clearTest();
    }

    @After
    public void tearDown() {
        Times.clearTest();
        accessor.close();
    }

    @Test
    public void testProbes() throws Throwable {
        ExternalMeasurementStrategy strategy = measurementStrategyManager.findMeasurementStrategy("strategy1");
        Map scopeTypes = Tests.get(accessor.getScopeContext(), "scopeTypesMap");
        assertThat(scopeTypes.size(), is(2));

        TestProbe1 probes1 = (TestProbe1) ((List) Tests.get(scopeTypes.get("scopeType1"), "probes")).get(0);
        assertThat(probes1.started, is(true));
        assertThat(probes1.stopped, is(false));
        assertThat(probes1.timered, is(false));
        assertThat(probes1.slot.get() instanceof TestProbe1.TestThreadLocal1, is(true));
        TestProbe2 probes2 = (TestProbe2) ((List) Tests.get(scopeTypes.get("scopeType2"), "probes")).get(0);
        assertThat(probes2.started, is(true));
        assertThat(probes2.timered, is(false));
        assertThat(probes2.stopped, is(false));
        assertThat(probes2.slot.get() instanceof TestProbe2.TestThreadLocal2, is(true));

        accessor.onTimer();

        assertThat(probes1.timered, is(true));
        assertThat(probes2.timered, is(true));

        strategy.setAllowed(false);
        accessor.onTimer();

        assertThat(probes1.enabled, is(false));
        assertThat(probes2.enabled, is(true));
        probes1.started = false;
        probes1.stopped = false;
        probes2.started = false;

        strategy.setAllowed(true);
        accessor.onTimer();

        assertThat(probes1.enabled, is(true));
        assertThat(probes2.enabled, is(true));

        accessor.close();

        assertThat(probes1.stopped, is(true));
        assertThat(probes2.stopped, is(true));
    }

    @Test
    public void testPermanentScopes() throws Throwable {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                ScopeContainer scopesContainer = accessor.get().scopes;
                try {
                    Scope[] scopes = Tests.get(scopesContainer, "activeScopes");
                    assertThat(findScope(scopes, "scope2"), nullValue());
                    Scope scope1 = findScope(scopes, "scope1");
                    assertThat(scope1.isActive(), is(true));
                    assertThat(scope1.isPermanent(), is(true));
                    List<IProbeCollector> collectors = Tests.get(scope1, "collectors");
                    assertThat(collectors.size(), is(1));
                    TestProbeCollector1 collector1 = (TestProbeCollector1) collectors.get(0);
                    assertThat(collector1.activated, is(true));
                    assertThat(collector1.deactivated, is(false));

                    sequencer.allowSingle();
                } catch (Throwable e) {
                    sequencer.denySingle(new SystemException(e));
                }
            }
        });
        thread1.start();
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                ScopeContainer scopesContainer = accessor.get().scopes;
                try {
                    Scope[] scopes = Tests.get(scopesContainer, "activeScopes");
                    Scope scope1 = findScope(scopes, "scope1");
                    Scope scope2 = findScope(scopes, "scope2");
                    assertThat(scope1.isActive(), is(true));
                    assertThat(scope2.isActive(), is(true));
                    assertThat(scope1.isPermanent(), is(true));
                    assertThat(scope2.isPermanent(), is(true));

                    List<IProbeCollector> collectors1 = Tests.get(scope1, "collectors");
                    assertThat(collectors1.size(), is(1));
                    TestProbeCollector1 collector1 = (TestProbeCollector1) collectors1.get(0);
                    assertThat(collector1.activated, is(true));
                    assertThat(collector1.deactivated, is(false));

                    List<IProbeCollector> collectors2 = Tests.get(scope2, "collectors");
                    assertThat(collectors2.size(), is(1));
                    TestProbeCollector2 collector2 = (TestProbeCollector2) collectors2.get(0);
                    assertThat(collector2.activated, is(true));
                    assertThat(collector2.deactivated, is(false));

                    sequencer.allowSingle();
                } catch (Throwable e) {
                    sequencer.denySingle(new SystemException(e));
                }
            }
        }, "TestThread1");
        thread2.start();

        thread1.join();
        thread2.join();

        sequencer.waitAll(2);
    }

    @Test
    public void testScopes() throws Throwable {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                Times.setTest(1000);
                ScopeContainer scopesContainer = accessor.get().scopes;
                try {
                    Scope[] scopes = Tests.get(scopesContainer, "activeScopes");
                    Scope scope1 = findScope(scopes, "scope1");

                    Scope scope3 = scopesContainer.get("scope3", "scopeType1");
                    assertThat(scope1.isActive(), is(true));
                    assertThat(scope3.isActive(), is(false));
                    scope3.begin();

                    assertThat(scopesContainer.get("scope3", "scopeType1") == scope3, is(true));
                    assertThat(findScope(scopes, "scope2"), nullValue());
                    assertThat(findScope(scopes, "scope1"), nullValue());
                    assertThat(findScope(scopes, "scope3") == scope3, is(true));

                    assertThat(scope1.isActive(), is(false));
                    assertThat(scope1.isPermanent(), is(true));
                    List<IProbeCollector> collectors1 = Tests.get(scope1, "collectors");
                    assertThat(collectors1.size(), is(1));
                    TestProbeCollector1 collector1 = (TestProbeCollector1) collectors1.get(0);
                    assertThat(collector1.activated, is(true));
                    assertThat(collector1.deactivated, is(true));
                    collector1.activated = false;

                    assertThat(scope3.isActive(), is(true));
                    assertThat(scope3.isPermanent(), is(false));
                    List<IProbeCollector> collectors3 = Tests.get(scope3, "collectors");
                    assertThat(collectors3.size(), is(1));
                    TestProbeCollector1 collector3 = (TestProbeCollector1) collectors3.get(0);
                    assertThat(collector3.activated, is(true));
                    assertThat(collector3.deactivated, is(false));

                    assertThat(scope3.getTotalTime(2000), is(1000l));
                    Times.setTest(3000);
                    scope3.end();

                    assertThat(scope3.getTotalTime(10000), is(2000l));

                    assertThat(scope3.isActive(), is(false));
                    assertThat(collector3.activated, is(true));
                    assertThat(collector3.deactivated, is(true));

                    assertThat(scope1.isActive(), is(true));
                    assertThat(collector1.activated, is(true));

                    scope3.begin();
                    Times.setTest(4000);
                    scope3.end();

                    assertThat(scope3.getTotalTime(10000), is(3000l));

                    sequencer.allowSingle();
                } catch (Throwable e) {
                    sequencer.denySingle(new SystemException(e));
                }
            }
        });
        thread1.start();
        thread1.join();

        sequencer.waitAll(1);
    }

    @Test
    public void testExtraction() throws Throwable {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                ScopeContainer scopesContainer = accessor.get().scopes;
                try {
                    Scope scope3 = scopesContainer.get("scope3", "scopeType1");
                    Scope scope4 = scopesContainer.get("scope4", "scopeType2");

                    scope3.begin();
                    scope4.begin();
                    assertThat(scope3.isActive(), is(true));
                    assertThat(scope4.isActive(), is(true));
                    scope4.end();

                    List<IProbeCollector> collectors3 = Tests.get(scope3, "collectors");
                    assertThat(collectors3.size(), is(1));
                    TestProbeCollector1 collector3 = (TestProbeCollector1) collectors3.get(0);
                    collector3.extractionRequired = true;
                    assertThat(collector3.extracted, is(false));

                    timeService.time = 2000;

                    sequencer.allowSingle();
                    sequencer.waitSingle();

                    //assertThat(collector3.extracted, is(true));
                    assertThat(scope4.getElement().isRemoved(), is(true));

                    sequencer.allowSingle();
                } catch (Throwable e) {
                    sequencer.denySingle(new SystemException(e));
                }
            }
        }, "TestThread1");
        thread1.start();

        sequencer.waitAll(1);

        accessor.onTimer();

        sequencer.allowAll(1);

        sequencer.waitAll(1);

        thread1.join();
    }

    private Scope findScope(Scope[] scopes, String name) {
        for (int i = 0; i < scopes.length; i++) {
            Scope scope = scopes[i];
            if (scope != null && scope.getName().toString().equals("node." + name))
                return scope;
        }

        return null;
    }

    private ThreadLocalAccessor createAccessor() {
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.THREAD_CPU_TIME, Collections.<MeasurementStrategyConfiguration>asSet(
                new ExternalMeasurementStrategyConfiguration("strategy1", true, 0)),
                Collections.asSet(new ScopeConfiguration("scope1", "scope1", "scopeType1", null), new ScopeConfiguration("scope2", "scope2", "scopeType2",
                        "$.exa.filter('TestThread*', name)"), new ScopeConfiguration("scope3", "scope3", "scopeType3", null)), Collections.<MonitorConfiguration>asSet(), Collections.asSet(
                new TestProbeConfiguration1("test1", "scopeType1", 100, "strategy1", 0),
                new TestProbeConfiguration2("test2", "scopeType2", 100, null, 0)),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);

        measurementStrategyManager.setConfiguration(configuration);

        return new ThreadLocalAccessor(configuration, null, joinPointProvider, null, timeService,
                measurementHandler, measurementStrategyManager, new HashMap(), null, false);
    }
}
