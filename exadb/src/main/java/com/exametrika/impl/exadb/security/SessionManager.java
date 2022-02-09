/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Times;


/**
 * The {@link SessionManager} is a session manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SessionManager {
    private static final long UPDATE_PERIOD = 10000;
    private final SimpleList<Session> sessions = new SimpleList<Session>();
    private long sessionTimeoutPeriod = 1800000;
    private long lastUpdateTime = Times.getCurrentTime();
    private Session currentSession;

    public void setSessionTimeoutPeriod(long value) {
        sessionTimeoutPeriod = value;
    }

    public SimpleList<Session> getSessions() {
        return sessions;
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(Session session) {
        this.currentSession = session;
    }

    public void addSession(Session session) {
        sessions.addLast(session.getElement());
    }

    public void onTimer(long currentTime) {
        if (currentTime > lastUpdateTime + UPDATE_PERIOD) {
            for (Session session : sessions.values()) {
                if (currentTime > session.getLastAccessTime() + sessionTimeoutPeriod)
                    session.close();
            }

            lastUpdateTime = currentTime;
        }
    }

    public void clearCaches() {
        for (Session session : sessions.values())
            session.getPrincipal().clearCache();
    }

    public void close() {
        for (Session session : sessions.values())
            session.close();
    }

}
