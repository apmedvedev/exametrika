/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.alerts;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message.RecipientType;

import com.exametrika.api.component.config.MailAlertChannelConfiguration;
import com.exametrika.api.component.config.model.MailAlertChannelSchemaConfiguration;
import com.exametrika.common.mail.MailMessage;
import com.exametrika.common.mail.MailTransportStrategy;
import com.exametrika.common.mail.Mailer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.TestMode;
import com.exametrika.spi.component.AlertMessage;
import com.exametrika.spi.component.AlertRecipient;
import com.exametrika.spi.component.IAlertChannel;
import com.exametrika.spi.component.config.AlertChannelConfiguration;


/**
 * The {@link MailAlertChannel} represents a mail alert channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class MailAlertChannel implements IAlertChannel {
    private volatile MailAlertChannelConfiguration configuration;

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return "mail";
    }

    @Override
    public AlertChannelConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(AlertChannelConfiguration configuration) {
        this.configuration = (MailAlertChannelConfiguration) configuration;
    }

    @Override
    public void send(List<AlertMessage> messages) {
        MailAlertChannelConfiguration configuration = this.configuration;
        if (configuration == null)
            return;

        List<MailMessage> mailMessages = new ArrayList<MailMessage>();
        for (AlertMessage message : messages) {
            MailAlertChannelSchemaConfiguration schema = (MailAlertChannelSchemaConfiguration) message.getSchema();
            MailMessage mailMessage = new MailMessage();

            for (AlertRecipient recipient : message.getRecipients())
                mailMessage.addRecipient(recipient.getName(), recipient.getAddress(), RecipientType.TO);

            String subject;
            switch (message.getType()) {
                case ON:
                    subject = schema.getOnSubject();
                    break;
                case OFF:
                    subject = schema.getOffSubject();
                    break;
                case STATUS:
                    subject = schema.getStatusSubject();
                    break;
                default:
                    Assert.error();
                    return;
            }

            mailMessage.setSubject(subject);
            if (schema.isFormatted())
                mailMessage.setTextHTML(message.getText());
            else
                mailMessage.setText(message.getText());

            String senderName, senderAddress;
            if (schema.getSenderName() != null) {
                senderName = schema.getSenderName();
                senderAddress = schema.getSenderAddress();
            } else {
                senderName = configuration.getSenderName();
                senderAddress = configuration.getSenderAddress();
            }

            mailMessage.setFromAddress(senderName, senderAddress);
            mailMessages.add(mailMessage);
        }

        if (!TestMode.isTest()) {
            Mailer mailer = new Mailer(configuration.getHost(), configuration.getPort(), configuration.getUserName(), configuration.getPassword(),
                    configuration.isSecured() ? MailTransportStrategy.SMTP_SSL : MailTransportStrategy.SMTP_PLAIN, configuration.getSendDelay());

            mailer.sendMail(mailMessages);
        }
    }
}
