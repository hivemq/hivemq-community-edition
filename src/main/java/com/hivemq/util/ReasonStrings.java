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

/**
 * @author Florian Raschbichler
 */
public class ReasonStrings {

    public static final String CONNACK_MALFORMED_PACKET_FIXED_HEADER = "Sent CONNECT with invalid fixed header.";
    public static final String CONNACK_MALFORMED_PACKET_USER_PROPERTY = "Sent CONNECT with malformed user property.";
    public static final String CONNACK_MALFORMED_PACKET_USERNAME = "Sent CONNECT with malformed username.";
    public static final String CONNACK_MALFORMED_PACKET_PASSWORD = "Sent CONNECT with malformed password.";
    public static final String CONNACK_MALFORMED_BOOLEAN = "Sent CONNECT with a malformed boolean value.";
    public static final String CONNACK_MALFORMED_AUTH_METHOD = "Sent CONNECT with a malformed auth method.";
    public static final String CONNACK_MALFORMED_AUTH_DATA = "Sent CONNECT with malformed auth data.";
    public static final String CONNACK_MALFORMED_WILL_PAYLOAD = "Sent CONNECT with a malformed Will payload.";
    public static final String CONNACK_MALFORMED_WILL_FLAG = "Sent CONNECT with an invalid will-topic/flag combination.";
    public static final String CONNACK_MALFORMED_RESPONSE_TOPIC = "Sent CONNECT with a malformed response topic.";
    public static final String CONNACK_MALFORMED_CONNECT_FLAGS = "Sent CONNECT with invalid connect flags.";
    public static final String CONNACK_MALFORMED_CORRELATION_DATA = "Sent CONNECT with malformed correlation data.";
    public static final String CONNACK_MALFORMED_CONTENT_TYPE = "Sent CONNECT with malformed content type.";
    public static final String CONNACK_MALFORMED_PFI = "Sent CONNECT with an invalid payload format indicator.";
    public static final String CONNACK_MALFORMED_REMAINING = "Sent CONNECT with not enough remaining read buffer length was sent. Property could not be read.";
    public static final String CONNACK_MALFORMED_PROPERTIES_LENGTH = "Sent CONNECT with malformed properties length.";
    public static final String CONNACK_MALFORMED_PROPERTIES_INVALID = "Sent CONNECT with an invalid property identifier.";

    public static final String CONNACK_NOT_AUTHORIZED_MAX_TOPIC_LENGTH_EXCEEDED = "Not authorized to connect. The will topic length exceeded the maximum length configured on the broker.";
    public static final String CONNACK_PROTOCOL_ERROR_INVALID_USER_PASS_COMB_MQTT3 = "Sent CONNECT with invalid username/password combination.";
    public static final String CONNACK_PROTOCOL_ERROR_NO_AUTH = "Sent CONNECT with auth data and no auth method. This is a protocol violation.";
    public static final String CONNACK_PROTOCOL_RECEIVE_MAXIMUM = "Sent CONNECT with receive maximum set to '0'. This is a protocol violation.";
    public static final String CONNACK_PROTOCOL_PACKET_SIZE = "Sent CONNECT with packet size set to '0'. This is a protocol violation.";
    public static final String CONNACK_PROTOCOL_ERROR_VARIABLE_HEADER = "Sent CONNECT with invalid variable header. This is a protocol violation.";
    public static final String CONNACK_PROTOCOL_MULTIPLE_KEY = "Sent CONNECT with %s included more than once. This is a protocol violation.";

    public static final String CONNACK_UNSUPPORTED_PROTOCOL_VERSION = "Sent CONNECT with invalid protocol name.";

    public static final String CONNACK_CLIENT_IDENTIFIER_NOT_VALID = "Sent CONNECT with invalid client identifier.";
    public static final String CONNACK_CLIENT_IDENTIFIER_EMPTY = "Sent CONNECT with empty client identifier.";
    public static final String CONNACK_CLIENT_IDENTIFIER_TOO_LONG = "Sent CONNECT with a client identifier that is too long.";

    public static final String CONNACK_NOT_AUTHORIZED_WILL_WILDCARD = "Not authorized to connect. Will topic contained wildcard characters (#/+). The broker does not allow this.";
    public static final String CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR = "Not authorized to connect. No authenticator registered.";
    public static final String CONNACK_NOT_AUTHORIZED_FAILED = "Not authorized to connect. Authentication failed.";

