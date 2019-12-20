/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.bootstrap.netty;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @author Lukas Brandl
 */
public class ChannelHandlerNames {


    /* *************
     *   Ingoing   *
     ***************/

    public static final String ALL_CHANNELS_GROUP_HANDLER = "all_channel_group_handler";

    public static final String STATISTICS_INITIALIZER = "statistics_initializer";
    public static final String MQTT_MESSAGE_DECODER = "mqtt_message_decoder";

    public static final String GLOBAL_THROTTLING_HANDLER = "global_throttling_handler";

    public static final String NEW_CONNECTION_IDLE_HANDLER = "new_connection_idle_handler";
    public static final String NO_CONNECT_IDLE_EVENT_HANDLER = "no_connect_idle_event_handler";
    public static final String NO_TLS_HANDSHAKE_IDLE_EVENT_HANDLER = "no_tls_handshake_idle_event_handler";

    public static final String MQTT_5_FLOW_CONTROL_HANDLER = "mqtt_5_flow_control_handler";

    public static final String MQTT_CONNECT_HANDLER = "mqtt_connect_handler";
    public static final String MQTT_CONNECT_PERSISTENCE_HANDLER = "mqtt_connect_persistence_handler";

    public static final String MQTT_DISCONNECT_HANDLER = "mqtt_disconnect_handler";
    public static final String MQTT_SUBSCRIBE_HANDLER = "mqtt_subscribe_handler";
    public static final String MQTT_UNSUBSCRIBE_HANDLER = "mqtt_unsubscribe_handler";
    public static final String MQTT_PINGREQ_HANDLER = "mqtt_pingreq_handler";

    public static final String MQTT_QOS_RECEIVER_HANDLER = "mqtt_qos_receiver_handler";
    public static final String MQTT_QOS_SENDER_HANDLER = "mqtt_qos_sender_handler";

    public static final String MQTT_DISALLOW_SECOND_CONNECT = "mqtt_disallow_second_connect";

    public static final String MQTT_KEEPALIVE_IDLE_NOTIFIER_HANDLER = "mqtt_keepalive_idle_notifier_handler";
    public static final String MQTT_KEEPALIVE_IDLE_HANDLER = "mqtt_keepalive_idle_handler";

    public static final String MQTT_PUBLISH_USER_EVENT_HANDLER = "mqtt_publish_user_event_handler";
    public static final String INCOMING_PUBLISH_HANDLER = "incoming_publish_handler";
    public static final String INCOMING_SUBSCRIBE_HANDLER = "incoming_subscribe_handler";

    public static final String MQTT_ORDERED_TOPIC_HANDLER = "mqtt_ordered_topic_handler";
    public static final String MQTT_MESSAGE_ID_RETURN_HANDLER = "mqtt_message_id_return_handler";

    public static final String HTTP_SERVER_CODEC = "http_server_codec";
    public static final String HTTP_OBJECT_AGGREGATOR = "http_object_aggregator";
    public static final String WEBSOCKET_SERVER_PROTOCOL_HANDLER = "websocket_server_protocol_handler";
    public static final String WEBSOCKET_BINARY_FRAME_HANDLER = "websocket_binary_frame_handler";
    public static final String WEBSOCKET_CONTINUATION_FRAME_HANDLER = "websocket_continuation_frame_handler";
    public static final String WEBSOCKET_TEXT_FRAME_HANDLER = "websocket_text_frame_handler";

    public static final String SSL_EXCEPTION_HANDLER = "ssl_exception_handler";
    public static final String SSL_HANDLER = "ssl_handler";
    public static final String NON_SSL_HANDLER = "no_ssl_handler";
    public static final String SSL_CLIENT_CERTIFICATE_HANDLER = "ssl_client_certificate_handler";
    public static final String SSL_PARAMETER_HANDLER = "ssl_parameter_handler";

    public static final String LISTENER_ATTRIBUTE_ADDER = "listener_attribute_adder";

    public static final String MQTT_AUTH_HANDLER = "mqtt_auth_handler";
    public static final String AUTH_IN_PROGRESS_MESSAGE_HANDLER = "auth_in_progress_message_handler";


    /* *************
     *   Outgoing  *
     ***************/

    public static final String MQTT_MESSAGE_ENCODER = "mqtt_message_encoder";
    public static final String MQTT_WEBSOCKET_ENCODER = "mqtt_websocket_encoder";
    public static final String DROP_OUTGOING_PUBLISHES_HANDLER = "drop_outgoing_publishes_handler";
    public static final String CHANNEL_INACTIVE_HANDLER = "channel_inactive_handler";
    public static final String MESSAGE_EXPIRY_HANDLER = "message_expiry_handler";

    /* *************
     *     Both    *
     ***************/

    public static final String GLOBAL_MQTT_MESSAGE_COUNTER = "global_mqtt_message_counter";
    public static final String GLOBAL_TRAFFIC_COUNTER = "global_traffic_counter";
    public static final String MQTT_CONNECTION_COUNTER_HANDLER = "mqtt_connection_counter_handler";
    public static final String MQTT_MESSAGE_BARRIER = "mqtt_message_barrier";
    public static final String MQTT_SUBSCRIBE_MESSAGE_BARRIER = "mqtt_subscribe_message_barrier";

    /* *************
     *   Extensions   *
     ***************/

    public static final String PLUGIN_INITIALIZER_HANDLER = "plugin_initializer_handler";
    public static final String CLIENT_LIFECYCLE_EVENT_HANDLER = "client_lifecycle_event_handler";
    public static final String PUBLISH_OUTBOUND_INTERCEPTOR_HANDLER = "publish_outbound_interceptor_handler";
    public static final String PUBACK_INTERCEPTOR_HANDLER = "puback_interceptor_handler";
    public static final String PUBREC_INTERCEPTOR_HANDLER = "pubrec_interceptor_handler";
    public static final String PUBREL_INTERCEPTOR_HANDLER = "pubrel_interceptor_handler";
    public static final String PUBCOMP_INTERCEPTOR_HANDLER = "pubcomp_interceptor_handler";
    public static final String CONNECT_INBOUND_INTERCEPTOR_HANDLER = "connect_inbound_interceptor_handler";
    public static final String CONNACK_OUTBOUND_INTERCEPTOR_HANDLER = "connack_outbound_interceptor_handler";
    public static final String DISCONNECT_INTERCEPTOR_HANDLER = "disconnect_interceptor_handler";
    public static final String SUBACK_OUTBOUND_INTERCEPTOR_HANDLER = "suback_outbound_interceptor_handler";
    public static final String UNSUBSCRIBE_INBOUND_INTERCEPTOR_HANDLER = "inbound_unsubscribe_interceptor_handler";
    public static final String UNSUBACK_OUTBOUND_INTERCEPTOR_HANDLER = "unsuback_outbound_interceptor_handler";
    public static final String PING_INTERCEPTOR_HANDLER = "ping_interceptor_handler";

    /* *************
     *     Misc    *
     ***************/

    public static final String EXCEPTION_HANDLER = "exception_handler";
}



