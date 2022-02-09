/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.config.AlertChannelConfiguration;

/**
 * The {@link MailAlertChannelConfiguration} represents a configuration of mail alert channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class MailAlertChannelConfiguration extends AlertChannelConfiguration {
    private final String host;
    private final int port;
    private final String userName;
    private final String password;
    private final boolean secured;
    private final String senderName;
    private final String senderAddress;
    private final long sendDelay;

    public MailAlertChannelConfiguration(String name, String host, int port, String userName, String password, boolean secured,
                                         String senderName, String senderAddress, long sendDelay) {
        super(name);

        Assert.notNull(host);
        Assert.notNull(senderName);
        Assert.notNull(senderAddress);

        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.secured = secured;
        this.senderName = senderName;
        this.senderAddress = senderAddress;
        this.sendDelay = sendDelay;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSecured() {
        return secured;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public long getSendDelay() {
        return sendDelay;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MailAlertChannelConfiguration))
            return false;

        MailAlertChannelConfiguration configuration = (MailAlertChannelConfiguration) o;
        return super.equals(configuration) && host.equals(configuration.host) && port == configuration.port &&
                Objects.equals(userName, configuration.userName) && Objects.equals(password, configuration.password) &&
                secured == configuration.secured && senderName.equals(configuration.senderName) &&
                senderAddress.equals(configuration.senderAddress) && sendDelay == configuration.sendDelay;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(host, port, userName, password, secured, senderName, senderAddress, sendDelay);
    }
}