    public static final String CONNACK_TOPIC_NAME_INVALID_WILL_LENGTH = "Sent CONNECT with incorrect will topic length.";
    public static final String CONNACK_TOPIC_NAME_INVALID_WILL_MALFORMED = "Sent CONNECT with malformed will topic.";
    public static final String CONNACK_TOPIC_NAME_INVALID_WILL_EMPTY = "Sent CONNECT with empty will topic.";
    public static final String CONNACK_TOPIC_NAME_INVALID_WILL_WILDCARD = "Not authorized to connect. Will topic contained wildcard characters (#/+) in the %s. The broker does not allow this.";

    public static final String CONNACK_RETAIN_NOT_SUPPORTED = "Retain flag of Will message was set to true. The broker does not allow this.";
    public static final String CONNACK_PACKET_TOO_LARGE_USER_PROPERTIES = "Sent CONNECT with too large user properties.";
    public static final String CONNACK_MALFORMED_PACKET_INCORRECT_WILL_TOPIC_LENGTH = "Incorrect CONNECT will-topic length.";
    public static final String CONNACK_MALFORMED_PACKET_BAD_UTF8 = "Sent CONNECT with bad UTF-8 character.";
    public static final String CONNACK_MALFORMED_PACKET_INVALID_WILL_TOPIC = "Sent CONNECT with invalid will-topic.";

    public static final String CONNACK_QOS_NOT_SUPPORTED_WILL = "Quality of service level of Will message in CONNECT exceeds maximum allowed QoS. QoS used: %s. Maximum allowed QoS: %s.";
    public static final String CONNACK_QOS_NOT_SUPPORTED_PUBLISH = "Quality of service level of PUBLISH exceeds maximum allowed QoS. QoS used: %s. Maximum allowed QoS: %s.";
    public static final String CONNACK_CONNECT_TIMED_OUT = "Connect timed out.";

    public static final String DISCONNECT_MALFORMED_USER_PROPERTY = "%s containing a malformed user property was sent.";
    public static final String DISCONNECT_MALFORMED_UTF8_LENGTH = "%s with incorrect UTF-8 String length for %s was sent.";
    public static final String DISCONNECT_MALFORMED_AUTH_METHOD = "%s with malformed authentication method was sent.";
    public static final String DISCONNECT_MALFORMED_WILDCARD = "%s with wildcard character (#/+) was sent.";
    public static final String DISCONNECT_MALFORMED_CORRELATION_DATA = "%s with malformed correlation data was sent.";
    public static final String DISCONNECT_MALFORMED_RESPONSE_TOPIC = "%s with malformed response topic was sent.";
    public static final String DISCONNECT_MALFORMED_SERVER_REFERENCE = "%s with a malformed UTF-8 String in server reference was sent.";
    public static final String DISCONNECT_MALFORMED_PFI = "%s with invalid payload format indicator was sent.";
    public static final String DISCONNECT_MALFORMED_PAYLOAD = "%s containing a payload was sent.";
    public static final String DISCONNECT_MALFORMED_CONTENT_TYPE = "%s with malformed UTF-8 String for content type was sent.";
    public static final String DISCONNECT_MALFORMED_REASON_STRING = "%s with malformed UTF-8 String for reason string was sent.";
    public static final String DISCONNECT_MALFORMED_REMAINING_LENGTH = "%s with not enough remaining read buffer length was sent.";
    public static final String DISCONNECT_MALFORMED_PROPERTIES_LENGTH = "%s with malformed properties length was sent.";
    public static final String DISCONNECT_MALFORMED_PROPERTY_IDENTIFIER = "%s with invalid property identifier was sent.";
    public static final String DISCONNECT_MALFORMED_PUBLISH_QOS_3 = "PUBLISH with quality of service set to '3' was sent.";
    public static final String DISCONNECT_MALFORMED_FIXED_HEADER = "%s with incorrect fixed header was sent.";
    public static final String DISCONNECT_MALFORMED_AUTH_DATA = "AUTH with malformed authentication data was sent.";
    public static final String DISCONNECT_MALFORMED_SUBSCRIPTION_OPTIONS = "SUBSCRIBE with malformed subscription options was sent.";
    public static final String DISCONNECT_MALFORMED_UTF8_STRING = "%s with malformed UTF-8 String for %s was sent.";
    public static final String DISCONNECT_MALFORMED_EMPTY_UNSUB_TOPIC = "Sent UNSUBSCRIBE with an empty topic.";
    public static final String DISCONNECT_SUBSCRIBE_TOPIC_FILTER_INVALID = "Sent SUBSCRIBE with an invalid topic filter.";

