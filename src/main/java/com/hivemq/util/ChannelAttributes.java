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
package com.hivemq.util;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.client.parameter.ConnectionAttributes;
import com.hivemq.extensions.events.client.parameters.ClientEventListeners;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.util.AttributeKey;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dominik Obermaier
 */
public class ChannelAttributes {

    public static final AttributeKey<ProtocolVersion> MQTT_VERSION = AttributeKey.valueOf("MQTT.Version");
    public static final AttributeKey<String> CLIENT_ID = AttributeKey.valueOf("MQTT.ClientId");
    public static final AttributeKey<Integer> CONNECT_KEEP_ALIVE = AttributeKey.valueOf("MQTT.KeepAlive");
    public static final AttributeKey<Boolean> CLEAN_START = AttributeKey.valueOf("MQTT.CleanStart");
    public static final AttributeKey<Boolean> GRACEFUL_DISCONNECT = AttributeKey.valueOf("MQTT.GracefulDisconnect");
    public static final AttributeKey<Boolean> SEND_WILL = AttributeKey.valueOf("MQTT.SendWill");
    public static final AttributeKey<Boolean> CONNACK_SENT = AttributeKey.valueOf("MQTT.ConnackSent");
    public static final AttributeKey<Boolean> TAKEN_OVER = AttributeKey.valueOf("MQTT.TakenOver");
    public static final AttributeKey<Boolean> PREVENT_LWT = AttributeKey.valueOf("MQTT.PreventLWT");

    /**
     * This reveres to the in-flight messages in the client queue, not the ones in the ordered topic queue
     */
    public static final AttributeKey<Boolean> IN_FLIGHT_MESSAGES_SENT = AttributeKey.valueOf("MQTT.inflight-messages.sent");

    /**
     * True if it is guarantied that this client has no shared subscriptions, if null it is unclear.
     * The value is never set to false.
     */
    public static final AttributeKey<Boolean> NO_SHARED_SUBSCRIPTION = AttributeKey.valueOf("MQTT.no-shared-subscriptions");

    /**
     * This attribute is added during connection. The future is set, when the client disconnect handling is complete.
     */
    public static final AttributeKey<SettableFuture<Void>> DISCONNECT_FUTURE = AttributeKey.valueOf("MQTT.DisconnectFuture");


    public static final AttributeKey<SslClientCertificate> AUTH_CERTIFICATE = AttributeKey.valueOf("Auth.Certificate");
    public static final AttributeKey<String> AUTH_CIPHER_SUITE = AttributeKey.valueOf("Auth.Cipher.Suite");
    public static final AttributeKey<String> AUTH_PROTOCOL = AttributeKey.valueOf("Auth.Protocol");
    public static final AttributeKey<String> AUTH_USERNAME = AttributeKey.valueOf("Auth.Username");
    public static final AttributeKey<byte[]> AUTH_PASSWORD = AttributeKey.valueOf("Auth.Password");
    public static final AttributeKey<CONNECT> AUTH_CONNECT = AttributeKey.valueOf("Auth.Connect");
    public static final AttributeKey<String> AUTH_METHOD = AttributeKey.valueOf("Auth.AuthenticationMethod");

    public static final AttributeKey<ByteBuffer> AUTH_DATA = AttributeKey.valueOf("Auth.Data");
    public static final AttributeKey<Mqtt5UserProperties> AUTH_USER_PROPERTIES = AttributeKey.valueOf("Auth.User.Properties");
    public static final AttributeKey<Boolean> AUTH_ONGOING = AttributeKey.valueOf("Auth.Ongoing");
    public static final AttributeKey<Boolean> RE_AUTH_ONGOING = AttributeKey.valueOf("ReAuth.Ongoing");

    public static final AttributeKey<Boolean> AUTH_AUTHENTICATED = AttributeKey.valueOf("Auth.Authenticated");
    public static final AttributeKey<Boolean> AUTHENTICATED_OR_AUTHENTICATION_BYPASSED = AttributeKey.valueOf("AuthenticatedOrAuthenticationBypassed");
    public static final AttributeKey<ScheduledFuture<?>> AUTH_FUTURE = AttributeKey.valueOf("Auth.Future");

    public static final AttributeKey<Long> MAX_PACKET_SIZE_SEND = AttributeKey.valueOf("Restriction.MaxPacketSize.Send");

