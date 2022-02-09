package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentTimer;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;

public class CompartmentTimer implements ICompartmentTimer {
    private final ICompartmentTimerProcessor handler;
    private boolean started;
    private long period;
    private long nextTimerTime;
    private int attempt;

    public CompartmentTimer(ICompartmentTimerProcessor handler) {
        this(false, 0, handler);
    }

    public CompartmentTimer(boolean started, long period, ICompartmentTimerProcessor handler) {
        Assert.notNull(handler);

        this.started = started;
        this.period = period;
        this.handler = handler;
    }

    @Override
    public long getPeriod() {
        return period;
    }

    @Override
    public void setPeriod(long period) {
        this.period = period;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public int getAttempt() {
        return attempt;
    }

    @Override
    public void start() {
        if (started)
            return;

        restart();
    }

    @Override
    public void restart() {
        started = true;
        nextTimerTime = Times.getCurrentTime() + period;
        attempt = 0;
    }

    @Override
    public void fire() {
        fire(Times.getCurrentTime());
    }

    @Override
    public void delayedFire() {
        started = true;
        nextTimerTime = Times.getCurrentTime();
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public void onTimer(long currentTime) {
        if (started && currentTime >= nextTimerTime) {
            fire(currentTime);
        }
    }

    private void fire(long currentTime) {
        nextTimerTime = currentTime + period;
        attempt++;

        handler.onTimer(currentTime);
    }
}