    public static final String DISCONNECT_PROTOCOL_ERROR_MESSAGE_ID = "%s without message id was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD = "%s with invalid authentication method was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_FIXED_HEADER = "Sent DISCONNECT with invalid fixed header.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SESSION_EXPIRY = "Sent DISCONNECT with invalid session expiry interval. Session expiry was set to zero on CONNECT and DISCONNECT contained a different value. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_REASON_CODE = "%s with invalid reason code was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_ID_ZERO = "%s with a packet identifier of '0' was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_ID_ZERO = "SUBSCRIBE with a packet identifier of '0' was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBLISH_ID_ZERO = "PUBLISH with quality of service level greater than zero and packet identifier of '0' was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_MULTI_KEY = "%s with %s included more than once was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBLISH_QOS_0_DP = "PUBLISH with quality of service set to '0' and DUP flag set to true was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBLISH_SUBSCRIPTION_IDENTIFIER = "PUBLISH containing subscription identifiers was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBACK_HEADER = "PUBACK with incorrect fixed header was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBREC_HEADER = "PUBREC with incorrect fixed header was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBREL_HEADER = "PUBREL with incorrect fixed header was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_PUBCOMP_HEADER = "PUBCOMP with incorrect fixed header was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_NO_SUBSCRIPTIONS = "SUBSCRIBE with zero subscriptions was sent.";
    public static final String DISCONNECT_PROTOCOL_ERROR_NO_SUBSCRIPTION_OPTIONS = "SUBSCRIBE with no subscription options was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SHARED_SUBSCRIPTION_NO_LOCAL = "SUBSCRIBE with a shared subscription and the no local flag set to true was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SUBSCRIPTION_IDENTIFIER_ZERO = "SUBSCRIBE with a subscription identifier of '0' was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_NO_QOS = "SUBSCRIBE without a quality of service level was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_QOS_3 = "SUBSCRIBE with a quality of service level set to '3' was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_RETAIN_HANDLING_3 = "SUBSCRIBE with retain handling set to '3' was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_UNSUBSCRIBE_NO_TOPIC_FILTERS = "UNSUBSCRIBE containing no topic filters was sent. This is a protocol violation.";
    public static final String DISCONNECT_PROTOCOL_ERROR_UNSUBSCRIBE_PACKET_ID_0 = "UNSUBSCRIBE with a packet identifier of '0' was sent. This is a protocol violation.";

    public static final String DISCONNECT_RECEIVE_MAXIMUM_EXCEEDED = "Too many concurrent PUBLISH messages sent.";

    public static final String DISCONNECT_TOPIC_NAME_INVALID_SHARED_EMPTY = "Shared subscription with empty topic.";
    public static final String DISCONNECT_TOPIC_ALIAS_INVALID_ZERO = "PUBLISH containing topic alias of '0' was sent.";
    public static final String DISCONNECT_TOPIC_ALIAS_INVALID_TOO_LARGE = "Topic alias in PUBLISH sent was too large.";
    public static final String DISCONNECT_TOPIC_ALIAS_INVALID_UNMAPPED = "Topic alias in PUBLISH could not be mapped.";
    public static final String DISCONNECT_TOPIC_ALIAS_INVALID_HARD_LIMIT = "Topic alias in PUBLISH exceeds the global memory hard limit.";
    public static final String DISCONNECT_TOPIC_ALIAS_INVALID_ABSENT = "PUBLISH missing both topic name and topic alias was sent.";

