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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.util.AttributeKey;

/**
 * @author Dominik Obermaier
 */
public class ChannelAttributes {

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


    /**
     * This contains the SNI hostname sent by the client if TLS SNI is used
     */
    public static final AttributeKey<String> AUTH_SNI_HOSTNAME = AttributeKey.valueOf("Auth.Sni.Hostname");
    public static final AttributeKey<SslClientCertificate> AUTH_CERTIFICATE = AttributeKey.valueOf("Auth.Certificate");
    public static final AttributeKey<String> AUTH_CIPHER_SUITE = AttributeKey.valueOf("Auth.Cipher.Suite");
    public static final AttributeKey<String> AUTH_PROTOCOL = AttributeKey.valueOf("Auth.Protocol");
    public static final AttributeKey<String> AUTH_USERNAME = AttributeKey.valueOf("Auth.Username");
    public static final AttributeKey<byte[]> AUTH_PASSWORD = AttributeKey.valueOf("Auth.Password");
    public static final AttributeKey<CONNECT> AUTH_CONNECT = AttributeKey.valueOf("Auth.Connect");

    /**
     * Representation of information regarding the connection of a single client.
     */
    public static final AttributeKey<ClientConnection> CLIENT_CONNECTION = AttributeKey.valueOf("Client.Connection");
}
