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

package util;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.*;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class TestMessageUtil {

    public static final Mqtt5UserProperties TEST_USER_PROPERTIES =
            Mqtt5UserProperties.of(new MqttUserProperty("user1", "property1"),
                    new MqttUserProperty("user2", "property2"));
    public static final List<String> topics = Lists.newArrayList("topic1", "topic2", "topic3");

    public static PUBLISH createMqtt3Publish() {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withTopic("topic")
                .withPayload("payload".getBytes())
                .withPacketIdentifier(1)
                .withHivemqId("hivemqId")
                .build();
    }

    public static PUBLISH createMqtt3Publish(
            final String hivemqId, final String topic, final QoS qoS, final byte[] payload, final boolean retain) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(qoS)
                .withTopic(topic)
                .withPayload(payload)
                .withRetain(retain)
                .withHivemqId(hivemqId)
                .withPacketIdentifier(1)
                .build();
    }

    public static PUBLISH createMqtt3Publish(final String topic, final byte[] payload, final QoS qoS) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(qoS)
                .withTopic(topic)
                .withPayload(payload)
                .withHivemqId("hivemqId")
                .build();
    }

    public static PUBLISH createMqtt3Publish(
            final String hivemqId, final Long payloadId, final PublishPayloadPersistence publishPayloadPersistence) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withTopic("topic")
                .withHivemqId(hivemqId)
                .withPayloadId(payloadId)
                .withPersistence(publishPayloadPersistence)
                .build();
    }

    public static PUBLISH createMqtt3Publish(
            final Long payloadId, final PublishPayloadPersistence publishPayloadPersistence) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withTopic("topic")
                .withPayloadId(payloadId)
                .withPersistence(publishPayloadPersistence)
                .withHivemqId("hivemqId")
                .build();
    }

    public static PUBLISH createMqtt3Publish(final String hivemqId, final PUBLISH publish) {
        return new PUBLISHFactory.Mqtt3Builder()
                .fromPublish(publish)
                .withHivemqId(hivemqId)
                .build();
    }

    public static PUBLISH createMqtt3Publish(
            final String hivemqId, final PUBLISH publish, final long timestamp, final long payloadId,
            final PublishPayloadPersistence persistence) {
        return new PUBLISHFactory.Mqtt3Builder()
                .fromPublish(publish)
                .withHivemqId(hivemqId)
                .withTimestamp(timestamp)
                .withPayloadId(payloadId)
                .withPersistence(persistence)
                .build();
    }

    public static PUBLISH createMqtt3Publish(final String hivemqId, final long publishid) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withTopic("topic")
                .withPacketIdentifier(1)
                .withPublishId(publishid)
                .withHivemqId(hivemqId)
                .withPayload(new byte[]{})
                .build();
    }

    public static PUBLISH createMqtt3Publish(final long timestamp) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withTopic("topic")
                .withTimestamp(timestamp)
                .withHivemqId("hivemqId")
                .withPayload(new byte[]{})
                .build();
    }

    public static PUBLISH createMqtt3Publish(final QoS qoS) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(qoS)
                .withTopic("topic")
                .withPayload("payload".getBytes())
                .withHivemqId("hivemqId")
                .withPacketIdentifier(1)
                .withPublishId(1L)
                .build();

    }

    public static PUBLISH createMqtt5Publish(final String topic) {
        return createMqtt5Publish(topic, QoS.AT_LEAST_ONCE);
    }

    public static PUBLISH createMqtt5Publish(final String topic, final QoS qos) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withQoS(qos)
                .withTopic(topic)
                .withPayload("payload".getBytes())
                .withPacketIdentifier(1)
                .withHivemqId("hivemqId")
                .build();
    }

    public static PUBLISHFactory.Mqtt5Builder getDefaultPublishBuilder(
            final @NotNull PublishPayloadPersistence publishPayloadPersistence) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withTopic("topic")
                .withPayload("payload".getBytes())
                .withPacketIdentifier(1)
                .withPayloadId(1L)
                .withPersistence(publishPayloadPersistence)
                .withHivemqId("hivemqId");
    }

    public static PUBLISH createMqtt5Publish(final int packetId) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withTopic("topic")
                .withPayload("payload".getBytes())
                .withPacketIdentifier(packetId)
                .withHivemqId("hivemqId")
                .build();
    }

    public static PUBLISH createMqtt5Publish() {
        return createMqtt5Publish("topic");
    }

    public static PUBLISH createMqtt5Publish(
            @NotNull final String hivemqId,
            @NotNull final String topic,
            @NotNull final byte[] payload,
            @NotNull final QoS qos,
            final boolean isRetain,
            final long messageExpiryInterval,
            @Nullable final Mqtt5PayloadFormatIndicator payloadFormatIndicator,
            @Nullable final String contentType,
            @Nullable final String responseTopic,
            @Nullable final byte[] correlationData,
            @NotNull final Mqtt5UserProperties userProperties,
            final int packetIdentifier,
            final boolean isDup,
            final boolean isNewTopicAlias,
            @Nullable final ImmutableList<Integer> subscriptionIdentifiers) {

        return new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId(hivemqId)
                .withTopic(topic)
                .withPayload(payload)
                .withQoS(qos)
                .withRetain(isRetain)
                .withMessageExpiryInterval(messageExpiryInterval)
                .withPayloadFormatIndicator(payloadFormatIndicator)
                .withContentType(contentType)
                .withResponseTopic(responseTopic)
                .withCorrelationData(correlationData)
                .withUserProperties(userProperties)
                .withPacketIdentifier(packetIdentifier)
                .withDuplicateDelivery(isDup)
                .withNewTopicAlias(isNewTopicAlias)
                .withSubscriptionIdentifiers(subscriptionIdentifiers)
                .build();
    }

    public static PUBLISH createFullMqtt5Publish() {

        return createMqtt5Publish("hivemqId", "topic", "payload".getBytes(), QoS.EXACTLY_ONCE,
                true, 360, Mqtt5PayloadFormatIndicator.UTF_8, "content type",
                "response topic", "correlation data".getBytes(), TEST_USER_PROPERTIES,
                1, true, true, ImmutableList.of(1, 2, 3));
    }

    public static CONNECT createFullMqtt5Connect() {

        return new CONNECT.Mqtt5Builder()
                .withMqtt5UserProperties(TEST_USER_PROPERTIES)
                .withClientIdentifier("clientid")
                .withKeepAlive(60)
                .withCleanStart(true)
                .withSessionExpiryInterval(360)
                .withResponseInformationRequested(true)
                .withProblemInformationRequested(true)
                .withReceiveMaximum(100)
                .withTopicAliasMaximum(10)
                .withMaximumPacketSize(200)
                .withUsername("username")
                .withPassword("password".getBytes())
                .withAuthMethod("auth method")
                .withAuthData("auth data".getBytes())
                .withWill(true)
                .withWillPublish(
                        new MqttWillPublish.Mqtt5Builder()
                                .withHivemqId("hivemqId1")
                                .withTopic("topic")
                                .withPayload("payload".getBytes())
                                .withQos(QoS.EXACTLY_ONCE)
                                .withRetain(true)
                                .withMessageExpiryInterval(360)
                                .withPayloadFormatIndicator(Mqtt5PayloadFormatIndicator.UTF_8)
                                .withContentType("content type")
                                .withResponseTopic("response topic")
                                .withCorrelationData("correlation data".getBytes())
                                .withUserProperties(TEST_USER_PROPERTIES)
                                .withDelayInterval(60)
                                .build()
                )
                .withPasswordRequired(true)
                .withUsernameRequired(true)
                .build();

    }

    public static CONNACK createFullMqtt5Connack() {
        return new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReasonString("success")
                .withUserProperties(TEST_USER_PROPERTIES)
                .withSessionPresent(true)
                .withSessionExpiryInterval(720)
                .withServerKeepAlive(120)
                .withAssignedClientIdentifier("assigned")
                .withAuthMethod("auth method")
                .withAuthData("auth data".getBytes())
                .withReceiveMaximum(100)
                .withTopicAliasMaximum(5)
                .withMaximumPacketSize(100)
                .withMaximumQoS(QoS.AT_LEAST_ONCE)
                .withRetainAvailable(true)
                .withWildcardSubscriptionAvailable(true)
                .withSubscriptionIdentifierAvailable(true)
                .withSharedSubscriptionAvailable(true)
                .withResponseInformation("response")
                .withServerReference("server")
                .build();
    }

    public static SUBSCRIBE createFullMqtt5Subscribe() {
        final ImmutableList.Builder<Topic> topicBuilder = new ImmutableList.Builder<>();
        topicBuilder.add(new Topic(topics.get(0), QoS.AT_MOST_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1));
        topicBuilder.add(new Topic(topics.get(1), QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1));
        topicBuilder.add(new Topic(topics.get(2), QoS.EXACTLY_ONCE, true, false,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 1));
        return createFullMqtt5Subscribe(topicBuilder.build());
    }

    public static SUBSCRIBE createFullMqtt5Subscribe(final ImmutableList<Topic> topics) {

        return new SUBSCRIBE(
                TEST_USER_PROPERTIES,
                topics,
                1,
                1);
    }

    public static SUBACK createFullMqtt5Suback() {

        final List<Mqtt5SubAckReasonCode> reasonCodes = Lists.newArrayList(
                Mqtt5SubAckReasonCode.GRANTED_QOS_0,
                Mqtt5SubAckReasonCode.GRANTED_QOS_1,
                Mqtt5SubAckReasonCode.GRANTED_QOS_2,
                Mqtt5SubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED,
                Mqtt5SubAckReasonCode.TOPIC_FILTER_INVALID,
                Mqtt5SubAckReasonCode.PACKET_IDENTIFIER_IN_USE,
                Mqtt5SubAckReasonCode.QUOTA_EXCEEDED,
                Mqtt5SubAckReasonCode.SHARED_SUBSCRIPTION_NOT_SUPPORTED,
                Mqtt5SubAckReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
                Mqtt5SubAckReasonCode.WILDCARD_SUBSCRIPTION_NOT_SUPPORTED);

        return new SUBACK(1, reasonCodes, "reason", TEST_USER_PROPERTIES);

    }

    public static UNSUBSCRIBE createFullMqtt5Unsubscribe() {

        return new UNSUBSCRIBE(ImmutableList.copyOf(topics), 1, TEST_USER_PROPERTIES);

    }

    public static UNSUBACK createFullMqtt5Unsuback() {

        final List<Mqtt5UnsubAckReasonCode> reasonCodes = Lists.newArrayList(
                Mqtt5UnsubAckReasonCode.SUCCESS,
                Mqtt5UnsubAckReasonCode.NO_SUBSCRIPTIONS_EXISTED,
                Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR,
                Mqtt5UnsubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
                Mqtt5UnsubAckReasonCode.NOT_AUTHORIZED,
                Mqtt5UnsubAckReasonCode.TOPIC_FILTER_INVALID,
                Mqtt5UnsubAckReasonCode.PACKET_IDENTIFIER_IN_USE);

        return new UNSUBACK(1, reasonCodes, "reason", TEST_USER_PROPERTIES);

    }

    public static DISCONNECT createFullMqtt5Disconnect() {
        return new DISCONNECT(
                Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, "reason", TEST_USER_PROPERTIES, "server reference",
                360);
    }

    public static AUTH createFullMqtt5Auth() {
        return new AUTH(
                "auth method", "auth data".getBytes(), Mqtt5AuthReasonCode.SUCCESS, TEST_USER_PROPERTIES, "reason");
    }

    @NotNull
    public static CONNECT createMqtt5ConnectWithWill() {

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder()
                .withTopic("topic")
                .withQos(QoS.EXACTLY_ONCE)
                .withPayload(new byte[]{1, 2, 3})
                .build();

        return new CONNECT.Mqtt5Builder()
                .withWillPublish(willPublish)
                .withClientIdentifier("client")
                .withCleanStart(false)
                .withSessionExpiryInterval(100)
                .build();
    }

    public static PUBACK createSuccessMqtt5Puback() {
        return new PUBACK(1, Mqtt5PubAckReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static PUBREC createSuccessPubrec() {
        return new PUBREC(1);
    }

    public static PUBREL createSuccessPubrel() {
        return new PUBREL(1);
    }

    public static PUBCOMP createFullMqtt5Pubcomp() {
        return new PUBCOMP(1, Mqtt5PubCompReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static PUBCOMP createSuccessPupcomp() {
        return new PUBCOMP(1);
    }
}