    public static final String DISCONNECT_MESSAGE_TYPE_ZERO = "Message type '0' is not allowed.";
    public static final String DISCONNECT_CONNACK_RECEIVED = "CONNACK message type is not allowed.";
    public static final String DISCONNECT_SUBACK_RECEIVED = "SUBACK message type is not allowed.";
    public static final String DISCONNECT_UNSUBACK_RECEIVED = "UNSUBACK message type is not allowed.";
    public static final String DISCONNECT_PINGRESP_RECEIVED = "PINGRESP message type is not allowed.";
    public static final String DISCONNECT_MESSAGE_TYPE_FIFTEEN = "Message type '15' is not allowed.";
    public static final String DISCONNECT_MESSAGE_TYPE_INVALID = "Message type invalid.";
    public static final String DISCONNECT_PACKET_TOO_LARGE_MESSAGE = "Size of the message sent was too large.";
    public static final String DISCONNECT_PACKET_TOO_LARGE_USER_PROPERTIES = "Sent %s with too large user properties.";
    public static final String DISCONNECT_MAXIMUM_TOPIC_LENGTH_EXCEEDED = "Maximum topic length configured at the broker exceeded.";

    public static final String DISCONNECT_PAYLOAD_FORMAT_INVALID_PUBLISH = "PUBLISH with no valid UTF-8 payload was sent.";

    public static final String DISCONNECT_RETAIN_NOT_SUPPORTED = "PUBLISH with retain flag set to true was sent. The broker does not allow this.";

    public static final String DISCONNECT_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED = "Disconnecting client. SUBSCRIBE containing subscription identifiers was sent. The broker does not allow this.";
    public static final String DISCONNECT_WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED = "Disconnecting client. SUBSCRIBE containing wildcard characters (#/+) was sent. The broker does not allow this.";
    public static final String DISCONNECT_SHARED_SUBSCRIPTIONS_NOT_SUPPORTED = "Disconnecting client. SUBSCRIBE containing shared subscriptions was sent. The broker does not allow this.";

    public static final String DISCONNECT_SESSION_TAKEN_OVER = "Another client connected with the same client id.";

    public static final String DISCONNECT_KEEP_ALIVE_TIMEOUT = "The client was idle for too long without sending an MQTT control packet.";

    public static final String SUBACK_EXTENSION_PREVENTED = "SUBSCRIBE prevented by an extension.";
    public static final String UNSUBACK_EXTENSION_PREVENTED = "UNSUBSCRIBE prevented by an extension.";

    public static final String AUTH_FAILED = "Authentication failed";
    public static final String RE_AUTH_FAILED = "Re-authentication failed";
    public static final String AUTH_FAILED_NO_AUTHENTICATOR = "Authentication failed, no authenticator registered";
    public static final String RE_AUTH_FAILED_NO_AUTHENTICATOR = "Re-authentication failed, no authenticator registered";
    public static final String AUTH_FAILED_CLIENT_TIMEOUT = "Authentication failed, timeout before the client provided required authentication data";
    public static final String RE_AUTH_FAILED_CLIENT_TIMEOUT = "Re-authentication failed, timeout before the client provided required authentication data";
    public static final String AUTH_FAILED_EXTENSION_TIMEOUT = "Authentication failed, authenticator timed out";
    public static final String RE_AUTH_FAILED_EXTENSION_TIMEOUT = "Re-authentication failed, authenticator timed out";
    public static final String AUTH_FAILED_UNDECIDED = "Authentication failed, authenticator did not decide authenticity";
    public static final String RE_AUTH_FAILED_UNDECIDED = "Re-authentication failed, authenticator did not decide authenticity";
    public static final String AUTH_FAILED_EXCEPTION = "Authentication failed, exception in authenticator";
    public static final String RE_AUTH_FAILED_EXCEPTION = "Re-authentication failed, exception in authenticator";
    public static final String AUTH_FAILED_SEND_EXCEPTION = "Authentication failed, could not send AUTH to client";
    public static final String RE_AUTH_FAILED_SEND_EXCEPTION = "Re-authentication failed, could not send AUTH to client";

    public static final String CLOSE_MALFORMED_REMAINING_LENGTH = "Message with a malformed remaining length was sent.";

    public static final String CONNACK_UNSPECIFIED_ERROR_EXTENSION_EXCEPTION = "Connect with client ID %s failed because of an exception was thrown by an CONNECT inbound interceptor.";

    private ReasonStrings() {
        //Don't instantiate
    }

}
