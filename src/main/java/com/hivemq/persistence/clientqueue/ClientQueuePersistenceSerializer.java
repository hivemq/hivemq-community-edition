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
package com.hivemq.persistence.clientqueue;

import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.PropertiesSerializationUtil;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.local.xodus.XodusUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.Bytes;
import jetbrains.exodus.ByteIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl.Key;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
public class ClientQueuePersistenceSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientQueuePersistenceSerializer.class);

    static final int NO_PACKET_ID = 0;
    static final int CLIENT_ID_MATCH = 0;
    static final int CLIENT_ID_SAME_PREFIX = 1;
    static final int CLIENT_ID_NO_MATCH = 2;

    private static final byte PUBLISH_BIT = (byte) 0b1000_0000;
    private static final byte PUBREL_BIT = (byte) 0b0100_0000;
    private static final byte RETAINED_BIT = (byte) 0b0010_0000;
    private static final byte DUPLICATE_DELIVERY_BIT = (byte) 0b0001_0000;
    //This bit is only set if the messages was sent as a result of a subscribe messages, regardless of the retain as published option.
    private static final byte RETAINED_MESSAGE_BIT = (byte) 0b0000_0100;
    private static final byte QOS_BITS = (byte) 0b0000_0011;

    private static final byte RESPONSE_TOPIC_PRESENT_BIT = (byte) 0b1000_0000;
    private static final byte CONTENT_TYPE_PRESENT_BIT = (byte) 0b0100_0000;
    private static final byte CORRELATION_DATA_PRESENT_BIT = (byte) 0b0010_0000;
    private static final byte SUBSCRIPTION_IDENTIFIERS_PRESENT_BIT = (byte) 0b0001_0000;
    private static final byte USER_PROPERTIES_PRESENT_BIT = (byte) 0b0000_1000;

    // The messages must preserve the order in which they are added to the persistence
    // ID's < Long.MAX_VALUE / 2 are reserved for messages that should be polled with priority
    public static final AtomicLong NEXT_PUBLISH_NUMBER = new AtomicLong(Long.MAX_VALUE / 2);

    @NotNull
    private final PublishPayloadPersistence payloadPersistence;

    ClientQueuePersistenceSerializer(@NotNull final PublishPayloadPersistence payloadPersistence) {
        this.payloadPersistence = payloadPersistence;
    }

    // ********** Key **********

    /**
     * Serializes the client id and adds a entry number to represent the message order.
     *
     * @param key to be serialized
     * @return the serialized key for storing a new PUBLISH
     */
    @NotNull
    ByteIterable serializeNewPublishKey(@NotNull final Key key) {
        return serializeKey(key, NEXT_PUBLISH_NUMBER.getAndIncrement());
    }

    /**
     * Serializes the client id and adds a entry number to represent the message order.
     *
     * @param key to be serialized
     * @return the serialized key for storing an unknown PUBREL
     */
    @NotNull
    ByteIterable serializeUnknownPubRelKey(@NotNull final Key key) {
        // Ensure unknown PUBRELs are always first
        final long messageNumber = NEXT_PUBLISH_NUMBER.getAndIncrement() - Long.MAX_VALUE / 2;
        return serializeKey(key, messageNumber);
    }

    @NotNull ByteIterable serializeKey(@NotNull final Key key, final long number) {
        final byte[] clientBytes = key.getQueueId().getBytes(UTF_8);
        final byte[] result = new byte[clientBytes.length + 1 + Long.BYTES];

        System.arraycopy(clientBytes, 0, result, 0, clientBytes.length);
        result[clientBytes.length] = (byte) (key.isShared() ? 1 : 0);
        Bytes.copyLongToByteArray(number, result, clientBytes.length + 1);

        return XodusUtils.bytesToByteIterable(result);
    }

    /**
     * Serializes the client id for searching.
     *
     * @param key to be serialized
     * @return the serialized client id for searching
     */
    @NotNull
    ByteIterable serializeKey(@NotNull final Key key) {
        final byte[] clientBytes = key.getQueueId().getBytes(UTF_8);
        final byte[] result = new byte[clientBytes.length + 1];

        System.arraycopy(clientBytes, 0, result, 0, clientBytes.length);
        result[clientBytes.length] = (byte) (key.isShared() ? 1 : 0);

        return XodusUtils.bytesToByteIterable(result);
    }

    int compareClientId(@NotNull final ByteIterable serializedClientId, @NotNull final ByteIterable serializedKey) {
        final int clientLength = serializedClientId.getLength();
        if (serializedClientId.compareTo(serializedKey.subIterable(0, clientLength)) != 0) {
            return CLIENT_ID_NO_MATCH;
        }
        if (clientLength == (serializedKey.getLength() - Long.BYTES)) {
            return CLIENT_ID_MATCH;
        }
        return CLIENT_ID_SAME_PREFIX;
    }

    @NotNull
    Key deserializeKeyId(@NotNull final ByteIterable serializedKey) {
        final byte[] bytes = serializedKey.getBytesUnsafe();
        final int clientIdLength = serializedKey.getLength() - 1 - Long.BYTES;
        final String client = new String(bytes, 0, clientIdLength, UTF_8);
        final boolean shared = bytes[clientIdLength] == 1;
        return new Key(client, shared);
    }

    long deserializeIndex(@NotNull final ByteIterable serializedKey) {
        final byte[] keyBytes = XodusUtils.byteIterableToBytes(serializedKey);
        final int indexIndex = serializedKey.getLength() - Long.BYTES;
        return Bytes.readLong(keyBytes, indexIndex);
    }

    // ********** Value **********

    @NotNull
    ByteIterable serializePublishWithoutPacketId(@NotNull final PUBLISH publish, final boolean retained) {
        return XodusUtils.bytesToByteIterable(createPublishBytes(publish, retained));
    }

    @NotNull
    ByteIterable serializeAndSetPacketId(@NotNull final ByteIterable serializedValue, final int packetId) {
        final byte[] bytes = XodusUtils.byteIterableToBytes(serializedValue);
        Bytes.copyUnsignedShortToByteArray(packetId, bytes, 0);
        return XodusUtils.bytesToByteIterable(bytes);
    }

    @NotNull
    ByteIterable serializePubRel(@NotNull final PUBREL pubrel, final boolean retained) {
        return XodusUtils.bytesToByteIterable(createPubrelBytes(pubrel.getPacketIdentifier(), retained, pubrel.getExpiryInterval(),
                pubrel.getPublishTimestamp()));
    }

    int deserializePacketId(@NotNull final ByteIterable serializedValue) {
        return Bytes.readUnsignedShort(serializedValue.getBytesUnsafe(), 0);
    }

    @NotNull
    MessageWithID deserializeValue(@NotNull final ByteIterable serializedValue) {
        final byte[] bytes = serializedValue.getBytesUnsafe();

        if ((bytes[Short.BYTES] & PUBREL_BIT) == PUBREL_BIT) {
            final int packetId = Bytes.readUnsignedShort(bytes, 0);
            final PUBREL pubrel = new PUBREL(packetId);
            if (serializedValue.getLength() >= Short.BYTES + 1 + Long.BYTES * 2) {
                final long expiry = Bytes.readLong(bytes, Short.BYTES + 1);
                pubrel.setExpiryInterval(expiry);
                final long timestamp = Bytes.readLong(bytes, Short.BYTES + 1 + Long.BYTES);
                pubrel.setPublishTimestamp(timestamp);
            }
            return pubrel;
        }
        if ((bytes[Short.BYTES] & PUBLISH_BIT) == PUBLISH_BIT) {
            return deserializePublish(bytes);
        }
        LOGGER.error("Could not deserialize client queue persistence value");
        throw new IllegalArgumentException("Invalid client queue persistence value to deserialize");
    }

    boolean deserializeRetained(@NotNull final ByteIterable serializedValue) {
        final byte[] bytes = serializedValue.getBytesUnsafe();
        return (bytes[Short.BYTES] & RETAINED_MESSAGE_BIT) == RETAINED_MESSAGE_BIT;
    }

    @NotNull
    private byte[] createPubrelBytes(final int packetId, final boolean retained, @Nullable final Long expiry, @Nullable final Long publishTimestamp) {
        final byte[] result;
        if (expiry != null && publishTimestamp != null) {
            result = new byte[Short.BYTES + 1 + Long.BYTES * 2];
        } else {
            result = new byte[Short.BYTES + 1];
        }
        int cursor = 0;
        cursor = XodusUtils.serializeShort(packetId, result, cursor);
        result[cursor] = PUBREL_BIT;
        if (retained) {
            result[cursor] |= RETAINED_MESSAGE_BIT;
        }
        cursor++;
        if (expiry != null && publishTimestamp != null) {
            cursor = XodusUtils.serializeLong(expiry, result, cursor);
            cursor = XodusUtils.serializeLong(publishTimestamp, result, cursor);
        }
        return result;
    }

    @NotNull
    private byte[] createPublishBytes(@NotNull final PUBLISH message, final boolean retained) {

        final byte[] topic = message.getTopic().getBytes(UTF_8);
        final byte[] hivemqId = message.getHivemqId().getBytes(UTF_8);
        final byte[] responseTopic = message.getResponseTopic() == null ? null : message.getResponseTopic().getBytes(UTF_8);
        final byte[] contentType = message.getContentType() == null ? null : message.getContentType().getBytes(UTF_8);
        final byte[] correlationData = message.getCorrelationData();
        final ImmutableIntArray subscriptionIdentifiers = message.getSubscriptionIdentifiers();
        final int subscriptionIdentifierLength = subscriptionIdentifiers == null ? 0 : subscriptionIdentifiers.length();
        final int payloadFormatIndicator = message.getPayloadFormatIndicator() != null ? message.getPayloadFormatIndicator().getCode() : -1;
        final Mqtt5UserProperties userProperties = message.getUserProperties();

        final byte[] result = new byte[
                Short.BYTES + // packet id
                        1 + // PUBLISH_BIT, dup, retain, qos
                        1 + // present flags
                        XodusUtils.shortLengthArraySize(topic) + // topic
                        Long.BYTES + // timestamp
                        Long.BYTES + // publish id
                        XodusUtils.shortLengthArraySize(hivemqId) + // hivemq id
                        Long.BYTES + // payload id
                        Long.BYTES + // message expiry
                        (responseTopic == null ? 0 : XodusUtils.shortLengthArraySize(responseTopic)) + // response topic
                        (contentType == null ? 0 : XodusUtils.shortLengthArraySize(contentType)) + // content type
                        (correlationData == null ? 0 : XodusUtils.shortLengthArraySize(correlationData)) + // correlation data
                        (subscriptionIdentifiers == null ? 0 : Integer.BYTES + subscriptionIdentifierLength * Integer.BYTES) + // subscription identifiers

                        1 + // payload format indicator
                        (userProperties.asList().size() == 0 ? 0 : PropertiesSerializationUtil.encodedSize(userProperties))
                ];

        int cursor = 0;

        cursor = XodusUtils.serializeShort(NO_PACKET_ID, result, cursor);
        byte flags = PUBLISH_BIT;
        flags |= message.getQoS().getQosNumber();
        if (message.isDuplicateDelivery()) {
            flags |= DUPLICATE_DELIVERY_BIT;
        }
        if (message.isRetain()) {
            flags |= RETAINED_BIT;
        }
        if (retained) {
            flags |= RETAINED_MESSAGE_BIT;
        }
        cursor = XodusUtils.serializeByte(flags, result, cursor);

        byte presentFlags = (byte) 0b0000_0000;

        if (responseTopic != null) {
            presentFlags |= RESPONSE_TOPIC_PRESENT_BIT;
        }
        if (contentType != null) {
            presentFlags |= CONTENT_TYPE_PRESENT_BIT;
        }
        if (correlationData != null) {
            presentFlags |= CORRELATION_DATA_PRESENT_BIT;
        }
        if (subscriptionIdentifiers != null) {
            presentFlags |= SUBSCRIPTION_IDENTIFIERS_PRESENT_BIT;
        }
        if (userProperties.asList().size() > 0) {
            presentFlags |= USER_PROPERTIES_PRESENT_BIT;
        }

        cursor = XodusUtils.serializeByte(presentFlags, result, cursor);

        cursor = XodusUtils.serializeShortLengthArray(topic, result, cursor);
        cursor = XodusUtils.serializeLong(message.getTimestamp(), result, cursor);
        cursor = XodusUtils.serializeLong(message.getPublishId(), result, cursor);
        cursor = XodusUtils.serializeShortLengthArray(hivemqId, result, cursor);
        cursor = XodusUtils.serializeLong(message.getMessageExpiryInterval(), result, cursor);
        if (responseTopic != null) {
            cursor = XodusUtils.serializeShortLengthArray(responseTopic, result, cursor);
        }
        if (contentType != null) {
            cursor = XodusUtils.serializeShortLengthArray(contentType, result, cursor);
        }
        if (correlationData != null) {
            cursor = XodusUtils.serializeShortLengthArray(correlationData, result, cursor);
        }

        if (subscriptionIdentifiers != null) {
            Bytes.copyIntToByteArray(subscriptionIdentifierLength, result, cursor);
            cursor += Integer.BYTES;
            if (subscriptionIdentifierLength > 0) {

                for (int i = 0; i < subscriptionIdentifiers.length(); i++) {
                    Bytes.copyIntToByteArray(subscriptionIdentifiers.get(i), result, cursor);
                    cursor += Integer.BYTES;
                }
            }
        }

        cursor = XodusUtils.serializeByte((byte) payloadFormatIndicator, result, cursor);
        if (userProperties.asList().size() > 0) {
            PropertiesSerializationUtil.write(userProperties, result, cursor);
        }

        return result;
    }

    @NotNull
    private PUBLISH deserializePublish(@NotNull final byte[] serialized) {
        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();

        int cursor = 0;

        builder.withPacketIdentifier(Bytes.readUnsignedShort(serialized, cursor));
        cursor += Short.BYTES;

        builder.withQoS(QoS.valueOf(serialized[cursor] & QOS_BITS));
        builder.withDuplicateDelivery((serialized[cursor] & DUPLICATE_DELIVERY_BIT) == DUPLICATE_DELIVERY_BIT);
        builder.withRetain((serialized[cursor] & RETAINED_BIT) == RETAINED_BIT);
        cursor += 1;

        final boolean responseTopicPresent = (serialized[cursor] & RESPONSE_TOPIC_PRESENT_BIT) == RESPONSE_TOPIC_PRESENT_BIT;
        final boolean contentTypePresent = (serialized[cursor] & CONTENT_TYPE_PRESENT_BIT) == CONTENT_TYPE_PRESENT_BIT;
        final boolean correlationDataPresent = (serialized[cursor] & CORRELATION_DATA_PRESENT_BIT) == CORRELATION_DATA_PRESENT_BIT;
        final boolean subscriptionIndetifiersPresent = (serialized[cursor] & SUBSCRIPTION_IDENTIFIERS_PRESENT_BIT) == SUBSCRIPTION_IDENTIFIERS_PRESENT_BIT;
        final boolean userPropertiesPresent = (serialized[cursor] & USER_PROPERTIES_PRESENT_BIT) == USER_PROPERTIES_PRESENT_BIT;
        cursor += 1;

        final int topicLength = Bytes.readUnsignedShort(serialized, cursor);
        cursor += Short.BYTES;
        builder.withTopic(new String(serialized, cursor, topicLength, UTF_8));
        cursor += topicLength;

        builder.withTimestamp(Bytes.readLong(serialized, cursor));
        cursor += Long.BYTES;

        builder.withPublishId(Bytes.readLong(serialized, cursor));
        cursor += Long.BYTES;

        final int hivemqIdLength = Bytes.readUnsignedShort(serialized, cursor);
        cursor += Short.BYTES;
        builder.withHivemqId(new String(serialized, cursor, hivemqIdLength, UTF_8));
        cursor += hivemqIdLength;

        builder.withMessageExpiryInterval(Bytes.readLong(serialized, cursor));
        cursor += Long.BYTES;

        if (responseTopicPresent) {
            final int responseTopicLength = Bytes.readUnsignedShort(serialized, cursor);
            cursor += Short.BYTES;
            if (responseTopicLength != 0) {
                builder.withResponseTopic(new String(serialized, cursor, responseTopicLength, UTF_8));
                cursor += responseTopicLength;
            }
        }

        if (contentTypePresent) {
            final int contentTypeLength = Bytes.readUnsignedShort(serialized, cursor);
            cursor += Short.BYTES;
            if (contentTypeLength != 0) {
                builder.withContentType(new String(serialized, cursor, contentTypeLength, UTF_8));
                cursor += contentTypeLength;
            }
        }

        if (correlationDataPresent) {
            final int correlationDataLength = Bytes.readUnsignedShort(serialized, cursor);
            cursor += Short.BYTES;
            if (correlationDataLength != 0) {
                final byte[] correlationData = new byte[correlationDataLength];
                System.arraycopy(serialized, cursor, correlationData, 0, correlationDataLength);
                builder.withCorrelationData(correlationData);
                cursor += correlationDataLength;
            }
        }

        if (subscriptionIndetifiersPresent) {
            final int subscriptionIdentifiersLength = Bytes.readInt(serialized, cursor);
            cursor += Integer.BYTES;

            final ImmutableIntArray.Builder subscriptionIdentifiers = ImmutableIntArray.builder();
            for (int i = 0; i < subscriptionIdentifiersLength; i++) {
                subscriptionIdentifiers.add(Bytes.readInt(serialized, cursor));
                cursor += Integer.BYTES;
            }
            builder.withSubscriptionIdentifiers(subscriptionIdentifiers.build());
        }

        builder.withPayloadFormatIndicator(Mqtt5PayloadFormatIndicator.fromCode(serialized[cursor]));
        cursor += 1;

        if (userPropertiesPresent) {
            builder.withUserProperties(PropertiesSerializationUtil.read(serialized, cursor));
        }

        return builder.withPersistence(payloadPersistence).build();
    }
}
