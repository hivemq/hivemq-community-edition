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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.bootstrap.UndefinedClientConnection;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import util.DummyClientConnection;
import util.LogbackCapturingAppender;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class EventLogTest {

    private final @NotNull EventLog eventLog = new EventLog();

    private final @NotNull LogbackCapturingAppender clientConnectedAppender =
            LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_CLIENT_CONNECTED));
    private final @NotNull LogbackCapturingAppender clientDisconnectedAppender =
            LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_CLIENT_DISCONNECTED));
    private final @NotNull LogbackCapturingAppender messageDroppedAppender =
            LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_MESSAGE_DROPPED));
    private final @NotNull LogbackCapturingAppender sessionExpiredAppender =
            LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(EventLog.EVENT_CLIENT_SESSION_EXPIRED));

    private @NotNull StringBuffer logMessageBuffer;

    private final int qos = 1;
    private final @NotNull String topic = "topic/a";
    private final @NotNull String clientId = "clientId_";
    private final @NotNull String reason = "its a reason";
    private final boolean cleanStart = false;
    private final @NotNull Long sessionExpiry = 123L;
    private final @NotNull Channel channel = mock(Channel.class);

    private @NotNull UndefinedClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        clientConnection = new UndefinedClientConnection(channel,
                null,
                new TcpListener(0, "localhost", "")
        );
        clientConnection.setClientSessionExpiryInterval(sessionExpiry);
        clientConnection.setCleanStart(cleanStart);
        clientConnection.setClientId(clientId);

        final Attribute<ClientConnectionContext> clientConnectionAttribute = mock(Attribute.class);
        when(channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME)).thenReturn(clientConnectionAttribute);
        when(clientConnectionAttribute.get()).thenReturn(clientConnection);

        logMessageBuffer = new StringBuffer();
    }

    @After
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void messageDropped() {
        eventLog.messageDropped(clientId, topic, qos, reason);

        logMessageBuffer.append("Outgoing publish message was dropped. Receiving client: ")
                .append(clientId)
                .append(", topic: ")
                .append(topic)
                .append(", qos: ")
                .append(qos)
                .append(", reason: ")
                .append(reason)
                .append(".");

        assertLogging(messageDroppedAppender);
    }

    @Test
    public void sharedSubscriptionMessageDropped() {
        final String group = "hiveMQ";
        eventLog.sharedSubscriptionMessageDropped(group, topic, qos, reason);

        logMessageBuffer.append("Outgoing publish message was dropped. Receiving shared subscription group: ")
                .append(group)
                .append(", topic: ")
                .append(topic)
                .append(", qos: ")
                .append(qos)
                .append(", reason: ")
                .append(reason)
                .append(".");

        assertLogging(messageDroppedAppender);
    }

    @Test
    public void mqttMessageDropped() {
        final String messageType = "myType";
        eventLog.mqttMessageDropped(clientId, messageType, reason);

        logMessageBuffer.append("Outgoing MQTT packet was dropped. Receiving client: ")
                .append(clientId)
                .append(", messageType: ")
                .append(messageType)
                .append(", reason: ")
                .append(reason)
                .append(".");

        assertLogging(messageDroppedAppender);
    }

    @Test
    public void clientConnected_unknown() {
        eventLog.clientConnected(channel, cleanStart);

        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(", IP: ")
                .append("UNKNOWN")
                .append(", Clean Start: ")
                .append(cleanStart)
                .append(", Session Expiry: ")
                .append(sessionExpiry)
                .append(" connected.");

        assertLogging(clientConnectedAppender);
    }

    @Test
    public void clientConnected_with_ip() {

        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1234));

        eventLog.clientConnected(channel, cleanStart);

        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(", IP: ")
                .append("127.0.0.1")
                .append(", Clean Start: ")
                .append(cleanStart)
                .append(", Session Expiry: ")
                .append(sessionExpiry)
                .append(" connected.");

        assertLogging(clientConnectedAppender);
    }

    @Test
    public void clientDisconnected_gracefully() {

        eventLog.clientDisconnectedGracefully(clientConnection, null);

        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(", IP: ")
                .append("UNKNOWN")
                .append(" disconnected gracefully.");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientDisconnected_ungracefully() {

        eventLog.clientDisconnectedUngracefully(clientConnection);

        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(", IP: ")
                .append("UNKNOWN")
                .append(" disconnected ungracefully from TCP Listener on port: 0.");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientWasDisconnected() {
        eventLog.clientWasDisconnected(channel, reason);

        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(", IP: ")
                .append("UNKNOWN")
                .append(" was disconnected.")
                .append(" reason: ")
                .append(reason)
                .append(".");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientWasDisconnected_with_ip() {

        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1234));

        eventLog.clientWasDisconnected(channel, reason);

        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(", IP: ")
                .append("127.0.0.1")
                .append(" was disconnected.")
                .append(" reason: ")
                .append(reason)
                .append(".");

        assertLogging(clientDisconnectedAppender);
    }

    @Test
    public void clientSessionExpired() {
        final long disconnectedSince = 1534251898287L;
        eventLog.clientSessionExpired(disconnectedSince, clientId);

        final ZoneId zoneId = ZoneId.of("UTC");
        final String localizedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(disconnectedSince), zoneId)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logMessageBuffer.append("Client ID: ")
                .append(clientId)
                .append(" session has expired at ")
                .append(localizedDateTime)
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

        assertTrue("The event message was not logged", isLogged);
    }
}
