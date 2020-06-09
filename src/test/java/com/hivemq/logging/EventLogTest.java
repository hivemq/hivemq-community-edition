/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class EventLogTest {

    private final EventLog eventLog = new EventLog();

    private final LogbackCapturingAppender clientConnectedAppender = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_CLIENT_CONNECTED));
    private final LogbackCapturingAppender clientDisconnectedAppender = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_CLIENT_DISCONNECTED));
    private final LogbackCapturingAppender messageDroppedAppender = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_MESSAGE_DROPPED));
    private final LogbackCapturingAppender sessionExpiredAppender = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_CLIENT_SESSION_EXPIRED));

    private StringBuffer logMessageBuffer;

    private final int qos = 1;
    private final String topic = "topic/a";
    private final String clientId = "clientId_";
    private final String reason = "its a reason";
    private final boolean cleanStart = false;
    private final Long sessionExpiry = 123L;

    @Mock
    private Channel channel;

    @Mock
    private Attribute attributeClientId, attributeCleanStart, attributeSessionExpiry, attributeDisconnect, attributeDisconnectEventLogged;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(channel.attr(ChannelAttributes.CLIENT_ID)).thenReturn(attributeClientId);
        when(attributeClientId.get()).thenReturn(clientId);

        when(channel.attr(ChannelAttributes.CLEAN_START)).thenReturn(attributeCleanStart);
        when(attributeCleanStart.get()).thenReturn(cleanStart);

        when(channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL)).thenReturn(attributeSessionExpiry);
        when(attributeSessionExpiry.get()).thenReturn(sessionExpiry);

        when(channel.attr(ChannelAttributes.DISCONNECT_EVENT_LOGGED)).thenReturn(attributeDisconnectEventLogged);

        logMessageBuffer = new StringBuffer();
    }

    @Test
    public void messageDropped() {
        eventLog.messageDropped(clientId, topic, qos, reason);

        logMessageBuffer.append("Outgoing publish message was dropped. Receiving client: ").append(clientId)
                .append(", topic: ").append(topic)
                .append(", qos: ").append(qos)
                .append(", reason: ").append(reason).append(".");

        assertLogging(messageDroppedAppender);
    }

    @Test
    public void sharedSubscriptionMessageDropped() {
        final String group = "hiveMQ";
        eventLog.sharedSubscriptionMessageDropped(group, topic, qos, reason);

        logMessageBuffer.append("Outgoing publish message was dropped. Receiving shared subscription group: ").append(group)
                .append(", topic: ").append(topic)
                .append(", qos: ").append(qos)
                .append(", reason: ").append(reason).append(".");

        assertLogging(messageDroppedAppender);
    }

    @Test
    public void mqttMessageDropped() {
        final String messageType = "myType";
        eventLog.mqttMessageDropped(clientId, messageType, reason);

        logMessageBuffer.append("Outgoing MQTT packet was dropped. Receiving client: ").append(clientId)
                .append(", messageType: ").append(messageType)
                .append(", reason: ").append(reason).append(".");

        assertLogging(messageDroppedAppender);
    }

    @Test
    public void clientConnected_unknown() {
        eventLog.clientConnected(channel);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(", IP: ").append("UNKNOWN")
                .append(", Clean Start: ").append(cleanStart)
                .append(", Session Expiry: ").append(sessionExpiry).append(" connected.");

        assertLogging(clientConnectedAppender);
    }

    @Test
    public void clientConnected_with_ip() {

        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1234));

        eventLog.clientConnected(channel);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(", IP: ").append("127.0.0.1")
                .append(", Clean Start: ").append(cleanStart)
                .append(", Session Expiry: ").append(sessionExpiry).append(" connected.");

        assertLogging(clientConnectedAppender);
    }

    @Test
    public void clientDisconnected_gracefully() {

        when(channel.attr(ChannelAttributes.GRACEFUL_DISCONNECT)).thenReturn(attributeDisconnect);
        when(attributeDisconnect.get()).thenReturn(true);

        eventLog.clientDisconnected(channel, null);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(", IP: ").append("UNKNOWN").append(" disconnected gracefully.");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientDisconnected_ungracefully() {

        when(channel.attr(ChannelAttributes.GRACEFUL_DISCONNECT)).thenReturn(attributeDisconnect);
        when(attributeDisconnect.get()).thenReturn(null);

        eventLog.clientDisconnected(channel, null);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(", IP: ").append("UNKNOWN").append(" disconnected ungracefully.");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientWasDisconnected() {
        eventLog.clientWasDisconnected(channel, reason);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(", IP: ").append("UNKNOWN")
                .append(" was disconnected.")
                .append(" reason: ").append(reason).append(".");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientWasDisconnected_with_ip() {

        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1234));

        eventLog.clientWasDisconnected(channel, reason);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(", IP: ").append("127.0.0.1")
                .append(" was disconnected.")
                .append(" reason: ").append(reason).append(".");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientSessionExpired() {
        final long disconnectedSince = 1534251898287L;
        eventLog.clientSessionExpired(disconnectedSince, clientId);

        logMessageBuffer.append("Client ID: ").append(clientId)
                .append(" session has expired at ").append("2018-08-14 13:04:58")
                .append(". All persistent data for this client has been removed.");

        assertLogging(sessionExpiredAppender);
    }

    private void assertLogging(final LogbackCapturingAppender appender) {

        boolean isLogged = false;
        for (final ILoggingEvent event : appender.getCapturedLogs()) {
            if (event.getFormattedMessage().equals(logMessageBuffer.toString())) {
                isLogged = true;
            }
        }

        assertTrue("The dropped message was not logged", isLogged);
    }
}