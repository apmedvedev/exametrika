/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.component.config.AlertServiceConfiguration;
import com.exametrika.api.component.config.MailAlertChannelConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.component.config.AlertChannelConfiguration;


/**
 * The {@link ComponentConfigurationLoader} is a loader of {@link AlertServiceConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("AlertService")) {
            long schedulePeriod = element.get("schedulePeriod");
            Map<String, AlertChannelConfiguration> channels = new LinkedHashMap<String, AlertChannelConfiguration>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("channels")) {
                AlertChannelConfiguration channel = loadAlertChannel(entry.getKey(), (JsonObject) entry.getValue(), context);
                channels.put(entry.getKey(), channel);
            }

            return new AlertServiceConfiguration(schedulePeriod, channels);
        } else
            throw new InvalidConfigurationException();
    }

    private AlertChannelConfiguration loadAlertChannel(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("MailAlertChannel")) {
            String host = element.get("host");
            long port = element.get("port");
            String userName = element.get("userName", null);
            String password = element.get("password", null);
            boolean secured = element.get("secured");
            String senderName = element.get("senderName");
            String senderAddress = element.get("senderAddress");
            long sendDelay = element.get("sendDelay");

            return new MailAlertChannelConfiguration(name, host, (int) port, userName, password, secured, senderName, senderAddress, sendDelay);
        } else
            return load(name, type, element, context);
    }
}