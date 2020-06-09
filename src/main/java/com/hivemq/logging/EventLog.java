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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The EventLog class is used to log certain events that could be important for customers to separate files.
 * In a future state of the implementation it may also be used to display those events in the web-interface.
 *
 * @author Lukas Brandl
 */
@LazySingleton
public class EventLog {

    private static final Logger log = LoggerFactory.getLogger(EventLog.class);

    public static final String EVENT_CLIENT_CONNECTED = "event.client-connected";
    public static final String EVENT_CLIENT_DISCONNECTED = "event.client-disconnected";
    public static final String EVENT_MESSAGE_DROPPED = "event.message-dropped";
    public static final String EVENT_CLIENT_SESSION_EXPIRED = "event.client-session-expired";
    public static final String EVENT_AUTHENTICATION = "event.authentication";
    /**
     * Events are logged to DEBUG, in case customers are using a custom logback.xml
     */

    private static final Logger logClientConnected = LoggerFactory.getLogger(EVENT_CLIENT_CONNECTED);
    private static final Logger logClientDisconnected = LoggerFactory.getLogger(EVENT_CLIENT_DISCONNECTED);
    private static final Logger logMessageDropped = LoggerFactory.getLogger(EVENT_MESSAGE_DROPPED);
    private static final Logger logClientSessionExpired = LoggerFactory.getLogger(EVENT_CLIENT_SESSION_EXPIRED);
    private static final Logger logAuthentication = LoggerFactory.getLogger(EVENT_AUTHENTICATION);

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final ZoneId ZONE = ZoneId.of("UTC");

    /**
     * Log that a outgoing publish message was dropped.
     *
     * @param clientId of the subscriber that would have received the message
     * @param topic    of the publish message
     * @param qos      of the publish message
     * @param reason   why the message was dropped
     */
    public void messageDropped(@Nullable final String clientId, @Nullable final String topic, @NotNull final int qos, @NotNull final String reason) {
        logMessageDropped.debug("Outgoing publish message was dropped. Receiving client: {}, topic: {}, qos: {}, reason: {}.",
                valueOrUnknown(clientId), valueOrUnknown(topic), qos, reason);
    }

    /**
     * Log that a outgoing publish message for a shared subscription was dropped.
     *
     * @param group  of the shared subscription
     * @param topic  of the publish message
     * @param qos    of the publish message
     * @param reason why the message was dropped
     */
    public void sharedSubscriptionMessageDropped(@Nullable final String group, @Nullable final String topic, @NotNull final int qos, @NotNull final String reason) {
        logMessageDropped.debug("Outgoing publish message was dropped. Receiving shared subscription group: {}, topic: {}, qos: {}, reason: {}.",
                valueOrUnknown(group), valueOrUnknown(topic), qos, reason);
    }

    /**
     * Log that a outgoing MQTT message for a client was dropped.
     *
     * @param client      identifier of the client that would have received the message
     * @param messageType MQTT message type
     * @param reason      why the message was dropped
     */
    public void mqttMessageDropped(@Nullable final String client, @Nullable final String messageType, @NotNull final String reason) {
        logMessageDropped.debug("Outgoing MQTT packet was dropped. Receiving client: {}, messageType: {}, reason: {}.",
                valueOrUnknown(client), valueOrUnknown(messageType), reason);
    }

    /**
     * Log that a client has successfully connected to the broker.
     *
     * @param channel of the client connection
     */
    public void clientConnected(@NotNull final Channel channel) {
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final String ip = ChannelUtils.getChannelIP(channel).orNull();
        final Boolean cleanStart = channel.attr(ChannelAttributes.CLEAN_START).get();
        final Long sessionExpiry = channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).get();

        logClientConnected.debug("Client ID: {}, IP: {}, Clean Start: {}, Session Expiry: {} connected.", valueOrUnknown(clientId), valueOrUnknown(ip), valueOrUnknown(cleanStart), valueOrUnknown(sessionExpiry));
    }

    /**
     * Log that the connection to a client was closed, regardless if the connection was closed by the client or the
     * server.
     *
     * @param channel      of the client connection
     * @param reasonString reason specified by the client for the DISCONNECT
     */
    public void clientDisconnected(@NotNull final Channel channel, @Nullable final String reasonString) {
        channel.attr(ChannelAttributes.DISCONNECT_EVENT_LOGGED).set(true);
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final String ip = ChannelUtils.getChannelIP(channel).orNull();
        final boolean graceful = channel.attr(ChannelAttributes.GRACEFUL_DISCONNECT).get() != null;

        if (graceful) {
            log.trace("Client {} disconnected gracefully.", clientId);
            if (reasonString != null) {
                logClientDisconnected.debug("Client ID: {}, IP: {} disconnected gracefully. Reason given by client: {}", valueOrUnknown(clientId), valueOrUnknown(ip), reasonString);
            } else {
                logClientDisconnected.debug("Client ID: {}, IP: {} disconnected gracefully.", valueOrUnknown(clientId), valueOrUnknown(ip));
            }
        } else {
            log.trace("Client {} disconnected ungracefully.", clientId);
            logClientDisconnected.debug("Client ID: {}, IP: {} disconnected ungracefully.", valueOrUnknown(clientId), valueOrUnknown(ip));
        }
    }

    /**
     * Log that the connection to the client was closed by the broker.
     *
     * @param channel of the client connection
     * @param reason  why the connection was closed
     */
    public void clientWasDisconnected(@NotNull final Channel channel, @NotNull final String reason) {
        channel.attr(ChannelAttributes.DISCONNECT_EVENT_LOGGED).set(true);
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final String ip = ChannelUtils.getChannelIP(channel).orNull();
        log.trace("Client {} was disconnected.", clientId);
        logClientDisconnected.debug("Client ID: {}, IP: {} was disconnected. reason: {}.", valueOrUnknown(clientId), valueOrUnknown(ip), reason);
    }

    /**
     * Log that the an AUTH packet was received or sent.
     *
     * @param channel    of the client connection
     * @param reasonCode of the AUTH packet.
     */
    public void clientAuthentication(@NotNull final Channel channel, @NotNull final Mqtt5AuthReasonCode reasonCode, final boolean received) {
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final String ip = ChannelUtils.getChannelIP(channel).orNull();
        if (received) {
            logAuthentication.debug("Received AUTH from Client ID: {}, IP: {}, reason code: {}.", valueOrUnknown(clientId), valueOrUnknown(ip), reasonCode.name());
        } else {
            logAuthentication.debug("Sent AUTH to Client ID: {}, IP: {}, reason code: {}.", valueOrUnknown(clientId), valueOrUnknown(ip), reasonCode.name());
        }
    }

    /**
     * Log that a client session expired and will be deleted.
     *
     * @param expiryTimestamp the {@link Long} timestamp of the client-session-expiration
     * @param clientId        of the expired session
     */
    public void clientSessionExpired(final Long expiryTimestamp, @Nullable final String clientId) {

        final LocalDateTime disconnectedSinceDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiryTimestamp),
                ZONE);
        logClientSessionExpired.debug("Client ID: {} session has expired at {}. All persistent data for this client has been removed.",
                valueOrUnknown(clientId), disconnectedSinceDateTime.format(dateTimeFormatter));
    }

    @NotNull
    private String valueOrUnknown(@Nullable final Object object) {
        return object != null ? object.toString() : "UNKNOWN";
    }
}