    public static final AttributeKey<ClientContextImpl> EXTENSION_CLIENT_CONTEXT = AttributeKey.valueOf("Extension.Client.Context");
    public static final AttributeKey<ClientEventListeners> EXTENSION_CLIENT_EVENT_LISTENERS = AttributeKey.valueOf("Extension.Client.Event.Listeners");
    public static final AttributeKey<Boolean> EXTENSION_CONNECT_EVENT_SENT = AttributeKey.valueOf("Extension.Connect.Event.Sent");
    public static final AttributeKey<Boolean> EXTENSION_DISCONNECT_EVENT_SENT = AttributeKey.valueOf("Extension.Disconnect.Event.Sent");
    public static final AttributeKey<ClientAuthenticators> EXTENSION_CLIENT_AUTHENTICATORS = AttributeKey.valueOf("Extension.Client.Authenticators");
    public static final AttributeKey<ClientAuthorizers> EXTENSION_CLIENT_AUTHORIZERS = AttributeKey.valueOf("Extension.Client.Authorizers");
    public static final AttributeKey<ClientInformation> EXTENSION_CLIENT_INFORMATION = AttributeKey.valueOf("Extension.Client.Information");
    public static final AttributeKey<ConnectionInformation> EXTENSION_CONNECTION_INFORMATION = AttributeKey.valueOf("Extension.Connection.Information");


    /**
     * The time at which the clients CONNECT message was received by the broker.
     */
    public static final AttributeKey<Long> CONNECT_RECEIVED_TIMESTAMP = AttributeKey.valueOf("Connect.Received.Timestamp");

    /**
     * This key contains the actual listener a client connected to
     */
    public static final AttributeKey<Listener> LISTENER = AttributeKey.valueOf("Listener");

    /**
     * Attribute for storing connection attributes. It is added only when connection attributes are set.
     */
    public static final AttributeKey<ConnectionAttributes> CONNECTION_ATTRIBUTES = AttributeKey.valueOf("ConnectionAttributes");

    /**
     * Attribute for storing the client session expiry interval.
     */
    public static final AttributeKey<Long> CLIENT_SESSION_EXPIRY_INTERVAL = AttributeKey.valueOf("ClientSession.ExpiryInterval");

    /**
     * The amount of messages that have been polled but not yet delivered
     */
    public static final AttributeKey<AtomicInteger> IN_FLIGHT_MESSAGES = AttributeKey.valueOf("Client.InFlightMessages");


    /* *****************
     *      MQTT 5     *
     *******************/

    public static final AttributeKey<String[]> TOPIC_ALIAS_MAPPING = AttributeKey.valueOf("TopicAlias.Mapping");

    public static final AttributeKey<Boolean> CLIENT_ID_ASSIGNED = AttributeKey.valueOf("Client.Identifier.Assigned");

    public static final AttributeKey<Integer> CLIENT_RECEIVE_MAXIMUM = AttributeKey.valueOf("Client.Receive.Maximum");

    public static final AttributeKey<Long> QUEUE_SIZE_MAXIMUM = AttributeKey.valueOf("Client.QueueSize.Maximum");

    public static final AttributeKey<Boolean> DISCONNECT_EVENT_LOGGED = AttributeKey.valueOf("Disconnect.Event.Logged");

    public static final AttributeKey<Boolean> REQUEST_RESPONSE_INFORMATION = AttributeKey.valueOf("Request.Response.Information");

    public static final AttributeKey<Boolean> REQUEST_PROBLEM_INFORMATION = AttributeKey.valueOf("Request.Problem.Information");

    public static final AttributeKey<ModifiableDefaultPermissions> AUTH_PERMISSIONS = AttributeKey.valueOf("Auth.User.Permissions");

    public static final AttributeKey<CONNECT> CONNECT_MESSAGE = AttributeKey.valueOf("Connect.Message");

    /**
     * True if this client is not allowed to publish any more messages, if <null> he is allowed to do so.
     * The value is never set to false.
     */
    public static final AttributeKey<Boolean> INCOMING_PUBLISHES_SKIP_REST = AttributeKey.valueOf("Incoming.Publishes.Skip.Rest");

    /**
     * True if this client is not allowed to publish any more messages by default, if <null> he is allowed to do so.
     * The value is never set to false.
     */
    public static final AttributeKey<Boolean> INCOMING_PUBLISHES_DEFAULT_FAILED_SKIP_REST = AttributeKey.valueOf("Incoming.Publishes.Default.Failed.Skip.Rest");
}
